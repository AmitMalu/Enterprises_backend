package com.im.admin.service.impl;

import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.paymentgateway.PGCreateOrderCommand;
import com.im.admin.repositories.*;
import com.im.admin.request.NixaSoftCallbackRequest;
import com.im.admin.request.NixaSoftCreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.NixaSoftCreateOrderResponse;
import com.im.admin.response.PgUnifiedOrderResponse;
import com.im.admin.service.NixaSoftPGService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /** Public base URL of the retailer portal, used for payment return/redirect links. */
    @Value("${app.portal.base-url:https://portal.nfttech.in/enterprise}")
    private String portalBaseUrl;


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

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String createOrder(NixaSoftCreateOrderRequest req, RequestHeaderMetaDto requestHeaderMetaDto) {

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

            // ===== 2. Generate Order ID =====
            String orderId = "ORD" + commonMethods.generateBaseTransactionId();

            // ===== 3. Handle AES Flow =====
            if ("AES".equals(credentials.getTokenType())) {
                return processAesCreateOrder(req, credentials, orderId, requestHeaderMetaDto, vendor);
            }

            // ===== 4. Handle Direct Vendor Flow =====
            return processDirectCreateOrder(req, credentials, orderId, retailer, requestHeaderMetaDto, vendor);

        } catch (HttpStatusCodeException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.error("Vendor HTTP Error | status={} | response={}", ex.getStatusCode(), errorBody);

            // Try to parse vendor error message
            try {
                Map<String, Object> errorMap = objectMapper.readValue(errorBody, Map.class);
                String errorMessage = (String) errorMap.getOrDefault("message", "Vendor error occurred");
                return "ERROR: " + errorMessage;
            } catch (Exception parseException) {
                return "ERROR: " + errorBody;
            }

        } catch (Exception e) {
            Throwable root = getRootCause(e);
            log.error("ROOT CAUSE => ", root, e);
            return "Error: " + root.getMessage();
        }
    }

/* =====================================================
   AES CREATE ORDER FLOW
   ===================================================== */

    private String processAesCreateOrder(
            NixaSoftCreateOrderRequest req,
            VendorCredentials credentials,
            String orderId,
            RequestHeaderMetaDto requestHeaderMetaDto,
            Vendors vendor) throws Exception {

        String token = commonMethods.generateTokenForAES(credentials);

        // Build JSON request for AES admin panel
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", credentials.getClientId());
        headers.set("token", token);
        headers.add("Authorization", "Bearer " + token);
        headers.add("Latitude", String.valueOf(requestHeaderMetaDto.getLatitude()));
        headers.add("Longitude", String.valueOf(requestHeaderMetaDto.getLongitude()));

        PGCreateOrderCommand command = new PGCreateOrderCommand();
        command.setAmount(Double.valueOf(req.getAmount()));
        command.setInitiatedBy(req.getInitiatedBy());
        command.setOrderId(orderId);
        command.setUsername(credentials.getUsername());
        command.setCardHolderName(req.getBeneficiaryName());
        command.setCardHolderMobile(req.getMobileNo());

        // Add transaction scheme placeholder if needed
        TransactionSchemeDetailsRequestDto scheme = new TransactionSchemeDetailsRequestDto();
        // TODO: fill required scheme fields
        command.setSchemeDetails(scheme);

        String uatUrl = credentials.getBaseUrlUat() + "/create-order";
        log.info("[NIXASOFT][AES] Calling AES Admin panel | url={}", uatUrl);

        HttpEntity<PGCreateOrderCommand> requestEntity = new HttpEntity<>(command, headers);

        ResponseEntity<CommonResponse<PgUnifiedOrderResponse>> response =
                restTemplate.exchange(
                        uatUrl,
                        HttpMethod.POST,
                        requestEntity,
                        new ParameterizedTypeReference<CommonResponse<PgUnifiedOrderResponse>>() {}
                );

        CommonResponse<PgUnifiedOrderResponse> responseBody = response.getBody();

        if (responseBody == null || responseBody.getData() == null) {
            log.error("[NIXASOFT][AES] Empty response from AES");
            return "ERROR: Empty response from AES vendor";
        }

        PgUnifiedOrderResponse pgResponse = responseBody.getData();

        commonFunctions.saveVendorLogs(
                LocalDateTime.now(),
                "AES order request",
                vendor.getId(),
                LocalDateTime.now(),
                pgResponse.toString(),
                null
        );

        // Save Transaction
        savePgTransaction(orderId, req, "PENDING", orderId, null, requestHeaderMetaDto);

        log.info("[NIXASOFT][AES] Order created successfully | orderId={}", orderId);
        return pgResponse.getPaymentLink();
    }

