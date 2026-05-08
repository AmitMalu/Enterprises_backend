package com.im.admin.service.impl;

import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.NixaSoftCreateOrderRequest;
import com.im.admin.response.NixaSoftCreateOrderResponse;
import com.im.admin.service.NixaSoftPGService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
@Slf4j
@Service
@LoggableVendor
public class NixaSoftPGServiceImpl implements NixaSoftPGService {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionServiceImpl;

    @Autowired
    private RetailerRepository retailerRepository;

    @Override
    public String createOrder(NixaSoftCreateOrderRequest req, Double latitude, Double longitude) {

        log.info("Starting NixaSoft Create Order for subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getAmount());

        try {

            // ===== 1. Get Vendor Credentials =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            // Find retailer by retailerId to get email and mobile
            Retailer retailer = retailerRepository.findByRetailerId(req.getInitiatedBy())
                    .orElseThrow(() -> new RuntimeException("Retailer not found for ID: " + req.getInitiatedBy()));

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            String apiToken = credentials.getToken();

            //String cleanAmount = req.getAmount().replace(",", "");
            //BigDecimal amountInPaisa = new BigDecimal(cleanAmount).multiply(new BigDecimal("100"));
            //String paisa = amountInPaisa.toPlainString();

            // ===== 2. Prepare Request =====
            String orderId = "ORD" + commonMethods.generateBaseTransactionId();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("beneficiaryName", req.getBeneficiaryName());
            requestBody.put("mobileNumber", req.getMobileNo());
            requestBody.put("emailId", retailer.getEmailId());
            requestBody.put("amount", req.getAmount());
            requestBody.put("redirectUrl", credentials.getCallbackUrl());
            requestBody.put("requestId", orderId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", apiToken);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            log.info("Calling NixaSoft Payin API");

            ResponseEntity<NixaSoftCreateOrderResponse> response =
                    restTemplate.exchange(
                            credentials.getBaseUrlUat() +"/payin/nixa-silver-1",
                            HttpMethod.POST,
                            entity,
                            NixaSoftCreateOrderResponse.class
                    );

            NixaSoftCreateOrderResponse body = response.getBody();

            assert body != null;
            log.info("NixaSoft response received statuscode={}, order_id={}",
                    body.getStatusCode(), body.getOrderId());

            // ===== 3. Save Transaction =====

            PgTransactionMapping transaction = new PgTransactionMapping();
            transaction.setOrderId(orderId);
            transaction.setAmount(Double.valueOf(req.getAmount()));
//            transaction.setAmount(Double.valueOf(paisa));
            transaction.setInitiatedBy(req.getInitiatedBy());
            transaction.setSubProductId(req.getSubProductId());
            transaction.setStatus(body.getStatusCode());
            transaction.setVendorName("NixaSoft Payment Gateway");
            transaction.setVendorSessionId(body.getOrderId());
            transaction.setCardBinNo(req.getCardBinNo());
            transaction.setLatitude(latitude);
            transaction.setLongitude(longitude);

            pgTransactionMappingRepository.save(transaction);

            // ===== 4. Return Payment URL =====

            if ("TXN".equals(body.getStatusCode())) {

                log.info("Payment URL generated successfully orderId={}", orderId);
                return body.getPaymentUrl();

            } else {

                log.error("Transaction failed for orderId={}", orderId);
                throw new RuntimeException("Transaction Failed");

            }

        } catch (HttpStatusCodeException ex) {

            String errorBody = ex.getResponseBodyAsString();

            log.error("Vendor Error Response: {}", errorBody);

            return errorBody;

        } catch (Exception e) {

            Throwable root = e;

            while (root.getCause() != null) {
                root = root.getCause();
            }

            log.error("ROOT CAUSE => ", root, e);

            return "Error: " + root.getMessage();
        }
    }

    @Override
    @Transactional
    public void processCallback(NixaSoftCallbackRequest request) {

        log.info("NixaSoft Callback Started {}", request.toString());
        log.info("Processing NixaSoft callback for orderId={}", request.getOrderId());

        PgTransactionMapping pgTransactionMapping =
                pgTransactionMappingRepository
                        .findByOrderId(request.getOrderId())
                        .orElseThrow(() ->
                                new RuntimeException("Transaction not found"));

        // Update status
        pgTransactionMapping.setStatus(request.getStatus().toUpperCase());
        pgTransactionMappingRepository.save(pgTransactionMapping);

        String subProductId = pgTransactionMapping.getSubProductId();

        SubProducts subProducts =
                subProductsRepository.findById(subProductId)
                        .orElseThrow(() -> {
                            log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
                            return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
                        });

        log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
                subProducts.getId(),
                subProducts.getSubProductCode());

        SchemeAllocation schemeAllocation =
                schemeAllocationRepository
                        .findByHierarchyIdAndSubProductCode(
                                pgTransactionMapping.getInitiatedBy(),
                                subProducts.getSubProductCode()
                        )
                        .orElseThrow(() -> new RuntimeException(
                                "SchemeAllocation not found | hierarchyId="
                                        + pgTransactionMapping.getInitiatedBy()
                                        + ", subProductCode="
                                        + subProducts.getSubProductCode()
                        ));

        String schemeCode = schemeAllocation.getSchemeCode();

        log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);

        // Prepare base data
        Double txnAmt = Double.valueOf(request.getAmount());
        log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);

        log.info("---- [Callback] Using operatorName for DB lookup: {}", request.getPayment_network());

        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        request.getPayment_network()
                );
        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
            log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                    schemeCode, request.getPayment_network());
            return;
        }

        log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());

        // Pick correct scheme by amount range or fixed value
        SchemeRate selectedScheme = matchingSchemes.stream()
                .filter(scheme ->
                        scheme.getSlabType() == SlabType.VARIABLE &&
                                txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()
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

        String orderId = pgTransactionMapping.getOrderId();

        String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

        // Build DTO (common for all statuses)
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

        dto.setBaseTranxId(baseTranxId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
        dto.setOperatorName(request.getPayment_network());
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
        dto.setVendorTxnId(request.getRequestId());
        dto.setVendorName("NixaSoft PG");

        log.debug("---- [Callback] DTO prepared: {}", dto);
        log.info("---- [Callback] DTO prepared: {}", dto);

        String vendorStatus = request.getStatus();

        // Forward to central transaction engine
        log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", vendorStatus);
        transactionServiceImpl.processTransaction(dto, vendorStatus);

        log.info("Transaction updated successfully orderId={}, status={}",
                request.getOrderId(),
                request.getStatus());
    }

    @Override
    public void checkStatus(String orderId) {

    }
}
