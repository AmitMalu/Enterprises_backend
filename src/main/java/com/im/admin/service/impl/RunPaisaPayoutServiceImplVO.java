package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.callbacks.RunpaisaPayoutCallback;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.controller.AESController;
import com.im.admin.dto.EncryptDecryptCredentialsDto;
import com.im.admin.dto.PayoutTokenResponseDTO;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.response.RunPaisaCallback;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutCallbackRegistrar;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service("Runpaisa PAYOUT")
@Slf4j
@LoggableVendor
public class RunPaisaPayoutServiceImplVO implements PayoutVendorService, PayoutCallbackRegistrar, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AESController aesController;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public String getVendorName() {
        return "Runpaisa PAYOUT";
    }



    @Override
    @Transactional
    public PayoutResponseDto sendPayout(SendPayoutRequest req,
                                        VendorCredentials creds,
                                        String orderId,
                                        SubProducts subProduct,
                                        WalletType walletType,
                                        Double latitude,
                                        Double longitude) {
        log.info("[RUNPAISA] Starting payout for orderId={} | amount={}",
                orderId, req.getAmount());

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            // =========================================================
            // Handle AES Token Flow (Admin Panel)
            // =========================================================
            if ("AES".equalsIgnoreCase(creds.getTokenType())) {
                return processAesPayout(req, creds, orderId, latitude, longitude);
            }

            // =========================================================
            // Direct Vendor Payout Flow
            // =========================================================
            return processDirectVendorPayout(req, creds, orderId, latitude, longitude);

        } catch (HttpStatusCodeException ex) {
            String vendorResponse = ex.getResponseBodyAsString();

            log.error("[RUNPAISA] Vendor API Error | orderId={} | response={}",
                    orderId, vendorResponse);

            return failed(orderId, vendorResponse);

        } catch (Exception e) {
            Throwable root = getRootCause(e);

            log.error("[RUNPAISA] Internal payout error | orderId={} | error={}",
                    orderId, root.getMessage(), e);

            return failed(orderId, root.getMessage());

        } finally {
            String vendorId = creds.getVendor().getId();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
        }
    }

    /* =====================================================
       COMMON FAILED RESPONSE
       ===================================================== */

    private PayoutResponseDto failed(String orderId, String message) {
        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(getVendorName())
                .status("FAILED")
                .message(message != null ? message : "Something went wrong")
                .build();
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

    /* =====================================================
       AES PAYOUT FLOW (Matching NixaSoft Pattern)
       ===================================================== */

    private PayoutResponseDto processAesPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String baseTxnId,
            Double latitude,
            Double longitude) throws Exception {

        String token = generateTokenForAES(creds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", creds.getClientId());
        headers.set("token", token);
        headers.setBearerAuth(token);
        headers.set("latitude", String.valueOf(latitude));
        headers.set("longitude", String.valueOf(longitude));

        // Handle TXN prefix like NixaSoft
        String orderId = baseTxnId.startsWith("TXN") ? baseTxnId.substring(3) : baseTxnId;

        SendPayoutRequest payoutRequest = new SendPayoutRequest();
        payoutRequest.setAmount(req.getAmount());
        payoutRequest.setOrderId(orderId);
        payoutRequest.setIfscCode(req.getIfscCode());
        payoutRequest.setAccountNumber(req.getAccountNumber());
        payoutRequest.setBankName(req.getBankName());
        payoutRequest.setPaymentMode(req.getPaymentMode());
        payoutRequest.setBeneficiaryName(req.getBeneficiaryName());
        payoutRequest.setPartnerId(creds.getUsername());

        HttpEntity<SendPayoutRequest> requestEntity = new HttpEntity<>(payoutRequest, headers);

        String prodUrl = creds.getBaseUrlProd();
        log.info("[RUNPAISA][AES] Calling AES payout API | url={}", prodUrl);

        ResponseEntity<CommonResponse<PayoutResponseDto>> response = restTemplate.exchange(
                prodUrl + "/api/send-payout",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<CommonResponse<PayoutResponseDto>>() {}
        );

        CommonResponse<PayoutResponseDto> responseBody = response.getBody();
        log.info("[RUNPAISA][AES] AES payout response => {}", responseBody);

        if (responseBody == null || responseBody.getData() == null) {
            return failed("TXN" + baseTxnId, "Empty response from AES vendor");
        }

        log.info("[RUNPAISA][AES] AES payout completed | orderId={} | status={}",
                "TXN" + baseTxnId,
                responseBody.getData().getStatus());

        // Save pending transaction (like NixaSoft)
        savePendingTransaction(req, orderId, latitude, longitude);

        return PayoutResponseDto.builder()
                .orderId("TXN" + baseTxnId)
                .vendorName(getVendorName())
                .status(responseBody.getData().getStatus())
                .amount(req.getAmount())
                .message(responseBody.getMessage())
                .build();
    }

    /* =====================================================
       DIRECT RUNPAISA PAYOUT FLOW
       ===================================================== */

    private PayoutResponseDto processDirectVendorPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            Double latitude,
            Double longitude) throws Exception {

        // Build headers
        HttpHeaders headers = buildPayoutHeaders(creds);

        // Build request body
        MultiValueMap<String, Object> body = buildPayoutRequestBody(req, orderId, creds);

        String url = creds.getBaseUrlProd() + "/payment";
        log.info("[RUNPAISA] Sending payout request to: {}", url);

        // Call vendor API
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        // Handle vendor response
        return handleVendorResponse(response.getBody(), req, orderId, latitude, longitude);
    }

    /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPayoutHeaders(VendorCredentials creds) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", creds.getClientId());
        headers.set("token", generateTokenForPayout(creds));
        return headers;
    }

    /* =====================================================
       BUILD PAYOUT REQUEST BODY
       ===================================================== */

    private MultiValueMap<String, Object> buildPayoutRequestBody(
            SendPayoutRequest req,
            String orderId,
            VendorCredentials creds) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("beneficiaryName", req.getBeneficiaryName());
        body.add("orderId", orderId);
        body.add("amount", req.getAmount());
        body.add("beneficiaryAccountNumber", req.getAccountNumber());
        body.add("beneficiaryIfscCode", req.getIfscCode());
        body.add("paymentMode", req.getPaymentMode());
        body.add("callbackurl", creds.getCallbackUrl());
        return body;
    }

    /* =====================================================
       HANDLE VENDOR RESPONSE
       ===================================================== */

    private PayoutResponseDto handleVendorResponse(
            Map<String, Object> vendorResp,
            SendPayoutRequest req,
            String orderId,
            Double latitude,
            Double longitude) {

        log.info("[RUNPAISA] Raw response: {}", vendorResp);

        if (vendorResp == null || vendorResp.isEmpty()) {
            log.warn("[RUNPAISA] Empty or invalid response from vendor");
            return failed(orderId, "Empty or invalid response from vendor");
        }

        String vendorCodeResp = (String) vendorResp.get("code");
        String vendorStatus = (String) vendorResp.get("status");
        String vendorMessage = (String) vendorResp.get("message");

        boolean accepted = "RP000".equals(vendorCodeResp)
                && "SUCCESS".equalsIgnoreCase(vendorStatus);

        if (!accepted) {
            return failed(orderId, vendorMessage);
        }

        // Save pending transaction
        savePendingTransaction(req, orderId, latitude, longitude);

        log.info("[RUNPAISA] Transaction saved | TxnID={} | Status={}", orderId, vendorStatus);

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(getVendorName())
                .status("PENDING")
                .message(vendorMessage)
                .amount(req.getAmount())
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .ifscCode(req.getIfscCode())
                .build();
    }

    /* =====================================================
       SAVE PENDING TRANSACTION
       ===================================================== */

    private void savePendingTransaction(
            SendPayoutRequest req,
            String orderId,
            Double latitude,
            Double longitude) {

        PayoutPendingTransactions pending = new PayoutPendingTransactions();
        pending.setTransactionId(orderId);
        pending.setSchemeCode(req.getSchemeCode());
        pending.setRetailerId(req.getRetailerId());
        pending.setAmount(req.getAmount());
        pending.setTransactionType(req.getPaymentMode());
        pending.setStatus("PENDING");
        pending.setCreatedAt(LocalDateTime.now());
        pending.setIfscCode(req.getIfscCode());
        pending.setAccountNumber(req.getAccountNumber());
        pending.setBeneficiaryName(req.getBeneficiaryName());
        pending.setLatitude(latitude);
        pending.setLongitude(longitude);

        payoutPendingTransactionRepository.save(pending);
    }

    /* =====================================================
       TOKEN GENERATION METHODS
       ===================================================== */

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

        ResponseEntity<Map> response = restTemplate.postForEntity(
                vendorCredentials.getTokenUrlUat(),
                requestEntity,
                Map.class
        );

        Map<String, Object> responseBodyMap = response.getBody();
        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
        }

        return (String) responseBodyMap.get("token");
    }

    public String generateTokenForPayout(VendorCredentials vendorCredentials) throws JsonProcessingException {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = vendorCredentials.getVendor().getVendorCode();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", vendorCredentials.getClientId());
            headers.set("client_secret", vendorCredentials.getClientSecret());

            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
            requestJson = "Headers: " + requestEntity.getHeaders();

            String url = vendorCredentials.getTokenUrlProd();
            log.info("[RUNPAISA] Sending token request to URL: {}", url);

            ResponseEntity<Map> rawResponse = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    Map.class
            );

            log.info("[RUNPAISA] Token Generation Raw Response: {}", rawResponse.getBody());
            responseJson = objectMapper.writeValueAsString(rawResponse.getBody());

            PayoutTokenResponseDTO tokenResponse = objectMapper.convertValue(
                    rawResponse.getBody(),
                    PayoutTokenResponseDTO.class
            );

            log.info("[RUNPAISA] Bearer token response: {}", tokenResponse);

            PayoutTokenResponseDTO.TokenData tokenData = tokenResponse.getTokenData();
            if (tokenData != null && tokenData.getToken() != null) {
                return tokenData.getToken();
            } else {
                throw new RuntimeException("Failed To Get Token From Payout Vendor - Response: " + rawResponse.getBody());
            }

        } finally {
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
        }
    }

