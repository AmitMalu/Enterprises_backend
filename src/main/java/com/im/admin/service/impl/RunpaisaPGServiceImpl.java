package com.im.admin.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.commons.VendorContext;
import com.im.admin.controller.AESController;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PgStatusResponse;
import com.im.admin.response.RunPaisaCallback;
import com.im.admin.service.VendorCallbackParser;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
@LoggableVendor
public class RunpaisaPGServiceImpl {

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

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
    private ProductTransactionLimitRepository productTransactionLimitRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;


    RestTemplate restTemplate = new RestTemplate();

    @Value("${runpaisa.token.username}")
    private String runpaisaUsername;

    @Value("${runpaisa.token.password}")
    private String runpaisaPassword;

    @Value("${runpaisa.client.id}")
    private String clientId;

    @Value("${runpaisa.order.status.url}")
    private String orderStatusUrl;

    @Autowired
    private CallbackParserFactory callbackParserFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private AESController aesController;


    public String generateToken() {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", runpaisaUsername);
        headers.set("password", runpaisaPassword);
        headers.set("client_id", clientId);

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = "https://dev.api.runpaisa.com/token";
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PgTokenResponseDTO> response = restTemplate.postForEntity(
                url,
                requestEntity,
                PgTokenResponseDTO.class
        );

        PgTokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed to get token from Runpaisa");
        }
    }

    @Transactional
    public ResponseEntity<CommonResponse<String>> createOrder(CreateOrderRequest req, Double latitude, Double longitude) {
        // Entry log with request details
        log.info("=== START createOrder ===");
        log.info("Request: {}, Latitude: {}, Longitude: {}", req, latitude, longitude);

        String orderId = "ORD" + commonMethods.generateBaseTransactionId();
        log.info("Generated Order ID: {}", orderId);

        LocalDateTime imRequestTime = LocalDateTime.now();
        String imResponseBody = null;

        try {
            // ====== Vendor Routing & Credentials ======
            log.debug("Fetching vendor routing for subProductId: {}", req.getSubProductId());
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (vendorRouting == null) {
                log.error("No vendor routing found for subProductId: {}", req.getSubProductId());
                throw new RuntimeException("Vendor routing not found");
            }
            log.debug("Vendor routing found: {}", vendorRouting);

            Vendors vendor = CommonMethods.getVendors(vendorRouting);
            log.info("Selected Vendor: ID={}, Name={}, Product={}",
                    vendor.getId(), vendor.getVendorName(), vendor.getProduct() != null ? vendor.getProduct().getId() : "N/A");

            VendorContext.setVendorId(vendor.getVendorName());
            log.debug("Vendor context set with ID: {}", vendor.getVendorName());

            log.debug("Fetching credentials for vendorId: {} and productId: {}",
                    vendor.getId(), vendor.getProduct().getId());
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> {
                        log.error("No credentials found for vendorId: {}, productId: {}",
                                vendor.getId(), vendor.getProduct().getId());
                        return new RuntimeException("No credentials found for selected vendor");
                    });

            log.info("Vendor credentials retrieved successfully for clientId: {}",
                    credentials.getClientId());

            // ====== Save Transaction Mapping (Retailer Context) ======
            log.debug("Saving transaction mapping for orderId: {}", orderId);
            PgTransactionMapping txnMap = new PgTransactionMapping();
            txnMap.setOrderId(orderId);
            txnMap.setInitiatedBy(req.getInitiatedBy());
            txnMap.setAmount(req.getAmount());
            txnMap.setStatus("PENDING");
            txnMap.setVendorName(vendor.getVendorName());
            txnMap.setSubProductId(req.getSubProductId());
            txnMap.setCardBinNo(req.getCardBinNo());
            txnMap.setLatitude(latitude);
            txnMap.setLongitude(longitude);
            pgTransactionMappingRepository.save(txnMap);
            log.info("Transaction mapping saved with status: PENDING, InitiatedBy: {}", req.getInitiatedBy());
            log.info("Transaction details - SubProductId: {}, CardBinNo: {}, Location: ({}, {})",
                    req.getSubProductId(), req.getCardBinNo(), latitude, longitude);

            // ====== Check Vendor Type ======
            log.info("Vendor Token Type: {}", credentials.getTokenType());

            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {
                // =========================
                // AES Flow (Admin Panel)
                // =========================
                log.info("=== Starting AES Flow for orderId: {} ===", orderId);

                log.debug("Generating AES token for clientId: {}", credentials.getClientId());
                String token = commonMethods.generateTokenForAES(credentials);
                log.debug("AES token generated successfully");

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token);
                log.debug("AES headers prepared: client_id={}, token present={}",
                        credentials.getClientId(), token != null && !token.isEmpty());

                CreateOrderRequest jsonBody = new CreateOrderRequest();
                jsonBody.setOrderId(orderId);
                jsonBody.setCallbackUrl(credentials.getCallbackUrl());
                jsonBody.setAmount(req.getAmount());
                jsonBody.setMerchantRef(req.getMerchantRef());
                jsonBody.setUsername(credentials.getUsername());

                // Add transaction scheme placeholder if needed
                TransactionSchemeDetailsRequestDto scheme = new TransactionSchemeDetailsRequestDto();
                jsonBody.setTransactionScheme(scheme);
                log.debug("AES request body prepared - OrderId: {}, Amount: {}, CallbackUrl: {}",
                        orderId, req.getAmount(), credentials.getCallbackUrl());

                HttpEntity<CreateOrderRequest> requestEntity = new HttpEntity<>(jsonBody, headers);

                String url = credentials.getBaseUrlUat() + "/create-order";
                log.info("AES Order URL: {}", url);
                log.info("Sending AES create order request...");

                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                long endTime = System.currentTimeMillis();

                imResponseBody = response.getBody();
                log.info("AES response received - Status: {}, Time taken: {}ms",
                        response.getStatusCode(), (endTime - startTime));
                log.debug("AES Response Body: {}", imResponseBody != null ? imResponseBody : "NULL");

                log.debug("Saving AES vendor logs...");
                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "AES order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        imResponseBody,
                        null
                );
                log.info("AES vendor logs saved");

                log.info("=== AES Flow Completed Successfully for orderId: {} ===", orderId);
                return ResponseEntity.status(response.getStatusCode())
                        .body(CommonResponse.success(imResponseBody, "Order created via AES"));

            } else {
                // =========================
                // Old Vendor Flow (RunPaisa, Razorpay)
                // =========================
                log.info("=== Starting Legacy Vendor Flow for orderId: {} ===", orderId);
                log.info("Vendor type: {}", credentials.getTokenType());

                log.debug("Generating token for vendor: {}", credentials.getClientId());
                String token = generateTokenForVendor(credentials);
                log.debug("Vendor token generated successfully");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                log.debug("Vendor headers prepared: client_id={}", credentials.getClientId());

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("callbackurl", credentials.getCallbackUrl());
                body.add("order_id", orderId);
                body.add("amount", String.valueOf(req.getAmount()));
                if (req.getMerchantRef() != null) {
                    body.add("merc_unq_ref", req.getMerchantRef());
                    log.debug("Merchant reference added: {}", req.getMerchantRef());
                }
                log.debug("Vendor request body prepared - OrderId: {}, Amount: {}, CallbackUrl: {}",
                        orderId, req.getAmount(), credentials.getCallbackUrl());

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                String url = credentials.getBaseUrlProd() + "/order";
                log.info("Legacy Vendor Order URL: {}", url);
                log.info("Sending legacy vendor create order request...");

                long startTime = System.currentTimeMillis();
                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                long endTime = System.currentTimeMillis();

                imResponseBody = response.getBody();
                log.info("Legacy vendor response received - Status: {}, Time taken: {}ms",
                        response.getStatusCode(), (endTime - startTime));
                log.debug("Legacy Vendor Response Body: {}", imResponseBody != null ? imResponseBody : "NULL");

                log.debug("Updating transaction status to REQUEST_SENT for orderId: {}", orderId);
                txnMap.setStatus("REQUEST_SENT");
                pgTransactionMappingRepository.save(txnMap);
                log.info("Transaction status updated to REQUEST_SENT");

                log.debug("Saving legacy vendor logs...");
                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "Runpaisa PG ->" + requestEntity.toString(),
                        vendor.getId(),
                        LocalDateTime.now(),
                        imResponseBody,
                        null
                );
                log.info("Legacy vendor logs saved");

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(imResponseBody);
                String status = json.path("status").asText();
                String message = json.path("message").asText();
                log.info("Legacy vendor response parsing - Status: {}, Message: {}", status, message);

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    log.info("=== Legacy Vendor Flow Completed Successfully for orderId: {} ===", orderId);
                    return ResponseEntity.ok(CommonResponse.success(imResponseBody, message));
                } else {
                    log.warn("Legacy vendor request returned non-success status: {} for orderId: {}", status, orderId);
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(CommonResponse.handleErrors(message));
                }
            }

        } catch (Exception e) {
            log.error("=== ERROR in createOrder for orderId: {} ===", orderId, e);
            log.error("Error details - Request: {}, Message: {}, Cause: {}",
                    req, e.getMessage(), e.getCause() != null ? e.getCause().getMessage() : "N/A");

            imResponseBody = "Order creation failed: " + e.getMessage();
            log.error("Error response body: {}", imResponseBody);

            // Save logs for failure
            log.debug("Saving error IM logs...");
            commonFunctions.saveImLogs(req.toString(), imRequestTime, null, imResponseBody, LocalDateTime.now());
            log.info("Error IM logs saved for orderId: {}", orderId);

            log.error("=== createOrder Failed for orderId: {} ===", orderId);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure(imResponseBody));
        } finally {
            log.info("=== END createOrder for orderId: {} ===", orderId);
            // Clear thread-local context if needed
            VendorContext.clear();
            log.debug("Vendor context cleared");
        }
    }

    // =========================
    // Token generation methods
    // =========================

    private String generateTokenForVendor(VendorCredentials vendorCredentials) {
        log.info("=== START generateTokenForVendor ===");
        log.info("Generating token for vendor clientId: {}",
                maskSensitiveData(vendorCredentials.getClientId()));

        long startTime = System.currentTimeMillis();

        try {
            // Prepare headers
            log.debug("Preparing request headers for token generation");
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("username", vendorCredentials.getUsername());
            headers.set("password", vendorCredentials.getPassword());
            headers.set("client_id", vendorCredentials.getClientId());

            // Mask sensitive data in logs
            log.debug("Headers prepared - username: {}, clientId: {}, password: [PROTECTED]",
                    vendorCredentials.getUsername(),
                    maskSensitiveData(vendorCredentials.getClientId()));

            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String tokenUrl = vendorCredentials.getTokenUrlProd();
            log.info("Calling token generation endpoint: {}", tokenUrl);

            // Make API call
            log.debug("Sending token generation request...");
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tokenUrl,
                    requestEntity,
                    Map.class
            );

            long endTime = System.currentTimeMillis();
            log.info("Token generation API call completed - Status Code: {}, Time taken: {}ms",
                    response.getStatusCodeValue(), (endTime - startTime));

            // Validate response
            Map responseBodyMap = response.getBody();
            if (responseBodyMap == null) {
                log.error("Token generation response body is null for clientId: {}",
                        maskSensitiveData(vendorCredentials.getClientId()));
                throw new RuntimeException("Old vendor token generation failed: Response body is null");
            }

            log.debug("Response body received: {}", responseBodyMap);

            // Check for response structure
            if (!responseBodyMap.containsKey("data")) {
                log.error("Response missing 'data' field for clientId: {}. Response: {}",
                        maskSensitiveData(vendorCredentials.getClientId()),
                        responseBodyMap);
                throw new RuntimeException("Old vendor token generation failed: 'data' field missing in response");
            }

            // Extract token
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (Map<String, Object>) responseBodyMap.get("data");

            if (dataMap == null) {
                log.error("'data' field is null for clientId: {}",
                        maskSensitiveData(vendorCredentials.getClientId()));
                throw new RuntimeException("Old vendor token generation failed: 'data' field is null");
            }

            if (!dataMap.containsKey("token")) {
                log.error("Response missing 'token' field in data for clientId: {}. Data: {}",
                        maskSensitiveData(vendorCredentials.getClientId()),
                        dataMap);
                throw new RuntimeException("Old vendor token generation failed: 'token' field missing in data");
            }

            String token = (String) dataMap.get("token");

            if (token == null || token.isEmpty()) {
                log.error("Generated token is null or empty for clientId: {}",
                        maskSensitiveData(vendorCredentials.getClientId()));
                throw new RuntimeException("Old vendor token generation failed: Generated token is null or empty");
            }

            // Log partial token for reference (first 10 chars)
            log.info("Token generated successfully for clientId: {}, Token: {}",
                    maskSensitiveData(vendorCredentials.getClientId()),
                    token.length() > 10 ? token.substring(0, 10) + "..." : token);

            log.info("=== END generateTokenForVendor - Success ===");
            return token;

        } catch (Exception e) {
            log.error("=== ERROR in generateTokenForVendor for clientId: {} ===",
                    maskSensitiveData(vendorCredentials.getClientId()), e);
            log.error("Error details: {}", e.getMessage());

            // Log cause if available
            if (e.getCause() != null) {
                log.error("Cause: {}", e.getCause().getMessage());
            }

            // Rethrow with meaningful message
            throw new RuntimeException("Old vendor token generation failed: " + e.getMessage(), e);
        } finally {
            log.info("=== END generateTokenForVendor ===");
        }
    }

    /**
     * Utility method to mask sensitive data for logging
     */
    private String maskSensitiveData(String data) {
        if (data == null) return "null";
        if (data.length() <= 4) return "****";
        return data.substring(0, 2) + "****" + data.substring(data.length() - 2);
    }


    public CommonResponse<RetailerCommissionResponseDTO> fetchRetailerCommission(PaymentGatewayCardBinRequestDTO requestDTO) {

        // 1. Validate Card BIN
        CardTypeMaster cardTypeMaster = cardTypeMasterRepository
                .findByCardBinNo(requestDTO.getCardBinNo())
                .orElseThrow(() -> new RuntimeException("Invalid Card BIN number"));

        String cardType = cardTypeMaster.getCardType();
        String normalizedCardType = cardType.replaceAll("[^a-zA-Z]", "").toLowerCase();
        String operatorCode = normalizedCardType.contains("creditcard") ? "CC" : "DC";

        // 2. Find Operator
        Operators operator = operatorsRepository
                .findByProductCodeAndSubProductCodeAndOperatorCode(
                        requestDTO.getProductCode(),
                        requestDTO.getSubProductCode(),
                        operatorCode
                )
                .orElseThrow(() -> new RuntimeException("Operator not found"));

        // 3. Get SchemeAllocation by SchemeCode + Product + SubProduct + HierarchyId
        SchemeAllocation allocation = schemeAllocationRepository
                .findBySchemeCodeAndProductCodeAndSubProductCodeAndHierarchyId(
                        requestDTO.getSchemeCode(),      // <-- NEW: Scheme code from request
                        requestDTO.getProductCode(),
                        requestDTO.getSubProductCode(),
                        requestDTO.getAgentId()
                )
                .orElseThrow(() -> new RuntimeException("No scheme allocated to this retailer"));

        // 4. Fetch Scheme
        Scheme scheme = schemeRepository
                .findBySchemeCode(allocation.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme not found"));

        // 5. Fetch SchemeRate using schemeCode + operatorCode
        SchemeRate schemeRate = schemeRateRepository
                .findBySchemeCodeAndOperatorCode(
                        scheme.getSchemeCode(),
                        operator.getOperatorCode()
                );

        if (schemeRate == null) {
            throw new RuntimeException("Scheme rate not found for operator");
        }

        RetailerCommissionResponseDTO dto = new RetailerCommissionResponseDTO(
                cardType,
                operator.getOperatorCode(),
                scheme.getSchemeCode(),
                schemeRate.getRetailer(),
                schemeRate.getRateType()
        );

        return new CommonResponse<>(200, "Retailer commission fetched successfully", dto);
    }


    //@Override
    public PgStatusResponse orderStatus(String orderId) {

        log.info("Initiating order status check for orderId: {}", orderId);

        try {
            // ===== 1. Generate Token =====
            String token = generateToken();

            // ===== 2. Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", clientId);
            headers.set("token", token);

            // ===== 3. Body =====
            Map<String, String> body = new HashMap<>();
            body.put("order_id", orderId);

            HttpEntity<Map<String, String>> requestEntity =
                    new HttpEntity<>(body, headers);

            log.info("Request body: {}", body);

            // ===== 4. API Call =====
            ResponseEntity<Map> response = restTemplate.exchange(
                    orderStatusUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();

            log.info("Order status response: {}", responseBody);

            // ===== 5. Parse Response =====
            String vendorStatus = responseBody.get("status") != null
                    ? responseBody.get("status").toString()
                    : "PENDING";

            String txnId = responseBody.get("transaction_id") != null
                    ? responseBody.get("transaction_id").toString()
                    : null;

            Double amount = responseBody.get("amount") != null
                    ? Double.valueOf(responseBody.get("amount").toString())
                    : 0.0;

            // ===== 6. Map Status =====
            String status;
            if ("SUCCESS".equalsIgnoreCase(vendorStatus)
                    || "paid".equalsIgnoreCase(vendorStatus)) {
                status = "SUCCESS";
            } else if ("FAILED".equalsIgnoreCase(vendorStatus)) {
                status = "FAILED";
            } else {
                status = "PENDING";
            }

            // ===== 7. Return Common DTO =====
            return PgStatusResponse.builder()
                    .orderId(orderId)
                    .status(status)
                    .vendorTxnId(txnId)
                    .amount(amount)
                    .message("Order status fetched successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error while fetching order status", e);

            return PgStatusResponse.builder()
                    .orderId(orderId)
                    .status("FAILED")
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }

    @Transactional
    // @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("---- [Callback] Received callback at: {} from IP: {}", requestTime, clientIp);
        log.info("RunPaisa Callback Started {}", payload);

        String rawCallbackData = extractCallbackData(payload);

        if (rawCallbackData != null) {

            VendorCallbackParser parser = callbackParserFactory.getParser(payload);
            RunPaisaCallback callback = parser.parse(payload);
            processCallback(callback);

            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp, "Callback processed successfully", LocalDateTime.now());

            return "Callback processed successfully";
        } else {
            log.info("---- [Callback] Single callback format detected. Converting to RunPaisaCallback object.");
            RunPaisaCallback callback = convertToCallback(payload);
            processCallback(callback);
        }

        try {
            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp, "Callback processed successfully", LocalDateTime.now());

            log.info("---- [Callback] Successfully logged callback event");
            return "Callback processed successfully";

        } catch (Exception e) {
            log.error("---- [Callback] Error while saving callback log: {}", e.getMessage(), e);
            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp, e.getMessage(), LocalDateTime.now());

            throw new RuntimeException("Callback processing failed");
        }
    }


    // @Override
    public void processCallback(RunPaisaCallback callback) throws JsonProcessingException {
        log.info("---- [Callback] Processing for Order ID: {}, Status: {}", callback.getOrderId(), callback.getStatus());

        // Fetch base transaction mapping
        log.info("---- [Callback] Fetching transaction mapping for Order ID: {}", callback.getOrderId());
        PgTransactionMapping pgTransactionMapping = pgTransactionMappingRepository.findByOrderId(callback.getOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found for Order ID: " + callback.getOrderId()));

        String subProductId = pgTransactionMapping.getSubProductId();
        log.info("---- [Callback] Fetching SubProduct using subProductId: {}", subProductId);

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
        Double txnAmt = Double.valueOf(callback.getTxnAmount());
        log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);

        String callbackOperatorName = toDbOperatorName(
                callback.getTxnMode(),
                callback.getCardCategory(),
                callback.getPgPartner()
        );
        log.info("---- [Callback] Using operatorName for DB lookup: {}", callbackOperatorName);

        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        callbackOperatorName
                );

        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
            log.error("---- [Callback] No matching scheme found for schemeCode={} and operatorName={}",
                    schemeCode, callbackOperatorName);
            return;
        }

