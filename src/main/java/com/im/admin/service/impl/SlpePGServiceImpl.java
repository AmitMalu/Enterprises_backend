package com.im.admin.service.impl;

import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.SlpePgCreateOrderRequest;
import com.im.admin.request.UnisusAuthRequest;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.service.SlpePGService;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
@RequiredArgsConstructor
@LoggableVendor
public class SlpePGServiceImpl implements SlpePGService {

    private final VendorRoutingRepository vendorRoutingRepository;
    private final VendorCredentialsRepository vendorCredentialsRepository;
    private final CommonMethods commonMethods;
    private final PgTransactionMappingRepository pgTransactionMappingRepository;
    private final TransactionEngineServiceImpl transactionEngineServiceImpl;
    private final SubProductsRepository subProductsRepository;
    private final SchemeAllocationRepository schemeAllocationRepository;
    private final SchemeRateRepository schemeRateRepository;
    private final ProductsRepository productsRepository;
    private final SchemeRepository schemeRepository;
    private final RetailerRepository retailerRepository;

    @Override
    public String createOrder(SlpePgCreateOrderRequest req, RequestHeaderMetaDto requestHeaderMetaDto) {

        log.info("Starting SLPE create order for subProductId={}, initiatedBy={}, amount={}", req.getSubProductId(), req.getInitiatedBy(), req.getAmount());

        try {

            // ====== 1. Get Vendor & Credentials ======

            Optional<Retailer> retailer = retailerRepository.findByRetailerId(req.getInitiatedBy());

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository.findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId()).orElseThrow(() -> new RuntimeException("No credentials found"));

            log.info("Vendor credentials fetched successfully for vendorId={}", vendor.getId());

            // ====== 2. Prepare Headers ======
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("access-token", credentials.getToken());
            headers.set("api-key", credentials.getSaltKey());
            headers.set("api-secret", credentials.getClientSecret());
            headers.set("api-mode", "test");

            // ====== 3. Prepare Request Body ======
            Map<String, Object> body = new HashMap<>();

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // current time + 25 minutes
            String expiry = LocalDateTime.now().plusMinutes(25).format(formatter);

            body.put("amount", String.valueOf(req.getAmount()));
            body.put("call_back_url", credentials.getCallbackUrl());
            body.put("gateway_id", "1");
            body.put("payment_link_expiry", expiry); // format: yyyy-MM-dd HH:mm:ss
            //body.put("payment_for", req.getPaymentFor());
            body.put("payment_for", "Utility Payment");


            // customer
            Map<String, Object> customer = new HashMap<>();
            customer.put("name", req.getCustomerName());
            customer.put("email", retailer.get().getEmailId());
            customer.put("phone", req.getCustomerMobile());

            body.put("customer", customer);

            // mode
            Map<String, Object> mode = new HashMap<>();
            mode.put("netbanking", false);
            mode.put("card", true);
            mode.put("upi", false);
            mode.put("wallet", false);

            body.put("mode", mode);

            // ====== 4. Call API ======
            RestTemplate restTemplate = new RestTemplate();

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            log.info("Calling SLPE create order API...");

            ResponseEntity<Map> response = restTemplate.exchange(credentials.getBaseUrlUat() + "/create-order", HttpMethod.POST, entity, Map.class);

            // ====== 5. Handle Response ======
            Map responseBody = response.getBody();

            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("result"))) {

                String paymentUrl = (String) responseBody.get("payment_url");
                String txnId = (String) responseBody.get("order_id");

                log.info("SLPE Order created successfully. orderId={}, paymentUrl={}", txnId, paymentUrl);

                // STEP 1 – Create Internal Transaction FIRST
                String orderId = "ORD" + commonMethods.generateBaseTransactionId();

                PgTransactionMapping pgTransactionMapping = new PgTransactionMapping();
                pgTransactionMapping.setTxnId(txnId);
                pgTransactionMapping.setOrderId(orderId);
                pgTransactionMapping.setAmount(req.getAmount());
                pgTransactionMapping.setInitiatedBy(req.getInitiatedBy());
                pgTransactionMapping.setSubProductId(req.getSubProductId());
                pgTransactionMapping.setStatus("PENDING");
                pgTransactionMapping.setVendorName("SLPE Payment Gateway");
                pgTransactionMapping.setLatitude(requestHeaderMetaDto.getLatitude());
                pgTransactionMapping.setLongitude(requestHeaderMetaDto.getLongitude());

                pgTransactionMappingRepository.save(pgTransactionMapping);

                log.info("Transaction saved successfully. orderId={}, status=INITIATED", orderId);

                return paymentUrl;

            } else {
                log.error("SLPE create order failed: {}", responseBody);
                throw new RuntimeException("SLPE order creation failed");
            }

        } catch (Exception e) {
            log.error("Error while creating SLPE order", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    @Transactional
    public void processCallback(Map<String, Object> payload) {

        try {

            // ===== Extract fields (adjust based on actual SLPE response) =====
            String orderId = (String) payload.get("order_id");
            String status = (String) payload.get("status"); // SUCCESS / FAILED / PENDING
            String txnId = (String) payload.get("txn_id");
            Double amount = (Double) payload.get("amount");
            String txnMode = (String) payload.get("mode");        //card
            String category = (String) payload.get("network");    //VISA
            String pgPartner = (String) payload.get("pg_partner");   //Razorpay

            log.info("Processing webhook for orderId={}, status={}", orderId, status);

            PgTransactionMapping pgTransactionMapping = pgTransactionMappingRepository.findByOrderId(orderId).orElseThrow(() -> new RuntimeException("Transaction not found"));

            if ("SUCCESS".equalsIgnoreCase(pgTransactionMapping.getStatus())) {
                log.warn("Transaction already processed for orderId={}", orderId);
                return;
            }

            String subProductId = pgTransactionMapping.getSubProductId();
            log.info("---- [SLPE Callback] Fetching SubProduct using subProductId: {}", subProductId);

            SubProducts subProducts = subProductsRepository.findById(subProductId).orElseThrow(() -> {
                log.error("---- [SLPE Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
                return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
            });

            log.info("---- [SLPE Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}", subProducts.getId(), subProducts.getSubProductCode());

            SchemeAllocation schemeAllocation = schemeAllocationRepository.findByHierarchyIdAndSubProductCode(pgTransactionMapping.getInitiatedBy(), subProducts.getSubProductCode()).orElseThrow(() -> new RuntimeException("SchemeAllocation not found | hierarchyId=" + pgTransactionMapping.getInitiatedBy() + ", subProductCode=" + subProducts.getSubProductCode()));

            String schemeCode = schemeAllocation.getSchemeCode();

            log.info("---- [SLPE Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);

            // Prepare base data
            log.info("---- [SLPE Callback] Parsed transaction amount: {}", amount);

            String callbackOperatorName = toDbOperatorName(txnMode, category, pgPartner);
            log.info("---- [SLPE Callback] Using operatorName for DB lookup: {}", callbackOperatorName);

            List<SchemeRate> matchingSchemes = schemeRateRepository.findBySchemeCodeAndOperatorName(schemeCode, callbackOperatorName);
            if (matchingSchemes == null || matchingSchemes.isEmpty()) {
                log.error("---- [SLPE Callback] No matching scheme found for schemeCode={} and operatorName={}", schemeCode, callbackOperatorName);
                return;
            }

            log.info("---- [SLPE Callback] Scheme(s) found: {}", matchingSchemes.size());

            // Pick correct scheme by amount range or fixed value
            SchemeRate selectedScheme = matchingSchemes.stream().filter(scheme -> scheme.getSlabType() == SlabType.VARIABLE && amount >= scheme.getSlabStart() && amount <= scheme.getSlabEnd()).findFirst().orElse(matchingSchemes.get(0));
            log.info("---- [SLPE Callback] Selected scheme: {}", selectedScheme);

            // Fetch scheme, product & subProduct details
            log.debug("---- [SLPE Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
            log.info("---- [SLPE Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode()).orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));

            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];
            log.debug("---- [SLPE Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
            log.info("---- [SLPE Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);

            Products product = productsRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            SubProducts subProduct = subProductsRepository.findById(subProductIdStr).orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));


            String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

            // Build DTO (common for all statuses)
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

            dto.setBaseTranxId(baseTranxId);
            dto.setAmount(amount);
            dto.setConfirmAmount(amount);
            dto.setOperatorName(callbackOperatorName);
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setHierarchyId(pgTransactionMapping.getInitiatedBy());
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setLatitude(pgTransactionMapping.getLatitude());
            dto.setLongitude(pgTransactionMapping.getLongitude());
            dto.setVendorName("SLPE PG");
            dto.setVendorTxnId(txnId);
            dto.setMedium(pgTransactionMapping.getMedium());
            dto.setClientIp(pgTransactionMapping.getClientIp());
            dto.setRequestDomainName(pgTransactionMapping.getRequestDomainName());

            log.debug("---- [Callback] DTO prepared: {}", dto);
            log.info("---- [Callback] DTO prepared: {}", dto);

            // ===== Update based on status =====
            if ("SUCCESS".equalsIgnoreCase(status)) {

                pgTransactionMapping.setStatus("SUCCESS");
                pgTransactionMapping.setTxnId(txnId);

                // Forward to central transaction engine
                log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", status);
                transactionEngineServiceImpl.processTransaction(dto, status);

                log.info("---- [Callback] Processing complete for Order ID: {} | Status: {}", orderId, status);

                // CREDIT WALLET / BUSINESS LOGIC HERE
                log.info("Payment SUCCESS for orderId={}", orderId);

            } else if ("FAILED".equalsIgnoreCase(status)) {

                pgTransactionMapping.setStatus("FAILED");

                // Forward to central transaction engine
                log.info("---- Callback Forwarding to Transaction Engine with Status: {}", status);
                transactionEngineServiceImpl.processTransaction(dto, status);

                log.info("Payment FAILED for orderId={}", orderId);

            } else {

                pgTransactionMapping.setStatus("PENDING");

                // Forward to central transaction engine
                log.info("---- [Callback Forwarding to Transaction Engine with Status: {}", status);
                transactionEngineServiceImpl.processTransaction(dto, status);
            }

            pgTransactionMappingRepository.save(pgTransactionMapping);

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException(e);
        }
    }

    private String toDbOperatorName(String txnMode, String cardCategory, String pgPartner) {
        log.info("<-----------------------Txn Mode: {}, Card Category: {}, PG Partner: {}", txnMode, cardCategory, pgPartner);

        return Stream.of(txnMode, cardCategory, pgPartner).filter(s -> s != null && !s.isBlank() && !s.equalsIgnoreCase("NA") && !s.equalsIgnoreCase("N/A")).map(s -> Arrays.stream(s.split("_")).filter(part -> !part.isBlank()).map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase()).collect(Collectors.joining("_"))).collect(Collectors.joining("_"));
    }

    @Override
    public PgStatusResponse checkStatus(String orderId) {

        try {
            log.info("Checking SLPE status for orderId={}", orderId);

            // ===== 1. Fetch Transaction =====
            PgTransactionMapping pgTxn = pgTransactionMappingRepository
                    .findByOrderId(orderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // ===== 2. Fetch Vendor Credentials =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(pgTxn.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            // ===== 3. Call SLPE API =====
            HttpHeaders headers = new HttpHeaders();
            headers.set("access-token", credentials.getToken());
            headers.set("api-key", credentials.getSaltKey());
            headers.set("api-secret", credentials.getClientSecret());
            headers.set("api-mode", "test");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = credentials.getBaseUrlProd() + "/order-status/" + orderId;

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map body = response.getBody();

            log.info("SLPE response: {}", body);

            // ===== 4. Parse Response =====
            Map data = (Map) body.get("data");
            Map orderData = (Map) data.get("order_data");

            String slpeStatus = (String) orderData.get("status");   // paid / pending / failed
            String mode = (String) orderData.get("mode");           // card / upi etc
            String amountStr = (String) orderData.get("amount");

            Double amount = Double.valueOf(amountStr);

            String pgPartner = (String) data.get("pg_partner");

            // ===== 5. Map Status =====
            String status;
            if ("paid".equalsIgnoreCase(slpeStatus)) {
                status = "SUCCESS";
            } else if ("failed".equalsIgnoreCase(slpeStatus)) {
                status = "FAILED";
            } else {
                status = "PENDING";
            }

            log.info("Mapped status: {}", status);

            // ===== 6. Build Operator Name =====
            String operatorName = toDbOperatorName(mode, null, pgPartner);

            // ===== 7. CALL COMMON PROCESS METHOD =====
            processTransactionFromStatus(pgTxn, amount, operatorName, status);

            // ===== RETURN COMMON DTO =====
            return PgStatusResponse.builder()
                    .orderId(orderId)
                    .status(status)
                    .amount(amount)
                    .vendorTxnId(null) // extract if available
                    .message("SLPE status fetched successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error in checkStatus", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private void processTransactionFromStatus(
            PgTransactionMapping pgTxn,
            Double amount,
            String operatorName,
            String status) {


        String subProductId = pgTxn.getSubProductId();
        log.info("---- [Callback] Fetching SubProduct using subProductId: {}", subProductId);

        SubProducts subProducts =
                subProductsRepository.findById(subProductId)
                        .orElseThrow(() -> {
                            log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
                            return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
                        });

        log.info("---- [Callback] SubProduct fetched successfully | subProductId={}", subProducts.getId());

        log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
                subProducts.getId(),
                subProducts.getSubProductCode());

        SchemeAllocation schemeAllocation =
                schemeAllocationRepository
                        .findByHierarchyIdAndSubProductCode(
                                pgTxn.getInitiatedBy(),
                                subProducts.getSubProductCode()
                        )
                        .orElseThrow(() -> new RuntimeException(
                                "SchemeAllocation not found | hierarchyId="
                                        + pgTxn.getInitiatedBy()
                                        + ", subProductCode="
                                        + subProducts.getSubProductCode()
                        ));

        String schemeCode = schemeAllocation.getSchemeCode();

        log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);

        // Prepare base data
        log.info("---- [Callback] Parsed transaction amount: {}", amount);

        log.info("---- [Callback] Using operatorName for DB lookup: {}", operatorName);

        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        operatorName
                );
        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
            log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                    schemeCode, operatorName);
            return;
        }

        log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());

        // Pick correct scheme by amount range or fixed value
        SchemeRate selectedScheme = matchingSchemes.stream()
                .filter(scheme ->
                        scheme.getSlabType() == SlabType.VARIABLE &&
                                amount >= scheme.getSlabStart() && amount <= scheme.getSlabEnd()
                )
                .findFirst()
                .orElse(matchingSchemes.get(0));
        log.info("---- [Callback] Selected scheme: {}", selectedScheme);

        // Fetch scheme, product & subproduct details
        log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
        log.info("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
        short productId = Short.parseShort(hashParts[0]);
        String subProductIdStr = hashParts[1];
        log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
        log.info("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

        String baseTranxId = pgTxn.getOrderId().startsWith("ORD")
                ? pgTxn.getOrderId().substring(3)
                : pgTxn.getOrderId();

        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTranxId);
        dto.setAmount(amount);
        dto.setConfirmAmount(amount);
        dto.setOperatorName(operatorName);
        dto.setOperatorCode(selectedScheme.getOperatorCode());
        dto.setSchemeRate(selectedScheme);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setHierarchyId(pgTxn.getInitiatedBy());
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setLatitude(pgTxn.getLatitude());
        dto.setLongitude(pgTxn.getLongitude());

        // ===== Call Engine =====
        transactionEngineServiceImpl.processTransaction(dto, status);

        // ===== Update PG Table =====
        pgTxn.setStatus(status);
        pgTransactionMappingRepository.save(pgTxn);
    }

}