//    @Override
//    @Transactional
//    public PayoutResponseDto sendPayout(SendPayoutRequest req,
//                                        VendorCredentials creds,
//                                        String orderId,
//                                        SubProducts subProduct,
//                                        WalletType walletType,
//                                        Double latitude,
//                                        Double longitude) {
//        log.info("[RUNPAISA] Starting payout for orderId={}", orderId);
//
//        LocalDateTime requestTime = LocalDateTime.now();
//        String requestJson = null;
//        String responseJson = null;
//
//        try {
//            // =========================================================
//            // Handle AES Token Flow (Admin Panel)
//            // =========================================================
//            if ("AES".equalsIgnoreCase(creds.getTokenType())) {
//                String token = generateTokenForAES(creds);
//
//                HttpHeaders headers = new HttpHeaders();
//                headers.setContentType(MediaType.APPLICATION_JSON);
//                headers.set("client_id", creds.getClientId());
//                headers.set("token", token);
//                headers.add("Authorization", "Bearer " + token);
//
//                SendPayoutRequest aesRequest = new SendPayoutRequest();
//                aesRequest.setAccountNumber(req.getAccountNumber());
//                aesRequest.setIfscCode(req.getIfscCode());
//                aesRequest.setAmount(req.getAmount());
//                aesRequest.setOrderId(orderId);
//                aesRequest.setBeneficiaryName(req.getBeneficiaryName());
//                aesRequest.setPaymentMode(req.getPaymentMode());
//                aesRequest.setCallbackUrl(req.getCallbackUrl());
//
//                String url = creds.getBaseUrlUat() + "/payment";
//                log.info("[RUNPAISA][AES] Sending payout request to {}", url);
//
//                restTemplate.exchange(
//                        url,
//                        HttpMethod.POST,
//                        new HttpEntity<>(aesRequest, headers),
//                        String.class
//                );
//
//                // Return immediately for AES, since callback will confirm later
//                return PayoutResponseDto.builder()
//                        .orderId(orderId)
//                        .vendorName(getVendorName())
//                        .status("PENDING")
//                        .message("AES Payout initiated. Awaiting callback confirmation.")
//                        .amount(req.getAmount())
//                        .build();
//            }
//
//            // =========================================================
//            // Prepare Vendor Request
//            // =========================================================
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("client_id", creds.getClientId());
//            headers.set("token", generateTokenForPayout(creds));
//
//            // multipart/form-data body
//            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
//            body.add("beneficiaryName", req.getBeneficiaryName());
//            body.add("orderId", orderId);
//            body.add("amount", req.getAmount());
//            body.add("beneficiaryAccountNumber", req.getAccountNumber());
//            body.add("beneficiaryIfscCode", req.getIfscCode());
//            body.add("paymentMode", req.getPaymentMode()); // IMPS / NEFT
//            body.add("callbackurl", creds.getCallbackUrl());
//
//            // Convert request to JSON for logging
//            requestJson = objectMapper.writeValueAsString(body);
//
//            String url = creds.getBaseUrlProd() + "/payment";
//            log.info("[RUNPAISA] Sending payout request to: {}", url);
//
//            ResponseEntity<Map> response = restTemplate.exchange(
//                    url,
//                    HttpMethod.POST,
//                    new HttpEntity<>(body, headers),
//                    Map.class
//            );
//
//            // Convert response to JSON
//            responseJson = objectMapper.writeValueAsString(response.getBody());
//
//            // =========================================================
//            // Parse Vendor Response
//            // =========================================================
//            Map<String, Object> vendorResp = response.getBody();
//            log.info("[RUNPAISA] Raw response: {}", vendorResp);
//
//            if (vendorResp == null || vendorResp.isEmpty()) {
//                log.warn("[RUNPAISA] Empty or invalid response from vendor");
//                return PayoutResponseDto.builder()
//                        .orderId(orderId)
//                        .vendorName(getVendorName())
//                        .status("FAILED")
//                        .message("Empty or invalid response from vendor")
//                        .amount(req.getAmount())
//                        .build();
//            }
//
//            String vendorCodeResp = (String) vendorResp.get("code");
//            String vendorStatus = (String) vendorResp.get("status");
//            String vendorMessage = (String) vendorResp.get("message");
//
//            boolean accepted = "RP000".equals(vendorCodeResp)
//                    && "SUCCESS".equalsIgnoreCase(vendorStatus);
//
//            if (!accepted) {
//                return PayoutResponseDto.builder()
//                        .orderId(orderId)
//                        .vendorName(getVendorName())
//                        .status("FAILED")
//                        .message(vendorMessage)
//                        .amount(req.getAmount())
//                        .build();
//            }
//
//            // =========================================================
//            // Save Pending Transaction
//            // =========================================================
//            PayoutPendingTransactions pending = new PayoutPendingTransactions();
//            pending.setTransactionId(orderId);
//            pending.setSchemeCode(req.getSchemeCode());
//            pending.setRetailerId(req.getRetailerId());
//            pending.setAmount(req.getAmount());
//            pending.setTransactionType(req.getPaymentMode());
//            pending.setStatus("PENDING");
//            pending.setCreatedAt(LocalDateTime.now());
//            pending.setIfscCode(req.getIfscCode());
//            pending.setAccountNumber(req.getAccountNumber());
//            pending.setBeneficiaryName(req.getBeneficiaryName());
//            pending.setLatitude(latitude);
//            pending.setLongitude(longitude);
//            payoutPendingTransactionRepository.save(pending);
//
//            log.info("[RUNPAISA] Transaction saved | TxnID={} | Status={}", orderId, vendorStatus);
//
//            // Adapt Vendor → Unified DTO
//            return PayoutResponseDto.builder()
//                    .orderId(orderId)              // USE YOUR ORDER ID
//                    .vendorName(getVendorName())
//                    .status(accepted ? "PENDING" : "FAILED")
//                    .message(vendorMessage)
//                    .amount(req.getAmount())
//                    .beneficiaryName(req.getBeneficiaryName())
//                    .accountNumber(req.getAccountNumber())
//                    .ifscCode(req.getIfscCode())
//                    .build();
//
//        } catch (Exception e) {
//            log.error("[RUNPAISA] Exception during payout: {}", e.getMessage(), e);
//            return PayoutResponseDto.builder()
//                    .orderId(orderId)
//                    .vendorName(getVendorName())
//                    .status("FAILED")
//                    .message("RunPaisa error: " + e.getMessage())
//                    .amount(req.getAmount())
//                    .build();
//        } finally {
//
//            String vendorId = creds.getVendor().getId();
//
//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    responseJson,
//                    null
//            );
//        }
//    }

