package com.im.admin.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.response.BankVerificationResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VerificationService;
import com.im.admin.service.WalletToWalletTransferService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
import com.im.admin.util.WalletTransactionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Override
    @Transactional
    public ResponseEntity<?> validateAadhaar(String aadhaarNumber) {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // 1. Get required objects
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Aadhar Verification"));

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);
            vendorId = vendor.getId(); //  store vendor ID for logging

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // 2. Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            // 3. Prepare request
            Map<String, String> body = new HashMap<>();
            body.put("id_number", aadhaarNumber);
            requestJson = objectMapper.writeValueAsString(body); //  store request JSON

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/aadhaar-validation/aadhaar-validation";

            // 4. Call API
            ResponseEntity<AadhaarValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AadhaarValidationResponse.class
            );

            AadhaarValidationResponse responseBody = response.getBody();
            responseJson = objectMapper.writeValueAsString(responseBody); //  store response JSON

            // 5. Save logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

            String remarks = responseBody.getData() != null ? responseBody.getData().getRemarks() : null;
            String message = remarks != null ? remarks : "Aadhaar validation successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                AadhaarValidationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        AadhaarValidationResponse.class
                );
                responseJson = objectMapper.writeValueAsString(errorBody); // 👈 error response JSON

                // Save logs
                commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

                String remarks = errorBody.getData() != null ? errorBody.getData().getRemarks() : null;
                String message = remarks != null ? remarks : "Aadhaar validation failed";

                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception parseEx) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "Aadhaar validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error: ", e);

            // Optional: log generic exception too if needed
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), e.getMessage(), null);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "Aadhaar validation failed: " + e.getMessage()));
        }
    }


    private static Vendors getVendors(VendorRouting vendorRouting) {
        Vendors vendor = null;

        if (vendorRouting.getRoutingPriority1() != null && vendorRouting.getRoutingPriority1().getActive()) {
            vendor = vendorRouting.getRoutingPriority1();
        } else if (vendorRouting.getRoutingPriority2() != null && vendorRouting.getRoutingPriority2().getActive()) {
            vendor = vendorRouting.getRoutingPriority2();
        } else if (vendorRouting.getRoutingPriority3() != null && vendorRouting.getRoutingPriority3().getActive()) {
            vendor = vendorRouting.getRoutingPriority3();
        }

        if (vendor == null) {
            throw new RuntimeException("No active vendor found for this subProductId");
        }
        return vendor;
    }


    public String generateTokenAsPerVendor(VendorCredentials vendorCredentials) {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", vendorCredentials.getUsername());
        headers.set("password", vendorCredentials.getPassword());
        headers.set("client_id", vendorCredentials.getClientId());

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = vendorCredentials.getTokenUrlUat();
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
    public ResponseEntity<?> validatePan(String panNumber) {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // Step 1: Get all required dependencies
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Pan Verification"));

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);
            vendorId = vendor.getId(); // For logs

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // Step 2: Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            Map<String, String> body = new HashMap<>();
            body.put("id_number", panNumber);
            requestJson = objectMapper.writeValueAsString(body); // For logs

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/pan/pan";

            // Step 3: Call API
            ResponseEntity<PanValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    PanValidationResponse.class
            );

            PanValidationResponse responseBody = response.getBody();
            responseJson = objectMapper.writeValueAsString(responseBody); // For logs

            // Save logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

            String message = responseBody.getMessage() != null && !responseBody.getMessage().isEmpty()
                    ? responseBody.getMessage()
                    : "PAN validation successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                PanValidationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        PanValidationResponse.class
                );
                responseJson = objectMapper.writeValueAsString(errorBody); // For logs

                // Save logs
                commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

                String message = errorBody.getMessage() != null ? errorBody.getMessage() : "PAN validation failed";
                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "PAN validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error during PAN validation: ", e);

            // Save generic exception logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), e.getMessage(), null);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "PAN validation failed: " + e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> validateEmail(String email) {
        try {
            // 1. Find SubProduct by Title
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Aadhar Verification"));

            // 2. Find VendorRouting by subProductId
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);

            // 3. Get credentials for this vendor
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // 4. Prepare headers with token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + credentials.getToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 5. Prepare request body
            Map<String, String> body = new HashMap<>();
            body.put("email", email);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/employment/email-check";

            // 6. Call API
            ResponseEntity<EmailValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    EmailValidationResponse.class
            );

            EmailValidationResponse responseBody = response.getBody();
            String message = responseBody.getData() != null && responseBody.getData().getStatus() != null
                    ? responseBody.getData().getStatus()
                    : "Email validation successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                EmailValidationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        EmailValidationResponse.class
                );

                String message = errorBody.getData() != null && errorBody.getData().getStatus() != null
                        ? errorBody.getData().getStatus()
                        : "Email validation failed";

                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception parseEx) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "Email validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "Email validation failed: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> validateVoterId(String voterId) {
        try {
            // 1. Find SubProduct by Title
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Aadhar Verification"));

            // 2. Find VendorRouting by subProductId
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);

            // 3. Get credentials for this vendor
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // 4. Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            // 5. Prepare request
            Map<String, String> body = new HashMap<>();
            body.put("id_number", voterId);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/voter-id/voter-id";

            // 6. Call API
            ResponseEntity<VoterIdValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    VoterIdValidationResponse.class
            );

            VoterIdValidationResponse responseBody = response.getBody();
            assert responseBody != null;
            String message = responseBody.getMessage() != null ? responseBody.getMessage() : "Voter ID validation successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                VoterIdValidationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        VoterIdValidationResponse.class
                );

                String message = errorBody.getMessage() != null ? errorBody.getMessage() : "Voter ID validation failed";
                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception parseEx) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(CommonResponse.error(400, "Voter ID validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Voter ID validation failed: " + e.getMessage()));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> AdharPanLink(String aadhaarNumber) {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // 1. Find SubProduct by Title
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Aadhar Verification"));

            // 2. Find VendorRouting by subProductId
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);
            vendorId = vendor.getId(); // log vendor ID

            // 3. Get credentials for this vendor
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // 4. Prepare headers with token
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("aadhaar_number", aadhaarNumber);
            requestJson = objectMapper.writeValueAsString(body); // log request

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL
            String url = credentials.getBaseUrlUat() + "/v1/pan/aadhaar-pan-link-check";

            // 7. Call the API
            ResponseEntity<AadhaarPanLinkResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AadhaarPanLinkResponse.class
            );

            AadhaarPanLinkResponse responseBody = response.getBody();
            responseJson = objectMapper.writeValueAsString(responseBody); // log response

            // 8. Save vendor logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

            assert responseBody != null;
            String message = responseBody.getData() != null && responseBody.getData().getReason() != null
                    ? responseBody.getData().getReason()
                    : "Aadhaar PAN link check successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                AadhaarPanLinkResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        AadhaarPanLinkResponse.class
                );
                responseJson = objectMapper.writeValueAsString(errorBody); // log error response

                // Save logs in case of error
                commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

                String message = errorBody.getData() != null && errorBody.getData().getReason() != null
                        ? errorBody.getData().getReason()
                        : "Aadhaar PAN link check failed";

                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception parseEx) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "PAN link check failed and response could not be parsed."));
            }
        } catch (Exception e) {
            log.error("Unexpected error: ", e);

            // Save generic error logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), e.getMessage(), null);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "PAN link check failed: " + e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> validateBank(BankVerificationRequest request) {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {

            String hierarchyId = tokenUtil.extractHierarchyId();

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated base transaction ID: {}", baseTransactionId);

            // 1. SubProduct lookup
            SubProducts subProduct = subProductsRepository
                    .findBySubProductCode(request.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Bank Verification"));

            // Get Product from SubProduct
            Products product = subProduct.getProduct();

            log.info("---- [Callback] Product fetched successfully | productId={}", product.getId());

            // 2. Vendor Routing
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);
            vendorId = vendor.getId();

            // 3. Vendor Credentials
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            String schemeCode;

            String hierarchyType = walletTransactionService.extractHierarchyType(hierarchyId);

            if ("RU".equalsIgnoreCase(hierarchyType) || "RET".equalsIgnoreCase(hierarchyType)) {

                // Retailer / RU → use request scheme
                schemeCode = request.getSchemeCode();

                if (schemeCode == null || schemeCode.isBlank()) {
                    throw new RuntimeException("Scheme code is required for Retailer/RU");
                }

            } else {

                // Other hierarchy → derive scheme
                String schemeName = getSchemeNameByHierarchy(hierarchyId);

                Optional<Scheme> scheme = Optional.ofNullable(schemeRepository.findBySchemeName(schemeName).orElseThrow(() -> new RuntimeException("No Scheme Found")));

                schemeCode = scheme.get().getSchemeCode();

                log.info("Scheme resolved from hierarchy | type={} | schemeCode={}", hierarchyType, schemeCode);
            }

            // Fetch Scheme Rate (COMMON for all)
            List<SchemeRate> schemeRates = schemeRateRepository.findBySchemeCode(schemeCode);

            if (schemeRates == null || schemeRates.isEmpty()) {
                throw new RuntimeException("Scheme rate not found. Contact admin.");
            }

            if (schemeRates.size() > 1) {
                log.error("Multiple scheme slabs found for schemeCode: {}", schemeCode);
                throw new RuntimeException("Multiple scheme slabs found. Contact admin.");
            }

            SchemeRate schemeRate = schemeRates.get(0);

            log.info("Scheme rate fetched successfully | schemeCode={} | rateType={} | slabType={}",
                    schemeRate.getSchemeCode(),
                    schemeRate.getRateType(),
                    schemeRate.getSlabType());

            BigDecimal grossAmount = BigDecimal.valueOf(schemeRate.getGross());

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(subProduct.getWalletType(), hierarchyId);
            BigDecimal availableBalance = walletAmountTable.getAvailableBalance();

            if (availableBalance.compareTo(grossAmount) < 0) {
                log.warn("Insufficient balance. Required: {}, Available: {}",
                        grossAmount, availableBalance);

                throw new IllegalArgumentException(
                        "Insufficient balance in " + WalletType.PREPAID.name() +
                                " wallet. Required: " + grossAmount +
                                ", Available: " + availableBalance
                );
            }

            // 4. Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            requestJson = objectMapper.writeValueAsString(request);
            HttpEntity<BankVerificationRequest> requestEntity = new HttpEntity<>(request, headers);

            String url = credentials.getBaseUrlUat() + "/v1/bank-verification/";

            // 5. Call API
            ResponseEntity<BankVerificationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    BankVerificationResponse.class
            );

            BankVerificationResponse responseBody = response.getBody();
            responseJson = objectMapper.writeValueAsString(responseBody);

            // 6. Save logs
            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

            // Check vendor response before processing charges
            if (responseBody == null
                    || responseBody.getData() == null
                    || responseBody.getStatus_code() != 200) {

                log.warn("Vendor verification failed. Skipping charge processing.");

                String message = responseBody != null && responseBody.getData() != null
                        ? responseBody.getData().getStatus()
                        : "Bank verification failed";

                return ResponseEntity.ok(CommonResponse.success(responseBody, message));
            }

            try {

                if(Boolean.TRUE.equals(product.getHasCharges())) {
                    processHierarchyCharges(
                            hierarchyId,
                            product,
                            subProduct,
                            schemeRate,
                            grossAmount,
                            baseTransactionId
                    );
                }

                if(Boolean.TRUE.equals(product.getIsCommissions())) {

                    WalletType walletType = "POSTPAID".equalsIgnoreCase(subProduct.getWalletType())
                            ? WalletType.POSTPAID
                            : WalletType.PREPAID;

                    TransactionSchemeDetailsRequestDto dto = walletTransactionService.buildTransactionDto(grossAmount, baseTransactionId,
                            schemeRate, product, subProduct, hierarchyId, walletType);
                    transactionEngineServiceImpl.processCommission(dto, baseTransactionId, "SUCCESS", dto);

                }

            } catch (Exception e) {
                log.error("**** [processChargesAndCommission] ERROR during processing {} {} ****", product.getHasCharges(), product.getIsCommissions(), e);
                throw e;
            }

            String message = responseBody.getData() != null && responseBody.getData().getStatus() != null
                    ? responseBody.getData().getStatus()
                    : "Bank verification successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                BankVerificationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        BankVerificationResponse.class
                );
                responseJson = objectMapper.writeValueAsString(errorBody);

                commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), responseJson, null);

                String message = errorBody.getData() != null ? errorBody.getData().getStatus() : "Bank verification failed";
                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "Bank verification failed and response could not be parsed."));
            }
        } catch (Exception e) {
            log.error("Unexpected error: ", e);

            commonFunctions.saveVendorLogs(requestDateTime, requestJson, vendorId, LocalDateTime.now(), e.getMessage(), null);

            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "Bank verification failed: " + e.getMessage()));
        }
    }


    public void processHierarchyCharges(
            String hierarchyId,
            Products product,
            SubProducts subProduct,
            SchemeRate schemeRate,
            BigDecimal grossCharge,
            String baseTransactionId) {

        String originalId = hierarchyId;
        String currentId = hierarchyId;

        WalletType walletType = "POSTPAID".equalsIgnoreCase(subProduct.getWalletType())
                ? WalletType.POSTPAID
                : WalletType.PREPAID;

        while (true) {

            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                throw new RuntimeException("No approver found for hierarchy: " + currentId);
            }

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if (isChargeableParent(parentType)) {

                walletTransactionService.processChargesForHierarchy(
                        originalId,
                        parentId,
                        walletType,
                        product,
                        subProduct,
                        schemeRate,
                        grossCharge,
                        baseTransactionId
                );

                break;
            }

            currentId = parentId;
        }
    }

    private boolean isChargeableParent(String type) {
        return "WL".equals(type) || "IM".equals(type) || "AD".equals(type);
    }

    public String getSchemeNameByHierarchy(String hierarchyId) {

        String type = walletTransactionService.extractHierarchyType(hierarchyId);

        return switch (type.toUpperCase()) {
            case "ET" -> "Bank_Verification_ET_Scheme";
            case "WL" -> "Bank_Verification_WL_Scheme";
            case "SD" -> "Bank_Verification_SD_Scheme";
            case "PT" -> "Bank_Verification_PT_Scheme";
            case "DT" -> "Bank_Verification_DT_Scheme";
            default -> throw new RuntimeException("No scheme defined for hierarchy: " + type);
        };
    }
}
