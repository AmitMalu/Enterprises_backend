package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.service.VerificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
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


    @Override
    @Transactional
    public ResponseEntity<?> validateAadhaar(String aadhaarNumber) {
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
            headers.set("Authorization","Bearer " +  credentials.getToken()); // use generated token

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("id_number", aadhaarNumber);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL (baseUrlUat + Aadhaar path)
            String url = credentials.getBaseUrlUat() + "/v1/aadhaar-validation/aadhaar-validation";


            // 7. Call the API
            ResponseEntity<AadhaarValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AadhaarValidationResponse.class
            );

            log.info("Aadhaar validation response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error validating Aadhaar: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Aadhaar validation failed: " + e.getMessage());
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
    public ResponseEntity<?> validatepan(String PanNumber) {
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
            headers.set("Authorization","Bearer " +  credentials.getToken()); // use generated token

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("id_number", PanNumber);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL (baseUrlUat + Aadhaar path)
            String url = credentials.getBaseUrlUat() + "/v1/pan/pan";


            // 7. Call the API
            ResponseEntity<PanValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    PanValidationResponse.class
            );

            log.info("Pan validation response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error validating Pan: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Pan validation failed: " + e.getMessage());
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

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("email", email);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL (baseUrlUat + Aadhaar path)
            String url = credentials.getBaseUrlUat() + "/v1/employment/email-check";


            // 7. Call the API
            ResponseEntity<EmailValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    EmailValidationResponse.class
            );

            log.info("Pan validation response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error validating Email: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Email validation failed: " + e.getMessage());
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

            // 4. Prepare headers with token
            HttpHeaders headers = new HttpHeaders();
//            headers.set("Authorization","Bearer" +  credentials.getToken()); // use generated token
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("id_number", voterid);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL (baseUrlUat + Aadhaar path)
            String url = credentials.getBaseUrlUat() + "/v1/voter-id/voter-id";


            // 7. Call the API
            ResponseEntity<VoterIdValidationResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    VoterIdValidationResponse.class
            );

            log.info("VoterId validation response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error validating VoterId: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("VoterId validation failed: " + e.getMessage());
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
            headers.set("Authorization","Bearer " +  credentials.getToken()); // use generated token

            // 5. Build request body
            Map<String, String> body = new HashMap<>();
            body.put("aadhaar_number", aadhaarNumber);

            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(body, headers);

            // 6. Build final endpoint URL (baseUrlUat + Aadhaar path)
            String url = credentials.getBaseUrlUat() + "/v1/pan/aadhaar-pan-link-check";


            // 7. Call the API
            ResponseEntity<AadhaarPanLinkResponse> response = restTemplate.postForEntity(
                    url,
                    requestEntity,
                    AadhaarPanLinkResponse.class
            );

            log.info("Aadhaar validation response: {}", response.getBody());
            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            log.error("Error validating Aadhaar: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Aadhaar validation failed: " + e.getMessage());
        }
    }






}
