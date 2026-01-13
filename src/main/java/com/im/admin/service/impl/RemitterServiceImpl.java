package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.SMSGatewayAPI;
import com.im.admin.dto.*;
import com.im.admin.request.RemitterUpdateRequest;
import com.im.admin.entities.DmtRemitter;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;

import com.im.admin.repositories.RemitterRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.DmtOtpRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DmtRemitterResponseDto;
import com.im.admin.service.RemitterService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.im.admin.entities.vendor.Vendors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private SMSGatewayAPI smsGatewayAPI;

    @Autowired private ObjectMapper objectMapper;


    //                CHECK REMITTER API
    @Override
    public CommonResponse<RemitterQueryResponse> checkRemitter(String mobile,String subProductId) throws Exception  {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
            // ====== STEP 1: Load Vendor Credentials Dynamically ======
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(subProductId); // subProductId dynamically aayega
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            vendorId = vendor.getId();

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ====== STEP 2: Check if remitter already exists in LOCAL DB ======
            Optional<DmtRemitter> exist = remitterRepository.findByMobile(mobile);
            if (exist.isPresent() && "REGISTERED".equals(exist.get().getStatus())) {
                DmtRemitter remitter = exist.get();

                // Create vendor-style response dynamically
                RemitterQueryResponse res = new RemitterQueryResponse();
                res.setStatus(true);
                res.setResponse_code(1);
                res.setMessage("Remitter account details fetched.!");

                RemitterQueryResponse.DataObj data = new RemitterQueryResponse.DataObj();
                data.setMobile(remitter.getMobile());
                data.setLimit(remitter.getLimitAmount() != null ? remitter.getLimitAmount() : 0.00);
                res.setData(data);
                return CommonResponse.success(res, "Remitter already registered and active");

            }

            // ====== STEP 3: Prepare request body for vendor query API ======
            Map<String, Object> reqBody = Map.of("mobile", mobile);

            requestJson = objectMapper.writeValueAsString(reqBody);

            HttpHeaders headers = getHeaders(credentials);
            HttpEntity<Map<String, Object>> httpEntity = new HttpEntity<>(reqBody, headers);

            // ====== STEP 4: Call Vendor Remitter Query API ======
            ResponseEntity<RemitterQueryResponse> responseEntity =
                    restTemplate.exchange(credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/queryremitter",
                            HttpMethod.POST, httpEntity, RemitterQueryResponse.class);

            RemitterQueryResponse res = responseEntity.getBody();

            responseJson = objectMapper.writeValueAsString(res);

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );


            int code = res.getResponse_code();

            // ====== STEP 5: Handle Vendor Response Cases ======
            switch (code) {

                case 1: // Remitter already exists on vendor side
                    saveRemitter(res); // Save in our DB as REGISTERED
                    return CommonResponse.success(res, "Remitter exists. You can proceed");

                case 2: // KYC required
                    return CommonResponse.success(res, "EKYC_REQUIRED");

                case 3: // OTP sent → Start registration
                    return CommonResponse.success(res, "OTP sent. Please register remitter.");

                default: // Error cases from vendor
                    return CommonResponse.handleErrors(res.getMessage());
            }

        } catch (Exception ex) {

                //  FAILURE VENDOR LOG
                commonFunctions.saveVendorLogs(
                        requestTime,
                        requestJson,
                        vendorId,
                        LocalDateTime.now(),
                        null,
                        ex.getMessage()
                );

                log.error("[CHECK REMITTER] Exception | mobile={}", mobile, ex);
                throw ex;
            }
            }



    //                E-KYC API
    @Override
    public CommonResponse<Object> doEkyc(RemitterEkycRequest request) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
        // STEP 1: Vendor Routing
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(request.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        vendorId = vendor.getId();

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found"));

        // ===== STEP 2: Prepare request JSON for logs =====
        requestJson = objectMapper.writeValueAsString(request);

        // STEP 2: Vendor API call
        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<RemitterEkycRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> resp = restTemplate.exchange(
                credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/queryremitter/kyc",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map body = resp.getBody();

        responseJson = objectMapper.writeValueAsString(body);

        //  SUCCESS VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                responseJson,
                null
        );

        int responseCode = (int) body.get("response_code");

        // STEP 3: If E-KYC SUCCESS → Save in DB as PENDING
        if (responseCode == 1) {

            Map data = (Map) body.get("data");

            String mobile = (String) data.get("mobile");

            //  UNIQUE CHECK
            Optional<DmtRemitter> existing =
                    remitterRepository.findByMobile(mobile);

            if (existing.isPresent()) {
                //  Already exists → do NOT insert
                return CommonResponse.success(
                        body,
                        "E-KYC already done for this mobile"
                );
            }

            DmtRemitter rem = new DmtRemitter();
            rem.setMobile((String) data.get("mobile"));
            rem.setEkycId((String) data.get("ekyc_id"));
            rem.setStateResp((String) data.get("stateresp"));
            rem.setStatus("PENDING"); // IMPORTANT
            rem.setCreatedAt(LocalDateTime.now());
            rem.setName(request.getName());
            rem.setVendor(vendor);
            rem.setAadhaar(request.getAadhaar_number());
            remitterRepository.save(rem);
        }

        // STEP 4: Wrap Response
        return CommonResponse.success(body, "E-KYC API Response");

        } catch (Exception ex) {

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    null,
                    ex.getMessage()
            );

            log.error("[DO EKYC] Exception | mobile={}",
                    request.getMobile(), ex);

            throw new RuntimeException(ex);
        }
    }


    //                REGISTER REMITTER API
    @Override
    public CommonResponse<RemitterRegisterResponse> registerRemitter(RemitterRegisterRequest req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;

        try {
        // STEP 1: Vendor Routing
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        vendorId = vendor.getId();

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found"));

        // ===== STEP 2: Prepare request JSON for logs =====
        requestJson = objectMapper.writeValueAsString(req);

        // STEP 2: Call Vendor API
        HttpHeaders headers = getHeaders(credentials);
        HttpEntity<RemitterRegisterRequest> entity = new HttpEntity<>(req, headers);

        ResponseEntity<RemitterRegisterResponse> respEntity =
                restTemplate.exchange(
                        credentials.getBaseUrlUat() + "/service-api/api/v1/service/dmt/kyc/remitter/registerremitter",
                        HttpMethod.POST,
                        entity,
                        RemitterRegisterResponse.class
                );

        RemitterRegisterResponse resp = respEntity.getBody();

        responseJson = objectMapper.writeValueAsString(resp);

        //  SUCCESS VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                responseJson,
                null
        );

        // STEP 3: On success update DB
        if (resp != null && resp.getResponse_code() == 1) {

            DmtRemitter rem = remitterRepository.findByMobile(req.getMobile())
                    .orElse(new DmtRemitter());

            rem.setMobile(req.getMobile());
            rem.setVendor(vendor);
            rem.setStatus("REGISTERED");
            rem.setEkycId(req.getEkyc_id());
            rem.setStateResp(req.getStateresp());
            rem.setLimitAmount(resp.getData().getLimit());

            remitterRepository.save(rem);
        }

        // STEP 4: Wrap response
        return CommonResponse.success(resp, resp.getMessage());

    } catch (Exception ex) {

        //  FAILURE VENDOR LOG
        commonFunctions.saveVendorLogs(
                requestTime,
                requestJson,
                vendorId,
                LocalDateTime.now(),
                null,
                ex.getMessage()
        );

        log.error("[REGISTER REMITTER] Exception | mobile={}",
                req.getMobile(), ex);

        throw new RuntimeException(ex);
    }
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
//    private void saveRemitter(RemitterQueryResponse res) {
//        DmtRemitter en = new DmtRemitter();
//        en.setMobile(res.getData().getMobile());
//        en.setStatus("REGISTERED");
//        remitterRepository.save(en);
//    }

    private void saveRemitter(RemitterQueryResponse res) {

        String mobile = res.getData().getMobile();

        Optional<DmtRemitter> existing =
                remitterRepository.findByMobile(mobile);

        if (existing.isPresent()) {
            // Already exists → just ensure status
            DmtRemitter rem = existing.get();
            rem.setStatus("REGISTERED");
            remitterRepository.save(rem);
            return;
        }

        // First time insert
        DmtRemitter en = new DmtRemitter();
        en.setMobile(mobile);
        en.setStatus("REGISTERED");
        en.setCreatedAt(LocalDateTime.now());

        remitterRepository.save(en);
    }


    @Override
    public CommonResponse<DmtRemitterResponseDto> fetchRemitterDetails(String mobileNumber) {

        Optional<DmtRemitter> remitter = remitterRepository.findByMobile(mobileNumber);

        return remitter.map(r -> {
            DmtRemitterResponseDto dto = convertToDto(r);
            return new CommonResponse<>(
                    200,
                    "Remitter details fetched successfully",
                    dto
            );
        }).orElseGet(() ->
                new CommonResponse<>(
                        404,
                        "No remitter found for mobile number: " + mobileNumber,
                        null
                )
        );
    }


    @Override
    public CommonResponse<DmtRemitterResponseDto> updateRemitter(RemitterUpdateRequest req) {

        Optional<DmtRemitter> optional = remitterRepository.findByMobile(req.getMobile());

        if (optional.isEmpty()) {
            return CommonResponse.handleErrors("Remitter not found");
        }

        DmtRemitter remitter = optional.get();

        // ===== OTP Validation =====
        if (remitter.getOtp() == null ||
                !remitter.getOtp().equals(req.getOtp())) {
            return CommonResponse.handleErrors("Invalid OTP");
        }

        // OTP Expiry (5 min)
        if (remitter.getOtpGeneratedAt().plusMinutes(5).isBefore(LocalDateTime.now())) {
            return CommonResponse.handleErrors("OTP expired");
        }

        // OTP verified
//        remitter.setOtpVerified(true);
        remitter.setOtp(null); // clear OTP

        // ===== Update fields =====
        if (req.getName() != null) {
            remitter.setName(req.getName());
        }

        remitterRepository.save(remitter);

        DmtRemitterResponseDto dto = convertToUpdateDto(remitter);

        return CommonResponse.success(dto, "Remitter updated successfully");
    }




    private DmtRemitterResponseDto convertToDto(DmtRemitter remitter) {
        DmtRemitterResponseDto dto = new DmtRemitterResponseDto();

        dto.setId(remitter.getId());
        dto.setMobile(remitter.getMobile());
        dto.setAadhaar(remitter.getAadhaar());
        dto.setEkycId(remitter.getEkycId());
        dto.setStateResp(remitter.getStateResp());
        dto.setLimitAmount(remitter.getLimitAmount());
        dto.setName(remitter.getName());
        dto.setStatus(remitter.getStatus());
        dto.setCreatedAt(remitter.getCreatedAt().toString());

        return dto;
    }

    private DmtRemitterResponseDto convertToUpdateDto(DmtRemitter r) {

        DmtRemitterResponseDto dto = new DmtRemitterResponseDto();

        dto.setId(r.getId());
        dto.setMobile(r.getMobile());
        dto.setName(r.getName());
        dto.setLimitAmount(r.getLimitAmount());
        dto.setAadhaar(r.getAadhaar());
        dto.setEkycId(r.getEkycId());
        dto.setStateResp(r.getStateResp());
        dto.setStatus(r.getStatus());
        dto.setCreatedAt(r.getCreatedAt().toString());

        return dto;
    }

    @Override
    public CommonResponse<String> sendOtp(DmtOtpRequest dmtOtpRequest) {

        Optional<DmtRemitter> optional = remitterRepository.findByMobile(dmtOtpRequest.getMobile());

        if (optional.isEmpty()) {
            return CommonResponse.handleErrors("Remitter not found");
        }

        DmtRemitter remitter = optional.get();

        String otp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // Save OTP
        remitter.setOtp(otp);
        remitter.setOtpGeneratedAt(LocalDateTime.now());
//        remitter.setOtpVerified(false);

        remitterRepository.save(remitter);

        // Send SMS
        smsGatewayAPI.sendOTPSms(dmtOtpRequest.getMobile(), otp);

        return CommonResponse.success(null, "OTP sent successfully");

    }

    @Override
    public CommonResponse<String> resendOtp(String mobile) {

        Optional<DmtRemitter> optional = remitterRepository.findByMobile(mobile);

        if (optional.isEmpty()) {
            return CommonResponse.handleErrors("Remitter not found");
        }

        DmtRemitter remitter = optional.get();

        // (Optional) Block resend if already verified
//        if (Boolean.TRUE.equals(remitter.getOtpVerified())) {
//            return CommonResponse.handleErrors("OTP already verified");
//        }

        // Generate new OTP
        String newOtp = String.valueOf((int) (Math.random() * 900000) + 100000);

        // Invalidate old OTP
        remitter.setOtp(newOtp);
        remitter.setOtpGeneratedAt(LocalDateTime.now());

        remitterRepository.save(remitter);

        // Send SMS (LIVE API)
        smsGatewayAPI.sendOTPSms(mobile, newOtp);

        return CommonResponse.success(null, "OTP resent successfully");
    }



}