//        // Fetch all matching schemes for operator name
//        List<SchemeRate> matchingSchemes = schemeRateRepository.findSchemeByOperatorName(callbackOperatorName);
//        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
//            log.error("---- [Callback] No matching scheme found for operatorName: {}", callbackOperatorName);
//            return;
//        }
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

        String orderId = callback.getOrderId();
        String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

        String txnId = commonMethods.buildTransactionId("TXN", baseTranxId);

        PgTransactionMapping mapping = pgTransactionMappingRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Record not found for orderId: " + orderId)
                );

        mapping.setTxnId(txnId);

        pgTransactionMappingRepository.save(mapping);

        // Build DTO (common for all statuses)
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
//        String orderId = callback.getOrderId();
//        String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

        dto.setBaseTranxId(baseTranxId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
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
        dto.setVendorName("Runpaisa PG");
        dto.setVendorTxnId(callback.getBankTxnId());

        log.debug("---- [Callback] DTO prepared: {}", dto);
        log.info("---- [Callback] DTO prepared: {}", dto);

        // Forward to central transaction engine
        log.info("---- [Callback] Forwarding to Transaction Engine with Status: {}", callback.getStatus());
        transactionServiceImpl.processTransaction(dto, callback.getStatus());

        log.info("---- [Callback] Processing complete for Order ID: {} | Status: {}", callback.getOrderId(), callback.getStatus());
    }


    private String toDbOperatorName(String txnMode, String cardCategory, String pgPartner) {
        log.info("<-----------------------Txn Mode: {}, Card Category: {}, PG Partner: {}", txnMode, cardCategory, pgPartner);

        return Stream.of(txnMode, cardCategory, pgPartner)
                .filter(s -> s != null && !s.isBlank() && !s.equalsIgnoreCase("NA") && !s.equalsIgnoreCase("N/A"))
                .map(s -> Arrays.stream(s.split("_"))
                        .filter(part -> !part.isBlank())
                        .map(part -> part.substring(0, 1).toUpperCase() + part.substring(1).toLowerCase())
                        .collect(Collectors.joining("_")))
                .collect(Collectors.joining("_"));
    }

    private String extractCallbackData(Map<String, String> payload) {
        log.info("---- [Callback] Extracting callback data from payload...");

        if (payload.containsKey("CALLBACKRESPONSE")) {
            String callbackResponse = payload.get("CALLBACKRESPONSE");
            log.info("---- [Callback] CALLBACKRESPONSE found (Razorpay): {}", callbackResponse);
            return callbackResponse;
        } else if (payload.containsKey("CALLBACK")) {
            String callback = payload.get("CALLBACK");
            log.info("---- [Callback] CALLBACK found (Cashfree): {}", callback);
            return callback;
        }

        log.warn("---- [Callback] No recognized callback data found in payload.");
        return null;
    }

    private RunPaisaCallback convertToCallback(Map<String, String> payload) {
        log.info("---- [Callback] Converting raw payload to RunPaisaCallback object...");

        RunPaisaCallback callback = new RunPaisaCallback();
        callback.setStatus(payload.get("STATUS"));
        callback.setStatusCode(payload.get("STATUS_CODE"));
        callback.setOrderId(payload.get("ORDER_ID"));
        callback.setTxnMode(payload.get("TXN_MODE"));
        callback.setTxnAmount(payload.get("TXN_AMOUNT"));
        callback.setCardCategory(payload.get("CARD_CATEGORY"));
        callback.setTxnDate(payload.get("TXN_DATE"));
        callback.setTxnInfo(payload.get("TXN_INFO"));
        callback.setCustomerName(payload.get("CUSTOMER_NAME"));
        callback.setCustomerEmail(payload.get("CUSTOMER_EMAIL"));
        callback.setCustomerPhone(payload.get("CUSTOMER_PHONE"));
        callback.setBankTxnId(payload.get("BANK_TXNID"));
        callback.setBankCode(payload.get("BANK_CODE"));
        callback.setErrorId(payload.get("ERROR_ID"));
        callback.setErrorDesc(payload.get("ERROR_DESC"));
        callback.setCardNumber(payload.get("CARD_NUMBER"));
        callback.setCardType(payload.get("CARD_TYPE"));
        callback.setUnmappedStatus(payload.get("UNMAPPED_STATUS"));
        callback.setPgPartner(payload.get("PG_PARTNER"));
        callback.setMercUnqRef(payload.get("MERC_UNQ_REF"));

        log.info("---- [Callback] Callback object created: {}", callback);
        return callback;
    }

    public CommonResponse<String> validateTransaction(PgTransactionRequestDto request) {

        ProductTransactionLimit limit = getLimit(request.getProductId(), request.getSubProductId())
                .orElseThrow(() -> new RuntimeException("No transaction limits configured"));

        if(request.getAmount() == null
            || limit.getMinAmount() == null
                || limit.getMaxAmount() == null
                || request.getAmount().compareTo(limit.getMinAmount()) < 0
                || request.getAmount().compareTo(limit.getMaxAmount()) > 0) {

            return CommonResponse.failure(
                    "Please enter an amount between ₹" + limit.getMinAmount() +
                            " and ₹" + limit.getMaxAmount()
            );
        }

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("check_pg_transaction_limit");

        // Register parameters by POSITION (VERY IMPORTANT)
        query.registerStoredProcedureParameter(1, String.class, ParameterMode.IN); // wallet_type
        query.registerStoredProcedureParameter(2, String.class, ParameterMode.IN); // bin
        query.registerStoredProcedureParameter(3, String.class, ParameterMode.IN); // mobile
        query.registerStoredProcedureParameter(4, BigDecimal.class, ParameterMode.IN); // amount
        query.registerStoredProcedureParameter(5, Integer.class, ParameterMode.IN); // daily_limit
        query.registerStoredProcedureParameter(6, Integer.class, ParameterMode.IN); // monthly_limit
        query.registerStoredProcedureParameter(7, Integer.class, ParameterMode.IN); // yearly_limit
        query.registerStoredProcedureParameter(8, BigDecimal.class, ParameterMode.IN); // daily_amt_limit
        query.registerStoredProcedureParameter(9, String.class, ParameterMode.OUT); // result_msg
        query.registerStoredProcedureParameter(10, Boolean.class, ParameterMode.OUT); // is_valid

        // Set values
        query.setParameter(1, request.getWalletType());
        query.setParameter(2, request.getCardBinNo());
        query.setParameter(3, request.getCardHolderMobileNumber());
        query.setParameter(4, request.getAmount());

        // FIX TYPES
        query.setParameter(5, safeParseInt(limit.getPerDayTransactionLimit()));
        query.setParameter(6, safeParseInt(limit.getMonthlyTransactionLimit()));
        query.setParameter(7, safeParseInt(limit.getMonthlyTransactionLimit()));
        query.setParameter(8, limit.getMaxAmount());

        query.execute();

        String resultMsg = (String) query.getOutputParameterValue(9);
        Boolean isValid = (Boolean) query.getOutputParameterValue(10);

        return isValid
                ? CommonResponse.success("ALLOWED", resultMsg)
                : CommonResponse.failure(resultMsg);
    }

    public Optional<ProductTransactionLimit> getLimit(Short productId, String subProductId) {
        return productTransactionLimitRepository
                .findByProduct_IdAndSubProduct_Id(productId, subProductId);
    }

    private Integer safeParseInt(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return 0; // or null depending on your logic
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0; // fallback
        }
    }

}