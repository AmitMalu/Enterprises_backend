package com.im.admin.service.impl;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.*;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VerificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class VerificationServiceImpl implements VerificationService {

//    private final RestTemplate restTemplate;
//
//    public VerificationServiceImpl(RestTemplate restTemplate) {
//        this.restTemplate = restTemplate;
//    }

    RestTemplate restTemplate = new RestTemplate();


//    @Value("${surepass.sandbox.token}")
//    private String sandboxToken; // You can set this in application.properties
//
//    @Value("${surepass.sandbox.aadhaar-validation-url}")
//    private String aadhaarValidationUrl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;


    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional
    public ResponseEntity<?> validateAadhaar(String aadhaarNumber) {
        try {
            // 1. Get required objects
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Aadhar Verification"));

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);

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

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/aadhaar-validation/aadhaar-validation";

            // 4. Call API
            ResponseEntity<AadhaarValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AadhaarValidationResponse.class
            );

            AadhaarValidationResponse responseBody = response.getBody();
            String remarks = responseBody.getData() != null ? responseBody.getData().getRemarks() : null;
            String message = remarks != null ? remarks : "Aadhaar validation successful";

            return ResponseEntity.ok(CommonResponse.success(responseBody, message));

        } catch (HttpClientErrorException e) {
            try {
                AadhaarValidationResponse errorBody = objectMapper.readValue(
                        e.getResponseBodyAsString(),
                        AadhaarValidationResponse.class
                );

                String remarks = errorBody.getData() != null ? errorBody.getData().getRemarks() : null;
                String message = remarks != null ? remarks : "Aadhaar validation failed";

                return ResponseEntity.ok(CommonResponse.success(errorBody, message));

            } catch (Exception parseEx) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "Aadhaar validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error: ", e);
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
    public ResponseEntity<?> validatepan(String panNumber) {
        try {
            // Step 1: Get all required dependencies
            SubProducts subProduct = subProductsRepository
                    .findBySubProductTitle("Aadhar Verification")
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for Pan Verification"));

            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProduct.getId());
            Vendors vendor = getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // Step 2: Prepare request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            Map<String, String> body = new HashMap<>();
            body.put("id_number", panNumber);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/pan/pan";

            // Step 3: Call API
            ResponseEntity<PanValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    PanValidationResponse.class
            );

            PanValidationResponse responseBody = response.getBody();
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
                String message = errorBody.getMessage() != null ? errorBody.getMessage() : "PAN validation failed";
                return ResponseEntity.ok(CommonResponse.success(errorBody, message));
            } catch (Exception ex) {
                return ResponseEntity.status(HttpStatus.OK)
                        .body(CommonResponse.error(200, "PAN validation failed and response could not be parsed."));
            }

        } catch (Exception e) {
            log.error("Unexpected error during PAN validation: ", e);
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
    public ResponseEntity<?> validateVoterid(String voterid) {
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
            body.put("id_number", voterid);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);
            String url = credentials.getBaseUrlUat() + "/v1/voter-id/voter-id";

            // 6. Call API
            ResponseEntity<VoterIdValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    VoterIdValidationResponse.class
            );

            VoterIdValidationResponse responseBody = response.getBody();
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
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + credentials.getToken());

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("aadhaar_number", aadhaarNumber);

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
            return ResponseEntity.status(HttpStatus.OK)
                    .body(CommonResponse.error(200, "PAN link check failed: " + e.getMessage()));
        }
    }







}