//    private String generateTokenForAES(VendorCredentials vendorCredentials) {
//        // Call AES encrypt service
//        ResponseEntity<?> responseEntity = aesController.encryptCredentials(
//                new EncryptDecryptCredentialsDto(vendorCredentials.getUsername(), vendorCredentials.getPassword())
//        );
//
//        @SuppressWarnings("unchecked")
//        Map<String, String> encryptedMap = (Map<String, String>) responseEntity.getBody();
//        String encUser = encryptedMap.get("cipherUsername");
//        String encPass = encryptedMap.get("cipherPassword");
//        String salt = encryptedMap.get("salt");
//        String iv = encryptedMap.get("iv");
//
//        // Build JSON for token request
//        Map<String, String> jsonBody = new HashMap<>();
//        jsonBody.put("username", encUser);
//        jsonBody.put("password", encPass);
//        jsonBody.put("client_id", vendorCredentials.getClientId());
//        jsonBody.put("salt", salt);
//        jsonBody.put("ivBase64", iv);
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(jsonBody, headers);
//
//        ResponseEntity<Map> response = restTemplate.postForEntity(vendorCredentials.getTokenUrlUat(), requestEntity, Map.class);
//        Map<String, Object> responseBodyMap = response.getBody();
//        if (responseBodyMap == null || responseBodyMap.get("token") == null) {
//            throw new RuntimeException("AES token generation failed: " + responseBodyMap);
//        }
//
//        return (String) responseBodyMap.get("token");
//    }
//
//
//    public String generateTokenForPayout(VendorCredentials vendorCredentials) throws JsonProcessingException {
//
//        LocalDateTime requestTime = LocalDateTime.now();
//        String requestJson = null;
//        String responseJson = null;
//        String vendorId = vendorCredentials.getVendor().getVendorCode();
//
//        try {
//            // Set headers only
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.set("client_id", vendorCredentials.getClientId());
//            headers.set("client_secret", vendorCredentials.getClientSecret());
//            //headers.set("username", vendorCredentials.getUsername());
//            //headers.set("password", vendorCredentials.getPassword());
//
//            HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);
//
//            requestJson = "Headers: " + requestEntity.getHeaders();
//
//            String url = vendorCredentials.getTokenUrlProd();
//            log.info("Sending token request to URL: {}", url);
//
//            log.info("Step 1 Rnpaisa Token Generation.......");
//
//            // First get raw response as Map to debug
//            ResponseEntity<Map> rawResponse = restTemplate.postForEntity(
//                    url,
//                    requestEntity,
//                    Map.class
//            );
//
//            log.info("Step 2 Rnpaisa Token Generation Raw Response: {}", rawResponse.getBody());
//
//            responseJson = objectMapper.writeValueAsString(rawResponse.getBody());
//
//            // Convert Map to DTO
//            PayoutTokenResponseDTO tokenResponse = objectMapper.convertValue(rawResponse.getBody(), PayoutTokenResponseDTO.class);
//            log.info("Bearer token response: {}", tokenResponse);
//
//            PayoutTokenResponseDTO.TokenData tokenData = tokenResponse.getTokenData();
//            if (tokenData != null && tokenData.getToken() != null) {
//                return tokenData.getToken();
//            } else {
//                throw new RuntimeException("Failed To Get Token From Payout Vendor - Response: " + rawResponse.getBody());
//            }
//
//        } finally {
//            //  ALWAYS save vendor logs (success + failure)
//
//            log.info("Step 3 Rnpaisa Token Generation.....");
//
//            commonFunctions.saveVendorLogs(
//                    requestTime,
//                    requestJson,
//                    vendorId,
//                    LocalDateTime.now(),
//                    responseJson,
//                    null
//            );
//        }
//    }

    @Override
    public PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials credentials, String callbackUrl) {
        log.info("[RUNPAISA] Registering callback URL for merchant={} | callback={}",
                credentials.getVendor().getVendorName(), callbackUrl);


        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = credentials.getVendor().getVendorCode();

        try {
            // Generate auth token
            String token = generateTokenForPayout(credentials);

            // Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", token);

            // Prepare request body
            Map<String, Object> body = new HashMap<>();
            body.put("callback_url", callbackUrl);

            requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = credentials.getBaseUrlProd() + "/updatemerchantcallback";
            log.info("[RUNPAISA] Hitting callback registration API: {}", url);

            ResponseEntity<PayoutMerchantCallbackResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PayoutMerchantCallbackResponse.class
            );


            responseJson = objectMapper.writeValueAsString(response.getBody());

            log.info("[RUNPAISA] Callback registered successfully. StatusCode={}, Response={}",
                    response.getStatusCode(), response.getBody());

            return response.getBody();

        } catch (HttpStatusCodeException e) {

            responseJson = e.getResponseBodyAsString();
            log.error("[RUNPAISA] HTTP error: {} - {}", e.getStatusCode(), responseJson);

            PayoutMerchantCallbackResponse errorResponse = new PayoutMerchantCallbackResponse();
            errorResponse.setCode(String.valueOf(e.getStatusCode().value()));
            errorResponse.setMessage("Vendor API Error");
            return errorResponse;

        } catch (Exception e) {

            responseJson = e.getMessage();
            log.error("[RUNPAISA] Exception while registering callback URL: {}", e.getMessage(), e);

            PayoutMerchantCallbackResponse errorResponse = new PayoutMerchantCallbackResponse();
            errorResponse.setCode("500");
            errorResponse.setMessage("Exception: " + e.getMessage());
            return errorResponse;

        } finally {
            //  ALWAYS EXECUTES
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
        }
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {

        LocalDateTime requestTime = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        log.info("---- [PAYOUT CALLBACK] Payload received: {}", payload);

        try {
            // Enable case-insensitive conversion just for this operation
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);

            // Convert directly to DTO (case-insensitive)
            RunpaisaPayoutCallback payoutCallback = objectMapper.convertValue(payload, RunpaisaPayoutCallback.class);
            log.info("---- [PAYOUT CALLBACK] Parsed DTO: {}", payoutCallback);

            // Create a unified callback object
            RunPaisaCallback callback = new RunPaisaCallback();
//            callback.setOrderId(payoutCallback.getOrder_id());
            callback.setOrderId(payoutCallback.getOrderid());
            callback.setTxnAmount(payoutCallback.getTxn_amount());
            callback.setStatus(payoutCallback.getStatus());
            callback.setStatusCode(payoutCallback.getCode());
            callback.setTxnInfo(payoutCallback.getMessage());
//            callback.setBankTxnId(payoutCallback.getUtr_no());
            callback.setBankTxnId(payoutCallback.getUtrno());

            log.info("<------------------ Calling Process Callback Method ------------------>");
            processCallback(callback);

            // Save callback log
//            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp,
//                    "Payout callback processed successfully", LocalDateTime.now());
            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp,
                    "Payout callback processed successfully", LocalDateTime.now());

            log.info("---- [PAYOUT CALLBACK] Successfully logged callback event");
            return "Payout callback processed successfully";

        } catch (Exception e) {
            log.error("---- [PAYOUT CALLBACK] Error while processing payout callback: {}", e.getMessage(), e);
//            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp,
//                    e.getMessage(), LocalDateTime.now());

            commonFunctions.saveCallbackEventLog(payload.toString(), requestTime, clientIp,
                    e.getMessage(), LocalDateTime.now());

            throw new RuntimeException("Payout callback processing failed");
        }
    }

    public void processCallback(RunPaisaCallback callback) {
        log.info("========== [RunPaisa CALLBACK RECEIVED] ==========");
        log.info("========== ORDER_ID: {}, STATUS: {}, TXN_AMOUNT: {}",
                callback.getOrderId(), callback.getStatus(), callback.getTxnAmount());

        TransactionSchemeDetailsRequestDto dto = null;

        try {
            // Fetch pending transaction (for all statuses)
            log.info("[1] Fetching pending transaction for ORDER_ID: {}", callback.getOrderId());
            PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(callback.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + callback.getOrderId()));

            log.info("[1.1] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                    pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                    pendingTxn.getAmount(), pendingTxn.getStatus());

            Double txnAmt = Double.valueOf(callback.getTxnAmount());

            // Fetch matching SchemeRate
            log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                    pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            pendingTxn.getSchemeCode(),
                            pendingTxn.getTransactionType(),
                            txnAmt)
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

            log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                    selectedScheme.getGross(), selectedScheme.getRetailer());

            // Fetch scheme entity
            log.info("[3] Fetching Scheme entity for SchemeCode={}", selectedScheme.getSchemeCode());
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

            log.info("[3.1] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

            // Parse scheme combination hash code
            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
            if (hashParts.length < 2) {
                throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.getSchemeCombinationHashCode());
            }

            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];

            log.info("[4] Extracted ProductID={}, SubProductID={} from SchemeCombinationHashCode", productId, subProductIdStr);

            // Fetch product and subproduct
            Products product = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));
            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                    .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + subProductIdStr));

            log.info("[4.1] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                    product.getName(), subProduct.getSubProductTitle());

            // Prepare DTO (common for all statuses)
            log.info("[5] Constructing TransactionSchemeDetailsRequestDto...");
            dto = new TransactionSchemeDetailsRequestDto();

            String orderId = callback.getOrderId();
            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(txnAmt);
            dto.setConfirmAmount(txnAmt);
            dto.setOperatorName(pendingTxn.getTransactionType());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(pendingTxn.getRetailerId());
            dto.setVendorName("Runpaisa PG");
            dto.setVendorTxnId(callback.getBankTxnId());

            log.info("[6] DTO ready for transaction processing: {}", dto);

            // Process transaction for any status (SUCCESS, FAILED, PENDING, etc.)
            transactionEngineServiceImpl.processTransaction(dto, callback.getStatus());
            log.info("[7] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                    callback.getOrderId(), callback.getStatus());

        } catch (Exception ex) {
            log.error("[ERROR] Exception while processing callback for ORDER_ID={} | Error={}",
                    callback.getOrderId(), ex.getMessage(), ex);
        }

        log.info("========== [RunPaisa CALLBACK PROCESSING COMPLETED] ORDER_ID={} ==========", callback.getOrderId());
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = credentials.getVendor().getVendorCode();

        try {
            String token = generateTokenForPayout(credentials);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("client_id", credentials.getClientId());
            headers.set("token", token);

            Map<String, String> requestBody = Map.of("order_id", orderId);

            requestJson = objectMapper.writeValueAsString(requestBody);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            String url = credentials.getBaseUrlProd() + "/status";
            log.info("[RUNPAISA] Checking status for orderId={} via {}", orderId, url);

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            responseJson = objectMapper.writeValueAsString(response.getBody());

            Map<String, Object> body = response.getBody();
            log.info("[RUNPAISA] Raw status response: {}", body);

            return new PayoutStatusResponse(
                    orderId,
                    (String) body.get("status"),
                    (String) body.get("message")
            );

        } catch (Exception e) {

            responseJson = e.getMessage();
            log.error("[RUNPAISA] Error checking status for orderId={}: {}", orderId, e.getMessage(), e);

            return new PayoutStatusResponse(
                    orderId,
                    "FAILED",
                    "Error during status check"
            );

        } finally {
            //  ALWAYS EXECUTES (success / failure / exception)
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
        }
    }
}