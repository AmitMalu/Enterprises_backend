package com.im.admin.service.impl;

import com.im.admin.dto.RemitterEkycRequest;
import com.im.admin.dto.RemitterQueryResponse;
import com.im.admin.dto.RemitterRegisterRequest;
import com.im.admin.dto.RemitterRegisterResponse;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;

import com.im.admin.repositories.RemitterRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.RemitterService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.im.admin.entities.vendor.Vendors;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class RemitterServiceImpl implements RemitterService {

    @Autowired
    private RemitterRepository remitterRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonMethods commonMethods;


    //                CHECK REMITTER API
    @Override
    public CommonResponse<RemitterQueryResponse> checkRemitter(String mobile,String subProductId) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProductId); // subProductId dynamically aayega
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // ====== STEP 2: Check if remitter already exists in LOCAL DB ======
        Optional<DmtRemitter> exist = remitterRepository.findByMobile(mobile);
        if (exist.isPresent() && "REGISTERED".equals(exist.get().getStatus())) {
            return CommonResponse.handleErrors("Remitter already registered and active");
        }

        // ====== STEP 3: Prepare request body for vendor query API ======
        Map<String, Object> reqBody = Map.of("mobile", mobile);

        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(reqBody, headers);

        // ====== STEP 4: Call Vendor Remitter Query API ======
        ResponseEntity<RemitterQueryResponse> responseEntity =
                restTemplate.exchange(credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/queryremitter",
                        HttpMethod.POST, httpEntity, RemitterQueryResponse.class);

        RemitterQueryResponse res = responseEntity.getBody();
        int code = res.getResponse_code();

        // ====== STEP 5: Handle Vendor Response Cases ======
        switch (code) {

            case 1: // Remitter already exists on vendor side
                saveRemitter(res); // Save in our DB as REGISTERED
                return CommonResponse.success(res, "Remitter exists. You can proceed");

            case 2: // KYC required
                return CommonResponse.handleErrors("EKYC_REQUIRED");

            case 3: // OTP sent → Start registration
                return CommonResponse.success(res, "OTP sent. Please register remitter.");

            default: // Error cases from vendor
                return CommonResponse.handleErrors(res.getMessage());
        }
    }


    //                E-KYC API
    @Override
    public CommonResponse<Object> doEkyc(RemitterEkycRequest request) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // ====== STEP 2: Prepare Headers ======
        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<RemitterEkycRequest> entity = new HttpEntity<>(request, headers);

        // ====== STEP 3: Call Vendor E-KYC API ======
        ResponseEntity<Map> resp = restTemplate.exchange(
                credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/queryremitter/kyc",
                HttpMethod.POST,
                entity,
                Map.class);

        // ====== STEP 4: Wrap Vendor Response in CommonResponse ======
        CommonResponse<Object> response = new CommonResponse<>();
        response.setStatusCode(resp.getStatusCodeValue());
        response.setMessage("E-KYC API Response");
        response.setData(resp.getBody());

        return response;
    }


    //                REGISTER REMITTER API

    @Override
    public CommonResponse<RemitterRegisterResponse> registerRemitter(RemitterRegisterRequest req) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        // ====== STEP 2: Prepare Headers and Request ======
        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<RemitterRegisterRequest> entity = new HttpEntity<>(req, headers);

        // ====== STEP 3: Call Vendor API ======
        ResponseEntity<RemitterRegisterResponse> respEntity =
                restTemplate.exchange(credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/registerremitter",
                        HttpMethod.POST, entity, RemitterRegisterResponse.class);

        RemitterRegisterResponse resp = respEntity.getBody();

        // ====== STEP 4: Update LOCAL DB after successful registration ======
        if (resp != null && resp.getResponse_code() == 1) {
            DmtRemitter r = new DmtRemitter();
            r.setMobile(req.getMobile());
            r.setStatus("REGISTERED");
            r.setEkycId(null);
            r.setStateResp(null);
            remitterRepository.save(r);
        }

        // ====== STEP 5: Wrap final response ======
        CommonResponse<RemitterRegisterResponse> common = new CommonResponse<>();
        common.setStatusCode(200);
        common.setMessage(resp != null ? resp.getMessage() : "Registration response not received");
        common.setData(resp);

        return common;
    }


    //                HELPER METHODS
    // Common method to set headers dynamically from VendorCredentials
    private HttpHeaders getHeaders(VendorCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", credentials.getToken());
        headers.set("Authorisedkey", credentials.getSaltKey());
        return headers;
    }

    // Save if remitter already exists
    private void saveRemitter(RemitterQueryResponse res) {
        DmtRemitter en = new DmtRemitter();
        en.setMobile(res.getData().getMobile());
        en.setStatus("REGISTERED");
        remitterRepository.save(en);
    }

}
