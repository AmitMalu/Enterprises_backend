package com.im.admin.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.controller.AESController;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.CreateOrderRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RunPaisaCallback;
import com.im.admin.service.PaymentGatewayService;
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
public class PaymentGatewayServiceImpl implements PaymentGatewayService {

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
    private TransactionServiceImpl transactionServiceImpl;

    @Autowired
    private TransactionSchemeRequestEntityRepository transactionSchemeRequestEntityRepository;

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

    @Value("${runpaisa.base.url.prod}")
    private String baseUrlPROD;

    @Value("${runpaisa.client.id}")
    private String clientId;

    @Value("${runpaisa.order.status.url.prod}")
    private String orderStatusUrl;

    @Autowired
    private CallbackParserFactory callbackParserFactory;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private AESController aesController;


    @Override
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

    @Override
    @Transactional
    public ResponseEntity<CommonResponse<String>> createOrder(CreateOrderRequest req) {
        log.info("Received createOrder request: {}", req);
        String orderId = "ORD" + commonMethods.generateBaseTransactionId();

        LocalDateTime imRequestTime = LocalDateTime.now();
        String imResponseBody = null;

        try {
            // ====== Vendor Routing & Credentials ======
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            log.debug("Vendor credentials retrieved: {}", credentials.getClientId());

            // ====== Save Transaction Mapping (Retailer Context) ======
            PgTransactionMapping txnMap = new PgTransactionMapping();
            txnMap.setOrderId(orderId);
            txnMap.setInitiatedBy(req.getInitiatedBy());  // <-- must come from logged-in user or request
            txnMap.setAmount(req.getAmount());
            txnMap.setStatus("INITIATED");
            txnMap.setVendorName(vendor.getVendorName());

            pgTransactionMappingRepository.save(txnMap);

            // ====== Check Vendor Type ======
            if ("AES".equalsIgnoreCase(credentials.getTokenType())) {
                // =========================
                // AES Flow (Admin Panel)
                // =========================
                String token = generateTokenForAES(credentials);

                // Build JSON request for AES admin panel
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);
                headers.add("Authorization", "Bearer " + token); // <--- ADD THIS

                CreateOrderRequest jsonBody = new CreateOrderRequest();
                jsonBody.setOrderId(orderId);
                jsonBody.setCallbackUrl(credentials.getCallbackUrl());
                jsonBody.setAmount(req.getAmount());
                jsonBody.setMerchantRef(req.getMerchantRef());
                jsonBody.setUsername(credentials.getUsername());

                // Add transaction scheme placeholder if needed
                TransactionSchemeDetailsRequestDto scheme = new TransactionSchemeDetailsRequestDto();
                // TODO: fill required scheme fields
                jsonBody.setTransactionScheme(scheme);

                HttpEntity<CreateOrderRequest> requestEntity = new HttpEntity<>(jsonBody, headers);

                String url = credentials.getBaseUrlUat() + "/create-order";
                log.info("AES Admin panel order URL: {}", url);

                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                imResponseBody = response.getBody();

                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "AES order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        imResponseBody,
                        null
                );

                return ResponseEntity.status(response.getStatusCode())
                        .body(CommonResponse.success(imResponseBody, "Order created via AES"));

            } else {
                // =========================
                // Old Vendor Flow (RunPaisa, Razorpay)
                // =========================
                String token = generateTokenForVendor(credentials);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                headers.set("client_id", credentials.getClientId());
                headers.set("token", token);

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                body.add("callbackurl", credentials.getCallbackUrl());
                body.add("order_id", orderId);
                body.add("amount", String.valueOf(req.getAmount()));
                if (req.getMerchantRef() != null) {
                    body.add("merc_unq_ref", req.getMerchantRef());
                }

                HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                //String url = credentials.getBaseUrlUat() + "/order";
                String url = credentials.getBaseUrlProd() + "/order";
                log.info("Old vendor order URL: {}", url);

                ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
                imResponseBody = response.getBody();

                txnMap.setStatus("REQUEST_SENT");
                pgTransactionMappingRepository.save(txnMap);

                commonFunctions.saveVendorLogs(
                        LocalDateTime.now(),
                        "Old vendor order request",
                        vendor.getId(),
                        LocalDateTime.now(),
                        imResponseBody,
                        null
                );

                ObjectMapper mapper = new ObjectMapper();
                JsonNode json = mapper.readTree(imResponseBody);

                String status = json.path("status").asText();
                String message = json.path("message").asText();

                if ("SUCCESS".equalsIgnoreCase(status)) {
                    // Success response
                    return ResponseEntity.ok(CommonResponse.success(imResponseBody, message));
                } else {
                    // Failure or any other status
                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(CommonResponse.handleErrors(message));
                }
            }

        } catch (Exception e) {
            log.error("Error while creating order for request: {}", req, e);
            imResponseBody = "Order creation failed: " + e.getMessage();

            // Save logs for failure
            commonFunctions.saveImLogs(req.toString(), imRequestTime, null, imResponseBody, LocalDateTime.now());

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure(imResponseBody));
        }
    }

    // =========================
    // Token generation methods
    // =========================

    private String generateTokenForAES(VendorCredentials vendorCredentials) {
        // Call AES encrypt service
        ResponseEntity<?> responseEntity = aesController.encryptCredentials(
                new EncryptDecryptCredentialsDto(vendorCredentials.getUsername(), vendorCredentials.getPassword())
        );

        @SuppressWarnings("unchecked")
        Map<String, String> encryptedMap = (Map<String, String>) responseEntity.getBody();
        String encUser = encryptedMap.get("cipherUsername");
        String encPass = encryptedMap.get("cipherPassword");
        String salt = encryptedMap.get("salt");
        String iv = encryptedMap.get("iv");

        // Build JSON for token request
        Map<String, String> jsonBody = new HashMap<>();
        jsonBody.put("username", encUser);
        jsonBody.put("password", encPass);
        jsonBody.put("client_id", vendorCredentials.getClientId());
        jsonBody.put("salt", salt);
        jsonBody.put("ivBase64", iv);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonBody, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(vendorCredentials.getTokenUrlUat(), requestEntity, Map.class);
        Map<String, Object> responseBodyMap = response.getBody();
        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
        }

        return (String) responseBodyMap.get("token");
    }

    private String generateTokenForVendor(VendorCredentials vendorCredentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", vendorCredentials.getUsername());
        headers.set("password", vendorCredentials.getPassword());
        headers.set("client_id", vendorCredentials.getClientId());

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(vendorCredentials.getTokenUrlProd(), requestEntity, Map.class);

        Map<String, Object> responseBodyMap = response.getBody();
        if (responseBodyMap == null || !responseBodyMap.containsKey("data")) {
            throw new RuntimeException("Old vendor token generation failed: " + responseBodyMap);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) responseBodyMap.get("data");
        return (String) dataMap.get("token");
    }


    @Override
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


    @Override
    public ResponseEntity<?> orderStatus(String orderId) {
        log.info("Initiating order status check for orderId: {}", orderId);

        try {
            // Step 1: Generate Token
            String token = generateToken();

            // Step 2: Build Multipart Request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", clientId);
            headers.set("token", token);

            // Step 3: Build request body
            Map<String, String> body = new HashMap<>();
            body.put("order_id", orderId);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Request body: {}", body);
            log.info("Headers: {}", headers);
            // Step 4: Send request using RestTemplate
            ResponseEntity<String> response = restTemplate.postForEntity(orderStatusUrl, requestEntity, String.class);

            log.info("Order status response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error while fetching order status: ", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Order status failed: " + e.getMessage());
        }
    }

    @Transactional
    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("---- [Callback] Received callback at: {} from IP: {}", requestTime, clientIp);
        log.info("---- [Callback] Raw Payload: {}", payload);

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


    @Override
    public void processCallback(RunPaisaCallback callback) throws JsonProcessingException {
        log.info("---- [Callback] Processing for Order ID: {}, Status: {}", callback.getOrderId(), callback.getStatus());

        // Fetch base transaction mapping
        log.info("---- [Callback] Fetching transaction mapping for Order ID: {}", callback.getOrderId());
        PgTransactionMapping pgTransactionMapping = pgTransactionMappingRepository.findByOrderId(callback.getOrderId())
                .orElseThrow(() -> new RuntimeException("Transaction not found for Order ID: " + callback.getOrderId()));

        // Prepare base data
        Double txnAmt = Double.valueOf(callback.getTxnAmount());
        log.info("---- [Callback] Parsed transaction amount: {}", txnAmt);

        String callbackOperatorName = toDbOperatorName(
                callback.getTxnMode(),
                callback.getCardCategory(),
                callback.getPgPartner()
        );
        log.info("---- [Callback] Using operatorName for DB lookup: {}", callbackOperatorName);

        // Fetch all matching schemes for operator name
        List<SchemeRate> matchingSchemes = schemeRateRepository.findSchemeByOperatorName(callbackOperatorName);
        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
            log.error("---- [Callback] No matching scheme found for operatorName: {}", callbackOperatorName);
            return;
        }
        log.info("---- [Callback] Scheme(s) found: {}", matchingSchemes.size());

        // Pick correct scheme by amount range or fixed value
        SchemeRate selectedScheme = matchingSchemes.stream()
                .filter(scheme ->
                        (scheme.getSlabType() == SlabType.FIXED && Objects.equals(scheme.getFixedSlabValue(), txnAmt)) ||
                                (scheme.getSlabType() == SlabType.VARIABLE && txnAmt >= scheme.getSlabStart() && txnAmt <= scheme.getSlabEnd())
                )
                .findFirst()
                .orElse(matchingSchemes.get(0));
        log.debug("---- [Callback] Selected scheme: {}", selectedScheme);

        // Fetch scheme, product & subproduct details
        log.debug("---- [Callback] Fetching scheme details for code: {}", selectedScheme.getSchemeCode());
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme Not Found for code: " + selectedScheme.getSchemeCode()));

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
        short productId = Short.parseShort(hashParts[0]);
        String subProductIdStr = hashParts[1];
        log.debug("---- [Callback] Extracted productId: {}, subProductId: {}", productId, subProductIdStr);

        Products product = productsRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
        SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

        // Build DTO (common for all statuses)
        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
        String orderId = callback.getOrderId();
        String baseTranxId = orderId.startsWith("ORD") ? orderId.substring(3) : orderId;

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

        log.debug("---- [Callback] DTO prepared: {}", dto);

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


    @Override
    public CommonResponse<String> validateTransaction(PgTransactionRequestDto request) {

        ProductTransactionLimit limit = getLimit(request.getProductId())
                .orElseThrow(() -> new RuntimeException("No transaction limits configured"));

        StoredProcedureQuery query = entityManager
                .createStoredProcedureQuery("check_pg_transaction_limit")
                .registerStoredProcedureParameter("wallet_type", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("in_card_bin_no", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("in_card_holder_mobile", String.class, ParameterMode.IN)
                .registerStoredProcedureParameter("transaction_amount", BigDecimal.class, ParameterMode.IN)
                .registerStoredProcedureParameter("daily_limit", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("monthly_limit", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("yearly_limit", Integer.class, ParameterMode.IN)
                .registerStoredProcedureParameter("daily_amt_limit", BigDecimal.class, ParameterMode.IN)
                .registerStoredProcedureParameter("result_msg", String.class, ParameterMode.OUT)
                .registerStoredProcedureParameter("is_valid", Boolean.class, ParameterMode.OUT);

        query.setParameter("wallet_type", request.getWalletType());
        query.setParameter("in_card_bin_no", request.getCardBinNo());
        query.setParameter("in_card_holder_mobile", request.getCardHolderMobileNumber());
        query.setParameter("transaction_amount", request.getAmount());
        query.setParameter("daily_limit", limit.getMaxAmount());
        query.setParameter("monthly_limit", limit.getMonthlyTransactionLimit());
        query.setParameter("yearly_limit", limit.getMaxAmount());
        query.setParameter("daily_amt_limit", limit.getMaxAmount());

        query.execute();

        String resultMsg = (String) query.getOutputParameterValue("result_msg");
        Boolean isValid = (Boolean) query.getOutputParameterValue("is_valid");

        return isValid
                ? CommonResponse.success("ALLOWED", resultMsg)
                : CommonResponse.failure(resultMsg);
    }

    public Optional<ProductTransactionLimit> getLimit(Short productId) {
        return productTransactionLimitRepository
                .findByProduct_Id(productId);
    }

}