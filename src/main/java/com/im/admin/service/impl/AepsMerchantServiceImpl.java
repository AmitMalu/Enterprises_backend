package com.im.admin.service.impl;

import com.im.admin.entities.*;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.AepsMerchantStatusRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.AepsCheckStatus;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.request.AepsStatusDataDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.AepsStatusCheckResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsMerchantService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ConvertImageUrlToBase64;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AepsMerchantServiceImpl implements AepsMerchantService {

    private static final String SUPER_USERNAME = "nftpayd";
    private static final String SUPER_PASSWORD_MD5 = "796c3ee556ac31f3754a38cfd15b8044";
    private static final Integer SUPER_MERCHANT_ID = 1396;
    private static final String MERCHANT_PIN = "1234d";
    private static final String FINGPAY_URL = "https://fingpayap.tapits.in/fpaepsweb/api/onboarding/merchant/creation/v2";

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;

    @Override
    public CommonResponse<AepsMerchantResponseDto> registerMerchantAndCall(AepsMerchantRequestDto aepsMerchantRequestDto) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(aepsMerchantRequestDto.getSubProductId()); // subProductId dynamically aayega
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentials = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

        Optional<Retailer> opt = retailerRepository.findById(aepsMerchantRequestDto.getRetailerId());

        if (opt.isEmpty()) {
            return CommonResponse.error(
                    400,
                    "Invalid retailerId: Retailer not found"
            );
        }


        Retailer r = opt.get();
        BanksDetails bank = r.getBankDetails();

        try {
            // Build JSON payload
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("username", SUPER_USERNAME);
            root.put("password", SUPER_PASSWORD_MD5);
            root.put("ipAddress", r.getCreatedByIPAddress());
            root.put("latitude", r.getLatitude());
            root.put("longitude", r.getLongitude());
            root.put("supermerchantId", SUPER_MERCHANT_ID);

            Map<String, Object> merchant = new LinkedHashMap<>();
            merchant.put("merchantLoginId", r.getRetailerId());
            merchant.put("merchantLoginPin", MERCHANT_PIN);

            // Name split
            String fullName = r.getName();
            String first = fullName.split(" ", 2)[0];
            String last = fullName.contains(" ") ? fullName.split(" ", 2)[1] : "";
            merchant.put("firstName", first);
            merchant.put("lastName", last);
            merchant.put("middleName", "");
            merchant.put("merchantPhoneNumber", r.getMobileNumber());
            // Merchant Address
            Map<String, Object> merchantAddress = new LinkedHashMap<>();
            String mainAddr = r.getAddress() != null ? r.getAddress().getAddress() : "";

            merchantAddress.put("merchantAddress1", ensureMinLen(mainAddr, 11, " Address"));
            merchantAddress.put("merchantAddress2", ensureMinLen("Near Test Building Complex", 11, " Building"));
            merchantAddress.put("merchantState", 15);
            merchantAddress.put("merchantCityName",
                    r.getAddress() != null ? r.getAddress().getCity() : null);

            merchantAddress.put("merchantDistrictName", "Hyderabad");
            merchantAddress.put("merchantPinCode",
                    r.getAddress() != null ? r.getAddress().getZipCode() : null);

            merchant.put("merchantAddress", merchantAddress);

            merchant.put("companyLegalName", "Svcreditlineltd");
            merchant.put("userType", "lakshmi");
            merchant.put("companyType", 4816);
            merchant.put("emailId", r.getEmailId());

            merchant.put("certificateOfIncorporationImage", "true");

            // KYC
            Map<String, Object> kyc = new LinkedHashMap<>();
            kyc.put("userPan", r.getPanNumber());
            kyc.put("aadhaarNumber", r.getAadhaarNumber());

            kyc.put("gstinNumber", safeString("27ABCDE1234F1Z2", "27ABCDE1234F1Z2"));
            kyc.put("companyOrShopPan", safeString(r.getPanNumber(), r.getPanNumber()));
//            kyc.put("merchantPanImage", r.getPanCardImage());

            String panBase64 = ConvertImageUrlToBase64.fromUrl(r.getPanCardImage());
            kyc.put("merchantPanImage", panBase64);

            kyc.put("merchantPanImage", panBase64);


//            kyc.put("maskedAadharImage", r.getAadhaarCardFrontImage());

            String aadhaarBase64 = ConvertImageUrlToBase64.fromUrl(r.getAadhaarCardFrontImage());
            kyc.put("maskedAadharImage", aadhaarBase64);


            kyc.put("shopAndPanImage", "true");
            merchant.put("kyc", kyc);

            // Settlement
            Map<String, Object> settlement = new LinkedHashMap<>();
            if (bank != null) {
                settlement.put("companyBankAccountNumber", bank.getBankAccountNumber());
                settlement.put("bankIfscCode", bank.getIfscCode());
                settlement.put("companyBankName", bank.getBankName());
                settlement.put("bankAccountName", "Savings account");
            } else {
                settlement.put("companyBankAccountNumber", "9876543210123456");
                settlement.put("bankIfscCode", "SBIN0005678");
                settlement.put("companyBankName", "State Bank of India");
                settlement.put("bankAccountName", "Savings account");
            }
            merchant.put("settlementV1", settlement);

            // Boolean flags
            merchant.put("tradeBusinessProof", "true");
            merchant.put("termsConditionCheck", "true");
            merchant.put("cancelledChequeImages", "true");
            merchant.put("physicalVerification", "true");
            merchant.put("videoKycWithLatLongData", "true");

            // Outlet Address
            Map<String, Object> mkyc = new LinkedHashMap<>();
            if (r.getOutletAddress() != null) {
                mkyc.put("shopAddress", ensureMinLen(safeString(r.getOutletAddress().getAddress(), "Highway Road Market"), 5, "Shop"));
                mkyc.put("shopCity", safeString(r.getOutletAddress().getCity(), "Navi Mumbai"));
                mkyc.put("shopDistrict", "Hyderabad");
                mkyc.put("shopState", 15);
                mkyc.put("shopPincode", safeString(r.getOutletAddress().getZipCode(), "411001"));
            } else {
                mkyc.put("shopAddress", "Highway Road Market");
                mkyc.put("shopCity", "Navi Mumbai");
                mkyc.put("shopDistrict", "Hyderabad");
                mkyc.put("shopState", 15);
                mkyc.put("shopPincode", "411001");
            }
            mkyc.put("shopLatitude", r.getLatitude());
            mkyc.put("shopLongitude", r.getLongitude());
//            mkyc.put("backgroundImageOfShop", r.getShopImage());

            String shopImageBase64 = ConvertImageUrlToBase64.fromUrl(r.getShopImage());
            mkyc.put("backgroundImageOfShop", shopImageBase64);


            merchant.put("merchantKycAddressData", mkyc);

            root.put("merchant", merchant);

            // Convert JSON and call Fingpay
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(root);

            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            FingpayEncryptionUtil.FingpayRequestPayload payload =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(json, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", LocalDateTime.now().toString());
            headers.set("hash", payload.hash);
            headers.set("eskey", payload.eskey);
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);
            ResponseEntity<AepsMerchantResponseDto> response = restTemplate.exchange(
                    FINGPAY_URL,
                    HttpMethod.POST,
                    entity,
                    AepsMerchantResponseDto.class
            );
            AepsMerchantResponseDto res = response.getBody();

            // ---- Vendor Null Check ----
            if (res == null) {
                return new CommonResponse<>(500, "Null vendor response", null);
            }

            // ---- Check if Merchant Already Exists ----
            Optional<AepsMerchantStatus> existingOpt =
                    aepsMerchantStatusRepository.findByRetailerId(r.getRetailerId());

            if (existingOpt.isPresent()) {
                // Merchant exists → UPDATE the details
                AepsMerchantStatus status = existingOpt.get();

                if (res.isStatus()
                        && res.getData() != null
                        && res.getData().getMerchantLoginId() != null) {

                    status.setMerchantLoginId(res.getData().getMerchantLoginId());
                }

                status.setUpdatedAt(LocalDateTime.now());
                status.setOnboarded(true);
                status.setVendor(vendor);

                aepsMerchantStatusRepository.save(status);

                return new CommonResponse<>(200, "Merchant updated successfully", res);
            }


            // ---- SUCCESS RESPONSE FROM VENDOR ----
            if (res.isStatus()
                    && "successful".equalsIgnoreCase(res.getMessage())
                    && res.getData() != null) {

                // Create New Status Record
                AepsMerchantStatus status = new AepsMerchantStatus();
                status.setRetailerId(r.getRetailerId());
                status.setMerchantLoginId(res.getData().getMerchantLoginId());
                status.setOnboarded(true);
                status.setEkycVerified(false);
                status.setBiometricVerified(false);
                status.setStatus("ACTIVE");
                status.setCreatedAt(LocalDateTime.now());
                status.setVendor(vendor);

                aepsMerchantStatusRepository.save(status);

                return new CommonResponse<>(200, "Merchant Onboarded Successfully", res);
            }

            // ---- Vendor Returned Failure ----
            return new CommonResponse<>(400, "Vendor onboarding failed", res);

        } catch (Exception ex) {
            ex.printStackTrace();
            return new CommonResponse<>(500, "Exception: " + ex.getMessage(), null);
        }
    }

    private String safeString(String v, String defaultVal) {
        return (v == null || v.trim().isEmpty()) ? defaultVal : v;
    }

    private String ensureMinLen(String s, int min, String suffixIfPad) {
        if (s == null) s = "";
        s = s.trim();
        if (s.length() >= min) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < min) sb.append(suffixIfPad);
        return sb.toString().substring(0, Math.max(min, sb.length()));
    }


    @Override
    public CommonResponse<AepsStatusDataDto> checkStatus(AepsCheckStatus req) {

        CommonResponse<AepsStatusDataDto> resp = new CommonResponse<>();
        AepsStatusDataDto data = new AepsStatusDataDto();

        // Fetch retailer first (for profile)
        Optional<Retailer> retailerOpt = retailerRepository.findByRetailerId(req.getRetailerId());

        AepsStatusDataDto.Profile profile = new AepsStatusDataDto.Profile();

        if (retailerOpt.isPresent()) {
            Retailer r = retailerOpt.get();

            profile.setRetailerId(r.getRetailerId());
            profile.setName(r.getName());
            profile.setMobileNumber(r.getMobileNumber());
            profile.setEmailId(r.getEmailId());
            profile.setDob(r.getDob() != null ? r.getDob().toString() : null);
            profile.setAadhaarNumber(r.getAadhaarNumber());
            profile.setPanNumber(r.getPanNumber());
        }

        data.setProfile(profile);

        // Check merchant AEPS status
        Optional<AepsMerchantStatus> opt =
                aepsMerchantStatusRepository.findByRetailerId(req.getRetailerId());

        // CASE 1: Not onboarded
        if (opt.isEmpty()) {
            data.setOnboardStatus(0);
            resp.setStatusCode(200);
            resp.setMessage("Merchant onboarding not done");
            resp.setData(data);
            return resp;
        }

        AepsMerchantStatus status = opt.get();

        // CASE 2: Onboarded but EKYC or Biometric pending
        if (status.getOnboarded() &&
                (!status.getEkycVerified() || !status.getBiometricVerified())) {

            data.setOnboardStatus(1);

            resp.setStatusCode(200);
            resp.setMessage("Onboarding done but EKYC / Biometric pending");
            resp.setData(data);
            return resp;
        }

        // CASE 3: Fully activated
        if (status.getOnboarded() && status.getEkycVerified() && status.getBiometricVerified()) {
            data.setOnboardStatus(2);

            resp.setStatusCode(200);
            resp.setMessage("AEPS fully activated");
            resp.setData(data);
            return resp;
        }

        // DEFAULT
        data.setOnboardStatus(0);
        resp.setStatusCode(200);
        resp.setMessage("Unknown AEPS status");
        resp.setData(data);
        return resp;
    }

}
