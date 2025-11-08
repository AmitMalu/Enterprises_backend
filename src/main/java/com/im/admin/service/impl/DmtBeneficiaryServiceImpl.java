package com.im.admin.service.impl;


import com.im.admin.dto.DmtBeneficiaryDeleteResponse;
import com.im.admin.dto.DmtBeneficiaryFetchByBeneIdResponse;
import com.im.admin.dto.DmtBeneficiaryFetchResponse;
import com.im.admin.entities.DmtBeneficiary;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.BeneficiaryRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.BeneficiaryRegisterRequest;
import com.im.admin.request.DmtBeneficiaryDeleteRequest;
import com.im.admin.request.DmtBeneficiaryFetchByBeneIdRequest;
import com.im.admin.request.DmtBeneficiaryFetchRequest;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DmtBeneficiaryService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class DmtBeneficiaryServiceImpl implements DmtBeneficiaryService {

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private BeneficiaryRepository beneficiaryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public CommonResponse<BeneficiaryRegisterResponse> registerBeneficiary(BeneficiaryRegisterRequest req) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // ====== STEP 2: Prepare Headers and Body ======
        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<BeneficiaryRegisterRequest> entity = new HttpEntity<>(req, headers);

        // ====== STEP 3: Call Vendor API ======
        ResponseEntity<BeneficiaryRegisterResponse> respEntity = restTemplate.exchange(
                credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary",
                HttpMethod.POST,
                entity,
                BeneficiaryRegisterResponse.class
        );

        BeneficiaryRegisterResponse resp = respEntity.getBody();

        // ====== STEP 4: If successful → save in local DB ======
        if (resp != null && resp.getResponse_code() == 1) {
            DmtBeneficiary bene = new DmtBeneficiary();
            bene.setMobile(req.getMobile());
            bene.setBeneId(resp.getData().getBene_id());
            bene.setBeneName(req.getBenename());
            bene.setBankId(req.getBankid());
            bene.setBankName(resp.getData().getBankname());
            bene.setAccountNo(req.getAccno());
            bene.setIfscCode(req.getIfsccode());
            bene.setVerified(req.getVerified());
            bene.setStatus("REGISTERED");
            beneficiaryRepository.save(bene);
        }

        // ====== STEP 5: Wrap in CommonResponse ======
        CommonResponse<BeneficiaryRegisterResponse> common = new CommonResponse<>();
        common.setStatusCode(200);
        common.setMessage(resp != null ? resp.getMessage() : "Registration failed");
        common.setData(resp);

        return common;
    }

    // Helper method to build headers
    private HttpHeaders getHeaders(VendorCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", credentials.getToken());
        headers.set("Authorisedkey", credentials.getSaltKey());
        return headers;
    }


    @Override
    public CommonResponse<DmtBeneficiaryDeleteResponse> deleteBeneficiary(DmtBeneficiaryDeleteRequest req) {

        // Step 1: Load vendor credentials dynamically
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for vendor"));

        // Step 2: Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", credentials.getToken());
        headers.set("Authorisedkey", credentials.getSaltKey());

        // Step 3: Prepare request body
        HttpEntity<DmtBeneficiaryDeleteRequest> entity = new HttpEntity<>(req, headers);

        // Step 4: Call Vendor Delete Beneficiary API
        ResponseEntity<DmtBeneficiaryDeleteResponse> responseEntity = restTemplate.exchange(
                credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/deletebeneficiary",
                HttpMethod.POST,
                entity,
                DmtBeneficiaryDeleteResponse.class
        );

        DmtBeneficiaryDeleteResponse vendorResponse = responseEntity.getBody();

        // Step 5: Update Local Database if deleted successfully
        if (vendorResponse != null && vendorResponse.getResponse_code() == 1) {
            DmtBeneficiary bene = beneficiaryRepository.findByBeneId(req.getBene_id());
            if (bene != null) {
                bene.setStatus("DELETED");
                beneficiaryRepository.save(bene);
            }
        }

        // Step 6: Wrap final response
        CommonResponse<DmtBeneficiaryDeleteResponse> common = new CommonResponse<>();
        common.setStatusCode(responseEntity.getStatusCodeValue());
        common.setMessage(vendorResponse != null ? vendorResponse.getMessage() : "No response from vendor");
        common.setData(vendorResponse);

        return common;
    }

    @Override
    public CommonResponse<DmtBeneficiaryFetchResponse> fetchBeneficiary(DmtBeneficiaryFetchRequest req) {
        try {
            // ====== STEP 1: Load Vendor Credentials Dynamically ======
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ====== STEP 2: Prepare Request ======
            Map<String, Object> reqBody = Map.of("mobile", req.getMobile());

            HttpHeaders headers = getHeaders(credentials);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // ====== STEP 3: Call Vendor API ======
            ResponseEntity<DmtBeneficiaryFetchResponse> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiary",
                    HttpMethod.POST,
                    entity,
                    DmtBeneficiaryFetchResponse.class
            );

            DmtBeneficiaryFetchResponse resp = response.getBody();
            if (resp == null) {
                return CommonResponse.handleErrors("No response from vendor");
            }

            // ====== STEP 4: Handle Response ======
            if (resp.getResponse_code() == 1) {
                // Optional: Save or update beneficiaries locally if you want
                return CommonResponse.success(resp, "Beneficiaries fetched successfully");
            } else {
                return CommonResponse.handleErrors(resp.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CommonResponse.handleErrors("Error fetching beneficiary list: " + e.getMessage());
        }
    }

    @Override
    public CommonResponse<DmtBeneficiaryFetchByBeneIdResponse> fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest req) {
        try {
            // ====== STEP 1: Load Vendor Credentials Dynamically ======
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ====== STEP 2: Prepare Request Body ======
            Map<String, Object> reqBody = Map.of(
                    "mobile", req.getMobile(),
                    "beneid", req.getBeneid()
            );

            HttpHeaders headers = getHeaders(credentials);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(reqBody, headers);

            // ====== STEP 3: Call Vendor API ======
            ResponseEntity<DmtBeneficiaryFetchByBeneIdResponse> response = restTemplate.exchange(
                    credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiarybybeneid",
                    HttpMethod.POST,
                    entity,
                    DmtBeneficiaryFetchByBeneIdResponse.class
            );

            DmtBeneficiaryFetchByBeneIdResponse resp = response.getBody();
            if (resp == null) {
                return CommonResponse.handleErrors("No response from vendor");
            }

            // ====== STEP 4: Handle Response ======
            if (resp.getResponse_code() == 1) {
                return CommonResponse.success(resp, "Beneficiary fetched successfully");
            } else {
                return CommonResponse.handleErrors(resp.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            return CommonResponse.handleErrors("Error fetching beneficiary details: " + e.getMessage());
        }
    }



}
