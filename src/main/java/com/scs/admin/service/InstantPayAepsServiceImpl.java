package com.scs.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.entities.AepsMerchantStatus;
import com.scs.admin.entities.Retailer;
import com.scs.admin.entities.VendorCredentials;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.entities.vendor.Vendors;
import com.scs.admin.mapper.AepsVendorMapper;
import com.scs.admin.repositories.*;
import com.scs.admin.request.*;
import com.scs.admin.response.*;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.InstantpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InstantPayAepsServiceImpl implements AepsVendorService{


    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ResourceLoader resourceLoader;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private Aeps2FaRepository aAeps2FaRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Override
    public CommonResponse<AepsMerchantResponseDto> onboardMerchant(AepsMerchantRequestDto request) {

        try {

            // ===== STEP 1 : GET VENDOR ROUTING =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(request.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for vendor"));

            // ===== STEP 2 : GET RETAILER =====
            Optional<Retailer> opt = retailerRepository.findById(request.getRetailerId());

            if (opt.isEmpty()) {
                return CommonResponse.error(400, "Retailer not found");
            }

            Retailer r = opt.get();

            // ===== STEP 3 : PREPARE URL =====
            String url = credentials.getBaseUrlProd()
                    + "/user/outlet/signup/minKyc";

            // ===== STEP 4 : ENCRYPT AADHAAR =====
            String encryptedAadhaar = InstantpayEncryptionUtil.encryptAadhaar(
                    r.getAadhaarNumber(),
                    credentials.getSaltKey()
            );

            // ===== STEP 5 : BUILD REQUEST BODY =====
            Map<String, Object> root = new LinkedHashMap<>();

            root.put("mobile", r.getMobileNumber());
            root.put("name", r.getName());
            root.put("gender", "M");
            root.put("pan", r.getPanNumber());
            root.put("email", r.getEmailId());

            Map<String, Object> address = new LinkedHashMap<>();

            address.put("full",
                    r.getAddress() != null ? r.getAddress().getAddress() : "Default Address");

            address.put("city",
                    r.getAddress() != null ? r.getAddress().getCity() : "Mumbai");

            address.put("pincode",
                    r.getAddress() != null ? r.getAddress().getZipCode() : "400001");

            root.put("address", address);

            root.put("aadhaar", encryptedAadhaar);
            root.put("dateOfBirth", r.getDob().toString());
            root.put("latitude", r.getLatitude());
            root.put("longitude", r.getLongitude());

            ObjectMapper mapper = new ObjectMapper();
            String requestJson = mapper.writeValueAsString(root);

            // ===== STEP 6 : HEADERS =====
            HttpHeaders headers = new HttpHeaders();

            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", r.getCreatedByIPAddress());

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // ===== STEP 7 : CALL INSTANTPAY API =====
            LocalDateTime requestTime = LocalDateTime.now();

            ResponseEntity<InstantpayMerchantResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayMerchantResponseDto.class
                    );

            InstantpayMerchantResponseDto vendorRes = response.getBody();

            if (vendorRes == null) {
                return new CommonResponse<>(500, "Null vendor response", null);
            }

            // ===== STEP 8 : MAP TO COMMON DTO =====
            AepsMerchantResponseDto res =
                    AepsVendorMapper.fromInstantpay(vendorRes);

            String responseJson = mapper.writeValueAsString(vendorRes);

            // ===== STEP 9 : SAVE VENDOR LOGS =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // ===== STEP 10 : CHECK EXISTING MERCHANT =====
            Optional<AepsMerchantStatus> existing =
                    aepsMerchantStatusRepository.findByRetailerId(r.getRetailerId());

            if (existing.isPresent()) {

                AepsMerchantStatus status = existing.get();

                if (res.getData() != null) {
                    status.setMerchantLoginId(res.getData().getMerchantLoginId());
                }

                status.setUpdatedAt(LocalDateTime.now());
                status.setOnboarded(true);
                status.setVendor(vendor);

                aepsMerchantStatusRepository.save(status);

                return new CommonResponse<>(200,
                        "Merchant updated successfully",
                        res);
            }

            // ===== STEP 11 : SUCCESS RESPONSE =====
            if ("TXN".equalsIgnoreCase(vendorRes.getStatuscode())) {

                AepsMerchantStatus status = new AepsMerchantStatus();

                status.setRetailerId(r.getRetailerId());
                status.setMerchantLoginId(
                        String.valueOf(vendorRes.getData().getOutletId())
                );

                status.setOnboarded(true);
                status.setEkycVerified(false);
                status.setBiometricVerified(false);
                status.setStatus("ACTIVE");

                status.setCreatedAt(LocalDateTime.now());
                status.setVendor(vendor);

                aepsMerchantStatusRepository.save(status);

                return new CommonResponse<>(
                        200,
                        "Merchant onboarded successfully",
                        res
                );
            }

            return new CommonResponse<>(
                    400,
                    "Vendor onboarding failed",
                    res
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new CommonResponse<>(
                    500,
                    "Exception : " + e.getMessage(),
                    null
            );
        }


    }

    @Override
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(AepsCombinedRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public String getVendorCode() {
        return "";
    }

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(AepsCashWithdrawalRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials) {
        return null;
    }
}
