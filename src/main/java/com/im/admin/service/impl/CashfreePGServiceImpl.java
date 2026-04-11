package com.im.admin.service.impl;

import com.cashfree.pg.Cashfree;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.ContentCachingRequestWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private WebhookIdempotencyRepository webhookIdempotencyRepository;

    @Override
    public String createOrder(CashfreeCreateOrderRequest req, Double latitude, Double longitude) {

        log.info("Starting Cashfree Payment Link | subProductId={}, initiatedBy={}, amount={}",
                req.getSubProductId(), req.getInitiatedBy(), req.getOrder_amount());

        try {

            Optional<Retailer> retailer = retailerRepository.findByRetailerId(req.getInitiatedBy());

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
            customer.put("customer_email", retailer.get().getEmailId());
            customer.put("customer_phone", req.getCustomer_details().getCustomer_phone());

            body.put("customer_details", customer);

            body.put("link_notify", Map.of(
                    "send_sms", true,
                    "send_email", true
            ));

            // ===== META (IMPORTANT) =====
            body.put("link_meta", Map.of(
                    "return_url", "https://uat.decipay.in/enterprise-portal/transaction-completed",
                    "notify_url", credentials.getCallbackUrl()
            ));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            String expiryTime = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                    .plusMinutes(30)
                    .format(formatter);

            body.put("link_expiry_time", expiryTime);

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

    @Override
    @Transactional
    public void processWebhook(String payload, String idempotencyKey) {

        log.info("========== CASHFREE WEBHOOK PROCESS START ==========");
        log.info("Payload >>> {}", payload);
        log.info("IdempotencyKey >>> {}", idempotencyKey);

        try {

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(payload);

            String eventType = root.path("type").asText();
            log.info("Event Type >>> {}", eventType);

            JsonNode dataNode = root.path("data");
            JsonNode payment = dataNode.path("payment");

            String dbOperatorName = "";
            String linkId = null;
            String status = null;
            double txnAmt;

            if ("PAYMENT_LINK_EVENT".equals(eventType)) {

                log.info("Handling PAYMENT_LINK_EVENT");

                linkId = dataNode.path("link_id").asText();
                status = dataNode.path("link_status").asText();

                String amountPaidStr = dataNode.path("link_amount_paid").asText();
                txnAmt = (amountPaidStr == null || amountPaidStr.isEmpty())
                        ? 0.0
                        : Double.parseDouble(amountPaidStr);

                dbOperatorName = "DEFAULT"; // No card info here

            }

            else if ("PAYMENT_SUCCESS_WEBHOOK".equals(eventType)) {

                log.info("Handling PAYMENT_SUCCESS_WEBHOOK");

                JsonNode order = dataNode.path("order");
                payment = dataNode.path("payment");

                linkId = order.path("order_tags").path("link_id").asText(); // IMPORTANT
                status = payment.path("payment_status").asText();

                txnAmt = payment.path("payment_amount").asDouble(0.0);

                dbOperatorName = handlePaymentSuccessEvent(root);

            }

            else {
                log.warn("Ignoring unsupported event type={}", eventType);
                return;
            }

            String cfPaymentId = payment.path("cf_payment_id").asText();
            String webhookDeliveryKey = resolveWebhookDeliveryKey(cfPaymentId, idempotencyKey, eventType, linkId, status);

            if (!registerIdempotencyKey(webhookDeliveryKey, "delivery")) {
                return;
            }

            log.info("Extracted Data | linkId={}, status={}, amount={}, operator={}",
                    linkId, status, txnAmt, dbOperatorName);

            log.info("Webhook Data | linkId={}, status={}, amountPaid={}",
                    linkId, status, txnAmt);

            // ===== 3. FETCH TRANSACTION =====
            PgTransactionMapping txn =
                    pgTransactionMappingRepository
                            .findByOrderId(linkId)
                            .orElseThrow(() -> new RuntimeException("Transaction not found"));

            if (isSuccessStatus(status)) {
                if (!registerIdempotencyKey("CF_ORDER_SUCCESS:" + linkId, "order-success")) {
                    log.warn("Payment already processed for linkId={}. Skipping duplicate accounting.", linkId);
                    return;
                }
            } else if ("SUCCESS".equalsIgnoreCase(txn.getStatus())) {
                log.warn("Ignoring non-success event for already successful linkId={} | status={}", linkId, status);
                return;
            }

            // ===== 4. UPDATE STATUS =====
            switch (status) {

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
                    txn.setStatus("FAILED");
                    break;

                default:
                    log.warn("Unhandled status={}", status);
            }

            pgTransactionMappingRepository.save(txn);

            // ===== 5. FETCH SUB PRODUCT =====
            SubProducts subProducts =
                    subProductsRepository.findById(txn.getSubProductId())
                            .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            // ===== 6. FETCH SCHEME ALLOCATION =====
            SchemeAllocation schemeAllocation =
                    schemeAllocationRepository
                            .findByHierarchyIdAndSubProductCode(
                                    txn.getInitiatedBy(),
                                    subProducts.getSubProductCode()
                            )
                            .orElseThrow(() -> new RuntimeException("SchemeAllocation not found"));

            String schemeCode = schemeAllocation.getSchemeCode();

            List<SchemeRate> matchingSchemes =
                    schemeRateRepository.findBySchemeCodeAndOperatorName(
                            schemeCode,
                            dbOperatorName
                    );

            if (matchingSchemes == null || matchingSchemes.isEmpty()) {
                log.error("No matching scheme found");
                return;
            }

            // ===== 8. SELECT SCHEME =====
            double finalTxnAmt = txnAmt;
            SchemeRate selectedScheme = matchingSchemes.stream()
                    .filter(scheme ->
                            scheme.getSlabType() == SlabType.VARIABLE &&
                                    finalTxnAmt >= scheme.getSlabStart() &&
                                    finalTxnAmt <= scheme.getSlabEnd()
                    )
                    .findFirst()
                    .orElse(matchingSchemes.get(0));

            // ===== 9. FETCH SCHEME DETAILS =====
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme not found"));

            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];

            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            // ===== 10. PREPARE DTO =====
            String orderId = txn.getOrderId();
            String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

            dto.setBaseTranxId(baseTranxId);
            dto.setAmount(txnAmt);
            dto.setConfirmAmount(txnAmt);
            dto.setOperatorName(dbOperatorName);
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setHierarchyId(txn.getInitiatedBy());
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setVendorName("Cashfree PG");

            // ===== 11. PROCESS TRANSACTION =====
            transactionServiceImpl.processTransaction(dto, status);

            log.info("Transaction processed successfully | linkId={}, status={}",
                    linkId, txn.getStatus());

        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new RuntimeException(e);
        }
    }

    private boolean registerIdempotencyKey(String key, String keyType) {
        if (key == null || key.isBlank()) {
            log.warn("Skipping {} idempotency registration because key is blank", keyType);
            return true;
        }

        try {
            WebhookIdempotency entity = new WebhookIdempotency();
            entity.setIdempotencyId(key);
            entity.setCreatedAt(LocalDateTime.now());
            webhookIdempotencyRepository.saveAndFlush(entity);
            log.info("Registered {} idempotency key={}", keyType, key);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.warn("Duplicate {} webhook ignored for key={}", keyType, key);
            return false;
        }
    }

    private String resolveWebhookDeliveryKey(String cfPaymentId,
                                            String headerIdempotencyKey,
                                            String eventType,
                                            String linkId,
                                            String status) {
        if (cfPaymentId != null && !cfPaymentId.isBlank()) {
            return "CF_PAYMENT:" + cfPaymentId;
        }
        if (headerIdempotencyKey != null && !headerIdempotencyKey.isBlank()) {
            return "CF_HEADER:" + headerIdempotencyKey;
        }
        return "CF_EVENT:" + eventType + ":" + linkId + ":" + status;
    }

    private boolean isSuccessStatus(String status) {
        return "PAID".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status);
    }

    private String toDbOperatorName(String cardType, String cardNetwork) {
        log.info("<----------------------- Card Category: {}, PG Partner: {}", cardType, cardNetwork);

        return Stream.of(cardType, cardNetwork)
                .filter(s -> s != null && !s.isBlank() && !s.equalsIgnoreCase("NA") && !s.equalsIgnoreCase("N/A"))
                .map(s -> Arrays.stream(s.split("_"))
                        .filter(part -> !part.isBlank())
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(Collectors.joining("_")))
                .collect(Collectors.joining("_"));
    }

    private String handlePaymentSuccessEvent(JsonNode root) {

        JsonNode payment = root.path("data").path("payment");
        JsonNode card = payment.path("payment_method").path("card");

        String cardType = card.path("card_type").asText(null);
        String cardNetwork = card.path("card_network").asText(null);

        log.info("CardType={}, CardNetwork={}", cardType, cardNetwork);

        String operatorName = toDbOperatorName(cardType, cardNetwork);

        if (operatorName == null || operatorName.isBlank()) {
            operatorName = "DEFAULT";
        }

        log.info("Final OperatorName={}", operatorName);

        return operatorName;
    }
}
