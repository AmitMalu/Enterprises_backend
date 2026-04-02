package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.CashfreeCreateOrderRequest;
import com.im.admin.request.CashfreeOrderPayRequest;
import com.im.admin.service.CashfreePGService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
@LoggableVendor
public class CashfreePGServiceImpl implements CashfreePGService {

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionServiceImpl;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;


    @Override
    public String createOrder(CashfreeCreateOrderRequest req, Double latitude, Double longitude) {

        log.info("Starting Cashfree Payment Link | subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getOrder_amount());

        try {

            // ===== 1. Get Vendor Credentials =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("No credentials found"));

            // ===== 2. Generate Link ID =====
            String linkId = "ORD" + commonMethods.generateBaseTransactionId();

            // ===== 3. Prepare Request =====
            Map<String, Object> body = new HashMap<>();

            body.put("link_id", linkId);
            body.put("link_amount", req.getOrder_amount());
            body.put("link_currency", "INR");
            body.put("link_purpose", "Payment for order" + linkId);

            Map<String, Object> customer = new HashMap<>();
            customer.put("customer_name", req.getCustomer_details().getCustomer_name());
            customer.put("customer_email", req.getCustomer_details().getCustomer_email());
            customer.put("customer_phone", req.getCustomer_details().getCustomer_phone());

            body.put("customer_details", customer);

            body.put("link_notify", Map.of(
                    "send_sms", true,
                    "send_email", true
            ));

            // ===== META (IMPORTANT) =====
            body.put("link_meta", Map.of(
                    "return_url", "https://uat.decipay.in/enterprises/success",
                    "notify_url", credentials.getCallbackUrl()
            ));

            // ===== EXPIRY (VERY IMPORTANT) =====
            body.put("link_expiry_time",
                    ZonedDateTime.now().plusMinutes(15).toString());

            // ===== 4. Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.set("x-client-id", credentials.getClientId());
            headers.set("x-client-secret", credentials.getClientSecret());
            headers.set("x-api-version", credentials.getSaltKey());
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            // ===== 5. Call API =====
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/links",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();

            if (response.getStatusCode().is2xxSuccessful() && responseBody != null) {

                String linkUrl = (String) responseBody.get("link_url");

                log.info("Payment Link created successfully | linkId={}, url={}",
                        linkId, linkUrl);

                // ===== 6. Save DB =====
                PgTransactionMapping mapping = new PgTransactionMapping();
                mapping.setOrderId(linkId);
                mapping.setAmount(req.getOrder_amount());
                mapping.setStatus("INITIATED");
                mapping.setInitiatedBy(req.getInitiatedBy());
                mapping.setSubProductId(req.getSubProductId());
                mapping.setVendorName("Cashfree PG");
                mapping.setLatitude(latitude);
                mapping.setLongitude(longitude);

                pgTransactionMappingRepository.save(mapping);

                return linkUrl;

            } else {
                log.error("Payment Link creation failed | linkId={}", linkId);
                throw new RuntimeException("Payment link creation failed");
            }

        } catch (Exception e) {
            log.error("Cashfree Payment Link API error", e);
            throw new RuntimeException("Cashfree API Failed");
        }
    }


//    @Override
//    public void processWebhook(String payload,
//                               String signature,
//                               String timestamp,
//                               String idempotencyKey) {
//
//        log.info("Processing Cashfree webhook");
//
//        try {
//
//            // ===== 1. VERIFY SIGNATURE =====
//            boolean isValid = verifySignature(payload, signature, timestamp);
//
//            if (!isValid) {
//                log.error("Invalid webhook signature");
//                throw new RuntimeException("Invalid signature");
//            }
//
//            log.info("Webhook signature verified successfully");
//
//            // ===== 2. PARSE PAYLOAD =====
//            ObjectMapper mapper = new ObjectMapper();
//            JsonNode jsonNode = mapper.readTree(payload);
//
//            String orderId = jsonNode.path("data").path("order").path("order_id").asText();
//            String paymentStatus = jsonNode.path("data").path("payment").path("payment_status").asText();
//            String paymentMethod = jsonNode.path("data").path("payment").path("payment_method").asText();
//            String cardType = jsonNode.path("data").path("payment").path("payment_method").path("card_type").asText();
//            String cardNetwork = jsonNode.path("data").path("payment").path("payment_method").path("card_network").asText();
//            String cfPaymentId = jsonNode.path("data").path("payment").path("cf_payment_id").asText();
//            String amount = jsonNode.path("data").path("order").path("order_amount").asText();
//
//            log.info("Webhook data | orderId={}, status={}, method={}, cfPaymentId={}",
//                    orderId, paymentStatus, paymentMethod, cfPaymentId);
//
//            PgTransactionMapping pgTransactionMapping =
//                    pgTransactionMappingRepository
//                            .findByOrderId(orderId)
//                            .orElseThrow(() ->
//                                    new RuntimeException("Transaction not found"));
//
//            // Update status
//            pgTransactionMapping.setStatus(paymentStatus.toUpperCase());
//            pgTransactionMappingRepository.save(pgTransactionMapping);
//
//            String subProductId = pgTransactionMapping.getSubProductId();
//
//            SubProducts subProducts =
//                    subProductsRepository.findById(subProductId)
//                            .orElseThrow(() -> {
//                                log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
//                                return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
//                            });
//
//            log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
//                    subProducts.getId(),
//                    subProducts.getSubProductCode());
//
//            SchemeAllocation schemeAllocation =
//                    schemeAllocationRepository
//                            .findByHierarchyIdAndSubProductCode(
//                                    pgTransactionMapping.getInitiatedBy(),
//                                    subProducts.getSubProductCode()
//                            )
//                            .orElseThrow(() -> new RuntimeException(
//                                    "SchemeAllocation not found | hierarchyId="
//                                            + pgTransactionMapping.getInitiatedBy()
//                                            + ", subProductCode="
//                                            + subProducts.getSubProductCode()
//                            ));
//
//            String schemeCode = schemeAllocation.getSchemeCode();
//
//            log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);
//
//            // Prepare base data
//            Double txnAmt = Double.valueOf(amount);
//            log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);
//
//            log.info("---- [Callback] Using operatorName for DB lookup: {} {}", cardNetwork, cardType);
//
//            String callbackOperatorName = toDbOperatorName(
//                    cardNetwork,
//                    cardType
//            );
//
//            List<SchemeRate> matchingSchemes =
//                    schemeRateRepository.findBySchemeCodeAndOperatorName(
//                            schemeCode,
//                            callbackOperatorName
//                    );
//            if (matchingSchemes == null || matchingSchemes.isEmpty()) {
//                log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
//                        schemeCode, callbackOperatorName);
//                return;
//            }
//
//            log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());
//
//            // Pick correct scheme by amount range or fixed value
//            SchemeRate selectedScheme = matchingSchemes.stream()
//                    .filter(scheme ->
//                            scheme.getSlabType() == SlabType.VARIABLE &&
//                                    txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()
//                    )
//                    .findFirst()
//                    .orElse(matchingSchemes.get(0));
//            log.info("---- [Callback] Selected scheme: {}", selectedScheme);
//
//            // Fetch scheme, product & subproduct details
//            log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
//            log.info("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
//            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
//                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));
//
//            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
//            short productId = Short.parseShort(hashParts[0]);
//            String subProductIdStr = hashParts[1];
//            log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
//            log.info("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
//
//            Products product = productsRepository.findById(productId)
//                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
//            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
//                    .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));
//
//            String orderId1 = pgTransactionMapping.getOrderId();
//
//            String baseTranxId = orderId1.startsWith("ORD") ? orderId1.substring(3) : orderId1;
//
//            // Build DTO (common for all statuses)
//            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
//
//            dto.setBaseTranxId(baseTranxId);
//            dto.setAmount(txnAmt);
//            dto.setConfirmAmount(txnAmt);
//            dto.setOperatorName(callbackOperatorName);
//            dto.setOperatorCode(selectedScheme.getOperatorCode());
//            dto.setSchemeRate(selectedScheme);
//            dto.setTransactionType(product.getTransactionType());
//            dto.setWalletType(subProduct.getWalletType());
//            dto.setGstValue(String.valueOf(product.getSgst()));
//            dto.setTdsRate(String.valueOf(product.getTds()));
//            dto.setHierarchyId(pgTransactionMapping.getInitiatedBy());
//            dto.setProductCode(product.getProductCode());
//            dto.setSubProductCode(subProduct.getSubProductCode());
//            dto.setIsCommission(product.getIsCommissions());
//            dto.setHasCharge(product.getHasCharges());
//            dto.setVendorTxnId(jsonNode.path("data").path("payment").path("cf_payment_id").asText());
//            dto.setVendorName("Cashfree PG");
//
//            log.debug("---- [Callback] DTO prepared: {}", dto);
//            log.info("---- [Callback] DTO prepared: {}", dto);
//
//            // Forward to central transaction engine
//            log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", paymentStatus);
//            transactionServiceImpl.processTransaction(dto, paymentStatus);
//
//            log.info("Transaction updated successfully orderId={}, status={}",
//                    orderId,
//                    paymentStatus);
//
//        } catch (Exception e) {
//            log.error("Error processing webhook", e);
//            throw new RuntimeException(e);
//        }
//    }
//
//    private boolean verifySignature(String payload,
//                                    String signature,
//                                    String timestamp) {
//
//        try {
//
//            String signedPayload = timestamp + payload;
//
//            String clientSecret = "YOUR_CLIENT_SECRET";
//
//            Mac mac = Mac.getInstance("HmacSHA256");
//            SecretKeySpec secretKey =
//                    new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");
//
//            mac.init(secretKey);
//
//            byte[] hash = mac.doFinal(signedPayload.getBytes());
//            String generatedSignature = Base64.getEncoder().encodeToString(hash);
//
//            return generatedSignature.equals(signature);
//
//        } catch (Exception e) {
//            log.error("Signature verification failed", e);
//            return false;
//        }
//    }
//

    private String toDbOperatorName(String cardCategory, String pgPartner) {
        log.info("<----------------------- Card Category: {}, PG Partner: {}", cardCategory, pgPartner);

        return Stream.of(cardCategory, pgPartner)
                .filter(s -> s != null && !s.isBlank() && !s.equalsIgnoreCase("NA") && !s.equalsIgnoreCase("N/A"))
                .map(s -> Arrays.stream(s.split("_"))
                        .filter(part -> !part.isBlank())
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(Collectors.joining("_")))
                .collect(Collectors.joining("_"));
    }

    @Override
    public void processWebhook(String payload,
                               String signature,
                               String timestamp,
                               String idempotencyKey) {

        log.info("Processing Cashfree Payment Link webhook :{}", payload);

        try {

            // ===== 1. VERIFY SIGNATURE =====
            if (!verifySignature(payload, signature, timestamp)) {
                log.error("Invalid webhook signature");
                throw new RuntimeException("Invalid signature");
            }

            log.info("Webhook signature verified successfully");

            // ===== 2. PARSE PAYLOAD =====
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            String type = root.path("type").asText();

            if (!"PAYMENT_LINK_EVENT".equals(type)) {
                log.warn("Ignoring event type={}", type);
                return;
            }

            JsonNode data = root.path("data");

            String linkId = data.path("link_id").asText();
            String linkStatus = data.path("link_status").asText();
            String amountPaid = data.path("link_amount_paid").asText();

            log.info("Webhook data | linkId={}, status={}, amountPaid={}",
                    linkId, linkStatus, amountPaid);

            // ===== 3. FETCH TRANSACTION =====
            PgTransactionMapping txn =
                    pgTransactionMappingRepository
                            .findByOrderId(linkId)
                            .orElseThrow(() -> new RuntimeException("Transaction not found"));

            // ===== 4. UPDATE STATUS =====
            switch (linkStatus) {

                case "PAID":
                    txn.setStatus("SUCCESS");
                    break;

                case "PARTIALLY_PAID":
                    txn.setStatus("PARTIAL");
                    break;

                case "EXPIRED":
                    txn.setStatus("EXPIRED");
                    break;

                case "CANCELLED":
                    txn.setStatus("CANCELLED");
                    break;

                default:
                    log.warn("Unhandled status={}", linkStatus);
            }

            pgTransactionMappingRepository.save(txn);

//            String subProductId = txn.getSubProductId();
//
//            SubProducts subProducts =
//                    subProductsRepository.findById(subProductId)
//                            .orElseThrow(() -> {
//                                log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", subProductId);
//                                return new RuntimeException("SubProduct not found for subProductId: " + subProductId);
//                            });
//
//            log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductCode={}",
//                    subProducts.getId(),
//                    subProducts.getSubProductCode());
//
//            SchemeAllocation schemeAllocation =
//                    schemeAllocationRepository
//                            .findByHierarchyIdAndSubProductCode(
//                                    txn.getInitiatedBy(),
//                                    subProducts.getSubProductCode()
//                            )
//                            .orElseThrow(() -> new RuntimeException(
//                                    "SchemeAllocation not found | hierarchyId="
//                                            + txn.getInitiatedBy()
//                                            + ", subProductCode="
//                                            + subProducts.getSubProductCode()
//                            ));
//
//            String schemeCode = schemeAllocation.getSchemeCode();
//
//            log.info("---- [Callback] SchemeAllocation fetched successfully | schemeCode={}", schemeCode);
//
//            // Prepare base data
//            Double txnAmt = Double.valueOf(amountPaid);
//            log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);
//
//            log.info("---- [Callback] Using operatorName for DB lookup: {} {}", cardNetwork, cardType);
//
//            String callbackOperatorName = toDbOperatorName(
//                    cardNetwork,
//                    cardType
//            );
//
//            List<SchemeRate> matchingSchemes =
//                    schemeRateRepository.findBySchemeCodeAndOperatorName(
//                            schemeCode,
//                            callbackOperatorName
//                    );
//            if (matchingSchemes == null || matchingSchemes.isEmpty()) {
//                log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
//                        schemeCode, callbackOperatorName);
//                return;
//            }
//
//            log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());
//
//            // Pick correct scheme by amount range or fixed value
//            SchemeRate selectedScheme = matchingSchemes.stream()
//                    .filter(scheme ->
//                            scheme.getSlabType() == SlabType.VARIABLE &&
//                                    txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd()
//                    )
//                    .findFirst()
//                    .orElse(matchingSchemes.get(0));
//            log.info("---- [Callback] Selected scheme: {}", selectedScheme);
//
//            // Fetch scheme, product & subproduct details
//            log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
//            log.info("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
//            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
//                    .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));
//
//            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
//            short productId = Short.parseShort(hashParts[0]);
//            String subProductIdStr = hashParts[1];
//            log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
//            log.info("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);
//
//            Products product = productsRepository.findById(productId)
//                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
//            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
//                    .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));
//
//            String orderId1 = txn.getOrderId();
//
//            String baseTranxId = orderId1.startsWith("ORD") ? orderId1.substring(3) : orderId1;
//
//            // Build DTO (common for all statuses)
//            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
//
//            dto.setBaseTranxId(baseTranxId);
//            dto.setAmount(txnAmt);
//            dto.setConfirmAmount(txnAmt);
//            dto.setOperatorName(callbackOperatorName);
//            dto.setOperatorCode(selectedScheme.getOperatorCode());
//            dto.setSchemeRate(selectedScheme);
//            dto.setTransactionType(product.getTransactionType());
//            dto.setWalletType(subProduct.getWalletType());
//            dto.setGstValue(String.valueOf(product.getSgst()));
//            dto.setTdsRate(String.valueOf(product.getTds()));
//            dto.setHierarchyId(txn.getInitiatedBy());
//            dto.setProductCode(product.getProductCode());
//            dto.setSubProductCode(subProduct.getSubProductCode());
//            dto.setIsCommission(product.getIsCommissions());
//            dto.setHasCharge(product.getHasCharges());
//            //dto.setVendorTxnId(jsonNode.path("data").path("payment").path("cf_payment_id").asText());
//            dto.setVendorName("Cashfree PG");
//
//            log.debug("---- [Callback] DTO prepared: {}", dto);
//            log.info("---- [Callback] DTO prepared: {}", dto);
//
//            // Forward to central transaction engine
//            log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", linkStatus);
//            transactionServiceImpl.processTransaction(dto, linkStatus);
//
//            log.info("Transaction updated successfully orderId={}, status={}",
//                    linkId,
//                    linkStatus);
//
//            log.info("Transaction updated successfully | linkId={}, status={}",
//                    linkId, txn.getStatus());

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException(e);
        }
    }

    private boolean verifySignature(String payload,
                                    String signature,
                                    String timestamp) {

        try {

            String signedPayload = timestamp + payload;

            String clientSecret = "cfsk_ma_prod_30241315015c8923ec7be845875d4a0b_dc828db4";  //this is uat client secret later put prod secret

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey =
                    new SecretKeySpec(clientSecret.getBytes(), "HmacSHA256");

            mac.init(secretKey);

            byte[] hash = mac.doFinal(signedPayload.getBytes());
            String generatedSignature = Base64.getEncoder().encodeToString(hash);

            return generatedSignature.equals(signature);

        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }
}