/* =====================================================
   DIRECT VENDOR CREATE ORDER FLOW
   ===================================================== */

    private String processDirectCreateOrder(
            NixaSoftCreateOrderRequest req,
            VendorCredentials credentials,
            String orderId,
            Retailer retailer,
            RequestHeaderMetaDto requestHeaderMetaDto,
            Vendors vendor) throws Exception {

        log.info("=== START: processDirectCreateOrder ===");
        log.info("Processing direct create order for orderId={}, amount={}, beneficiary={}, mobile={}",
                orderId, req.getAmount(), req.getBeneficiaryName(), req.getMobileNo());

        long startTime = System.currentTimeMillis();

        try {
            // ===== BUILD REQUEST BODY =====
            log.debug("Building request body for NixaSoft API");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("customer_name", req.getBeneficiaryName());
            requestBody.put("customer_phone", req.getMobileNo());
            requestBody.put("customer_email", retailer.getEmailId());
            requestBody.put("amount", req.getAmount());
            requestBody.put("redirect_url", portalBaseUrl + "/products/pgScreen");
            requestBody.put("requestId", orderId);

            log.debug("Request body built: customer_name={}, customer_phone={}, customer_email={}, amount={}, requestId={}",
                    req.getBeneficiaryName(), req.getMobileNo(), retailer.getEmailId(), req.getAmount(), orderId);

            // ===== BUILD HTTP HEADERS =====
            log.debug("Building HTTP headers for NixaSoft API");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", credentials.getToken());

            log.info("API Token: {}... (masked)", maskToken(credentials.getToken()));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // ===== CALL NIXASOFT API =====
            String apiUrl = credentials.getBaseUrlProd() + "/payin/premium-pg-3";
            log.info("[NIXASOFT] Calling NixaSoft Payin API | orderId={} | url={}", orderId, apiUrl);

            long apiStartTime = System.currentTimeMillis();

            // Get raw response as String first
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            long apiDuration = System.currentTimeMillis() - apiStartTime;
            log.info("[NIXASOFT] API call completed | orderId={} | statusCode={} | duration={} ms",
                    orderId, rawResponse.getStatusCode(), apiDuration);

            String responseBody = rawResponse.getBody();
            log.info("[NIXASOFT] Raw response: {}", responseBody);

            // ===== PARSE RESPONSE =====
            log.debug("Parsing response from NixaSoft API | orderId={}", orderId);

            Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
            String statusCode = (String) responseMap.get("statuscode");
            String statusMessage = (String) responseMap.getOrDefault("message", "No message");

            log.info("[NIXASOFT] Response status | orderId={} | statusCode={} | statusMessage={}",
                    orderId, statusCode, statusMessage);

            // ===== HANDLE ERROR RESPONSE =====
            if ("ERR".equals(statusCode)) {
                log.warn("[NIXASOFT] Received error response | orderId={} | statusCode={}", orderId, statusCode);

                String errorMessage = (String) responseMap.getOrDefault("message", "Transaction Failed");
                String errorCode = (String) responseMap.getOrDefault("error_code", "UNKNOWN");

                log.error("[NIXASOFT] Vendor returned error | orderId={} | errorCode={} | errorMessage={}",
                        orderId, errorCode, errorMessage);

                // Log full error details for debugging
                log.debug("[NIXASOFT] Full error response | orderId={} | response={}", orderId, responseBody);

                // Save failed transaction
                log.info("[NIXASOFT] Saving failed transaction | orderId={} | status=FAILED", orderId);

                savePgTransaction(orderId, req, "FAILED", null, retailer.getMobileNumber(), requestHeaderMetaDto);

                long totalDuration = System.currentTimeMillis() - startTime;
                log.info("[NIXASOFT] Error handling completed | orderId={} | totalDuration={} ms", orderId, totalDuration);
                log.info("=== END: processDirectCreateOrder (ERROR) ===");

                return "ERROR: " + errorMessage + " (Code: " + errorCode + ")";
            }

            // ===== HANDLE SUCCESS RESPONSE =====
            if ("TXN".equals(statusCode)) {
                log.info("[NIXASOFT] Received success response | orderId={} | statusCode={}", orderId, statusCode);

                NixaSoftCreateOrderResponse successResponse =
                        objectMapper.readValue(responseBody, NixaSoftCreateOrderResponse.class);

                log.info("[NIXASOFT] Success response details | orderId={} | vendorOrderId={} | paymentUrl={}",
                        orderId, successResponse.getOrderId(), successResponse.getPaymentUrl());

                if (successResponse.getPaymentUrl() == null || successResponse.getPaymentUrl().isEmpty()) {
                    log.warn("[NIXASOFT] Payment URL is null or empty | orderId={} | vendorOrderId={}",
                            orderId, successResponse.getOrderId());
                }

                // Save Transaction as PENDING
                log.info("[NIXASOFT] Saving pending transaction | orderId={} | vendorOrderId={} | status=PENDING",
                        orderId, successResponse.getOrderId());

                savePgTransaction(orderId, req, "PENDING", successResponse.getOrderId(), retailer.getMobileNumber(), requestHeaderMetaDto);

                long totalDuration = System.currentTimeMillis() - startTime;
                log.info("[NIXASOFT] Success processing completed | orderId={} | totalDuration={} ms | paymentUrl={}",
                        orderId, totalDuration, successResponse.getPaymentUrl());
                log.info("=== END: processDirectCreateOrder (SUCCESS) ===");

                return successResponse.getPaymentUrl();
            }

            // ===== HANDLE UNKNOWN STATUS =====
            log.error("[NIXASOFT] Unknown status code received | orderId={} | statusCode={} | fullResponse={}",
                    orderId, statusCode, responseBody);

            // Save transaction as FAILED for unknown status
            log.info("[NIXASOFT] Saving transaction as FAILED due to unknown status | orderId={}", orderId);
            savePgTransaction(orderId, req, "FAILED", null, retailer.getMobileNumber(), requestHeaderMetaDto);

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[NIXASOFT] Unknown status handling completed | orderId={} | totalDuration={} ms", orderId, totalDuration);
            log.info("=== END: processDirectCreateOrder (UNKNOWN STATUS) ===");

            return "ERROR: Unknown response status - " + statusCode;

        } catch (Exception e) {
            log.error("[NIXASOFT] Exception occurred while processing create order | orderId={} | error={}",
                    orderId, e.getMessage(), e);

            // Save transaction as FAILED
            try {
                log.info("[NIXASOFT] Saving failed transaction due to exception | orderId={}", orderId);
                savePgTransaction(orderId, req, "FAILED", null, retailer.getMobileNumber(), requestHeaderMetaDto);
            } catch (Exception saveEx) {
                log.error("[NIXASOFT] Failed to save transaction after exception | orderId={}", orderId, saveEx);
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("[NIXASOFT] Exception handling completed | orderId={} | totalDuration={} ms", orderId, totalDuration);
            log.info("=== END: processDirectCreateOrder (EXCEPTION) ===");

            throw new RuntimeException("Failed to process NixaSoft order: " + e.getMessage(), e);
        }
    }

    // ===== HELPER METHOD TO MASK TOKEN =====
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "****";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

/* =====================================================
   SAVE PG TRANSACTION (Common Method)
   ===================================================== */

    private void savePgTransaction(
            String orderId,
            NixaSoftCreateOrderRequest req,
            String status,
            String vendorSessionId,
            String mobileNumber,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        PgTransactionMapping transaction = new PgTransactionMapping();
        transaction.setOrderId(orderId);
        transaction.setAmount(Double.valueOf(req.getAmount()));
        transaction.setInitiatedBy(req.getInitiatedBy());
        transaction.setSubProductId(req.getSubProductId());
        transaction.setStatus(status);
        transaction.setVendorName("NixaSoft Payment Gateway");
        transaction.setVendorSessionId(vendorSessionId);
        transaction.setCardBinNo(req.getCardBinNo());
        transaction.setLatitude(requestHeaderMetaDto.getLatitude());
        transaction.setLongitude(requestHeaderMetaDto.getLongitude());
        transaction.setCreatedBy(mobileNumber);

        pgTransactionMappingRepository.save(transaction);

        log.info("[NIXASOFT] Transaction saved | orderId={} | status={}", orderId, status);
    }

/* =====================================================
   ROOT CAUSE EXTRACTOR
   ===================================================== */

    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root;
    }


//    @Override
//    public String createOrder(NixaSoftCreateOrderRequest req, Double latitude, Double longitude) {
//
//        log.info("Starting NixaSoft Create Order for subProductId={}, initiatedBy={}, amount={}",
//                req.getSubProductId(), req.getInitiatedBy(), req.getAmount());
//
//        try {
//
//            // ===== 1. Get Vendor Credentials =====
//            VendorRouting vendorRouting =
//                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());
//
//            // Find retailer by retailerId to get email and mobile
//            Retailer retailer = retailerRepository.findByRetailerId(req.getInitiatedBy())
//                    .orElseThrow(() -> new RuntimeException("Retailer not found for ID: " + req.getInitiatedBy()));
//
//            Vendors vendor = CommonMethods.getVendors(vendorRouting);
//
//            VendorCredentials credentials =
//                    vendorCredentialsRepository
//                            .findByVendorIdAndProductId(
//                                    vendor.getId(),
//                                    vendor.getProduct().getId())
//                            .orElseThrow(() -> new RuntimeException("No credentials found"));
//
//            String apiToken = credentials.getToken();
//
//            // ===== 2. Prepare Request =====
//            String orderId = "ORD" + commonMethods.generateBaseTransactionId();
//
//            if ("AES".equals(credentials.getTokenType())) {
//
//                String token = commonMethods.generateTokenForAES(credentials);
//
//                // Build JSON request for AES admin panel
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_JSON);
//                headers.set("client_id", credentials.getClientId());
//                headers.set("token", token);
//                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS
//                headers.add("Latitude", String.valueOf(latitude));
//                headers.add("Longitude", String.valueOf(longitude));
//
//                PGCreateOrderCommand command = new PGCreateOrderCommand();
//
//                command.setAmount(Double.valueOf(req.getAmount()));
//                command.setInitiatedBy(req.getInitiatedBy());
//                command.setOrderId(orderId);
//                command.setUsername(credentials.getUsername());
//                command.setCardHolderName(req.getBeneficiaryName());
//                command.setCardHolderMobile(req.getMobileNo());
//
//                // Add transaction scheme placeholder if needed
//                TransactionSchemeDetailsRequestDto scheme = new TransactionSchemeDetailsRequestDto();
//                // TODO: fill required scheme fields
//                command.setSchemeDetails(scheme);
//
//                String uatUrl = credentials.getBaseUrlUat() + "/create-order";
//                //String prodUrl = credentials.getBaseUrlProd() + "/create-order";
//
//                log.info("AES Admin panel order URL: {}", uatUrl);
//
//                HttpEntity<PGCreateOrderCommand> requestEntity =
//                        new HttpEntity<>(command, headers);
//
//                ResponseEntity<CommonResponse<PgUnifiedOrderResponse>> response =
//                        restTemplate.exchange(
//                                uatUrl,
//                                HttpMethod.POST,
//                                requestEntity,
//                                new ParameterizedTypeReference<CommonResponse<PgUnifiedOrderResponse>>() {
//                                }
//                        );
//
//                CommonResponse<PgUnifiedOrderResponse> responseBody = response.getBody();
//
//                PgUnifiedOrderResponse pgResponse = responseBody.getData();
//
//                commonFunctions.saveVendorLogs(
//                        LocalDateTime.now(),
//                        "AES order request",
//                        vendor.getId(),
//                        LocalDateTime.now(),
//                        pgResponse.toString(),
//                        null
//                );
//
//                // ===== 3. Save Transaction =====
//
//                PgTransactionMapping transaction = new PgTransactionMapping();
//                transaction.setOrderId(orderId);
//                transaction.setAmount(Double.valueOf(req.getAmount()));
//                transaction.setInitiatedBy(req.getInitiatedBy());
//                transaction.setSubProductId(req.getSubProductId());
//                transaction.setStatus("PENDING");
//                transaction.setVendorName("NixaSoft Payment Gateway");
//                transaction.setVendorSessionId(orderId);
//                transaction.setCardBinNo(req.getCardBinNo());
//                transaction.setLatitude(latitude);
//                transaction.setLongitude(longitude);
//
//                pgTransactionMappingRepository.save(transaction);
//
//                return response.getBody().getData().getPaymentLink();
//
//            }
//
//            Map<String, Object> requestBody = new HashMap<>();
//            requestBody.put("beneficiaryName", req.getBeneficiaryName());
//            requestBody.put("mobileNumber", req.getMobileNo());
//            requestBody.put("emailId", retailer.getEmailId());
//            requestBody.put("amount", req.getAmount());
//            requestBody.put("redirectUrl", "https://uat.nfttech.in/enterprise-portal/products/pgScreen");
//            requestBody.put("requestId", orderId);
//
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("apiToken", apiToken);
//
//            HttpEntity<Map<String, Object>> entity =
//                    new HttpEntity<>(requestBody, headers);
//
//            log.info("Calling NixaSoft Payin API");
//
//            ResponseEntity<NixaSoftCreateOrderResponse> response =
//                    restTemplate.exchange(
//                            credentials.getBaseUrlUat() + "/payin/nixa-silver-1",
//                            HttpMethod.POST,
//                            entity,
//                            NixaSoftCreateOrderResponse.class
//                    );
//
//            NixaSoftCreateOrderResponse body = response.getBody();
//
//            assert body != null;
//            log.info("NixaSoft response received statuscode={}, order_id={}",
//                    body.getStatusCode(), body.getOrderId());
//
//            // ===== 3. Save Transaction =====
//
//            PgTransactionMapping transaction = new PgTransactionMapping();
//            transaction.setOrderId(orderId);
//            transaction.setAmount(Double.valueOf(req.getAmount()));
//            transaction.setInitiatedBy(req.getInitiatedBy());
//            transaction.setSubProductId(req.getSubProductId());
//            transaction.setStatus(body.getStatusCode());
//            transaction.setVendorName("NixaSoft Payment Gateway");
//            transaction.setVendorSessionId(body.getOrderId());
//            transaction.setCardBinNo(req.getCardBinNo());
//            transaction.setLatitude(latitude);
//            transaction.setLongitude(longitude);
//
//            pgTransactionMappingRepository.save(transaction);
//
//            // ===== 4. Return Payment URL =====
//
//            if ("TXN".equals(body.getStatusCode())) {
//
//                log.info("Payment URL generated successfully orderId={}", orderId);
//                return body.getPaymentUrl();
//
//            } else {
//
//                log.error("Transaction failed for orderId={}", orderId);
//                throw new RuntimeException("Transaction Failed");
//
//            }
//
//        } catch (HttpStatusCodeException ex) {
//
//            String errorBody = ex.getResponseBodyAsString();
//
//            log.error("Vendor Error Response: {}", errorBody);
//
//            return errorBody;
//
//        } catch (Exception e) {
//
//            Throwable root = e;
//
//            while (root.getCause() != null) {
//                root = root.getCause();
//            }
//
//            log.error("ROOT CAUSE => ", root, e);
//
//            return "Error: " + root.getMessage();
//        }
//    }

    @Override
    @Transactional
    public void processCallback(NixaSoftCallbackRequest request) {

        log.info("NixaSoft Callback Started {}", request.toString());
        log.info("Processing NixaSoft callback for orderId={}", request.getOrderId());

        PgTransactionMapping pgTransactionMapping =
                pgTransactionMappingRepository
                        .findByOrderId(request.getRequestId())
                        .orElseThrow(() ->
                                new RuntimeException("Transaction not found"));

        // Prevent duplicate success processing
        if ("SUCCESS".equalsIgnoreCase(pgTransactionMapping.getStatus())) {
            log.warn(
                    "Transaction already marked SUCCESS for orderId={}",
                    request.getRequestId()
            );
            return;
        }

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

        log.info("---- [Callback] Using operatorName for DB lookup: {}", request.getPayment_mode());

        String operatorName;

        if ("consumer".equalsIgnoreCase(request.getSubtype())) {

            operatorName =
                    "credit_" +
                            request.getPayment_mode().toLowerCase() +
                            "_" +
                            request.getNetwork().toLowerCase();

        } else {

            operatorName =
                    "debit_" +
                            request.getPayment_mode().toLowerCase() +
                            "_" +
                            request.getNetwork().toLowerCase();
        }

        log.info("Generated operatorName={}", operatorName);

        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        operatorName
                );

        if (matchingSchemes == null || matchingSchemes.isEmpty()) {

            log.error(
                    "---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                    schemeCode,
                    operatorName
            );

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
        dto.setOperatorName(request.getPayment_mode());
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
        dto.setVendorName("NixaSoft Payment Gateway");
        dto.setMedium(pgTransactionMapping.getMedium());
        dto.setClientIp(pgTransactionMapping.getClientIp());
        dto.setRequestDomainName(pgTransactionMapping.getRequestDomainName());

        dto.setTransactionType("CREDIT");

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
