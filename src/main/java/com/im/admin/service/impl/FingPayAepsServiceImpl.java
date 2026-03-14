package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.*;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsVendorService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.ConvertImageUrlToBase64;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FingPayAepsServiceImpl implements AepsVendorService {

    @Autowired
    private RetailerRepository retailerRepository;

    private static final String SUPER_USERNAME = "nftpayd";
    private static final String MERCHANT_MD5_PIN = "796c3ee556ac31f3754a38cfd15b8044";  // 81dc9bdb52d04dc20036dbd8313ed055  //1234d
    private static final int SUPER_MERCHANT_ID = 1396;
    private static final String SUPER_PASSWORD_MD5 = "796c3ee556ac31f3754a38cfd15b8044";
    private static final String MERCHANT_PIN = "1234d";
    private static final String FINGPAY_URL = "https://fingpayap.tapits.in/fpaepsweb/api/onboarding/merchant/creation/v2";
    private static final String EKY_TRANSACTION_TYPE = "EKY";

    private static final String SEND_OTP_URL = "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/sendotp";
    private static final String RESEND_OTP_URL = "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/resendotp";
    private static final String BIOMETRIC_EKYC_URL =
            "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/biometric";

    private static final String FINGPAY_2FA_URL = "https://fingpayap.tapits.in/fpaepsservice/auth/tfauth/merchant/validate/aadhar";

    private static final String FINGPAY_CW_URL =
            "https://fingpayap.tapits.in/fpaepsservice/api/cashWithdrawal/merchant/withdrawal";




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


    @Override
    public String getVendorCode() {
        return "fingpay aeps";
    }

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(
            AepsCashWithdrawalRequest req, VendorCredentials credentials) {

        VendorRouting vendorRouting =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);


        // ===== Vendor Log Variables =====
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String errorMsg = null;
        String vendorId = vendor.getId();

        try {
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            Map<String, Object> body = new HashMap<>();

            body.put("merchantTranId", "CW-" + System.currentTimeMillis());
            body.put("captureResponse", req.getCaptureResponse());

            Map<String, Object> cardMap = new HashMap<>();
            cardMap.put("adhaarNumber", req.getCardnumberORUID().getAdhaarNumber());
            cardMap.put("indicatorforUID", 0);
            cardMap.put("nationalBankIdentificationNumber", req.getNationalBankIdentificationNumber());
//            cardMap.put(
//                    "nationalBankIdentificationNumber",
//                    req.getCardnumberORUID().getNationalBankIdentificationNumber()
//            );
            body.put("cardnumberORUID", cardMap);

            body.put("languageCode", "en");
            body.put("serviceType", "AEPS");
            body.put("latitude", req.getLatitude());
            body.put("longitude", req.getLongitude());
            body.put("mobileNumber", req.getMobileNumber());
            body.put("paymentType", "B");
            body.put("requestRemarks", "Test");
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String timestamp = sdf.format(new Date());

            body.put("timestamp", timestamp);
            body.put("transactionAmount", req.getTransactionAmount());
            body.put("transactionType", "CW");

            body.put("merchantUserName", retailer.getRetailerId());
//            body.put("subMerchantId", SUPER_USERNAME);
            body.put("merchantPin", MERCHANT_MD5_PIN);
//            body.put("merchantLoginId", req.getMerchantLoginId());
            body.put("superMerchantId", SUPER_MERCHANT_ID);
            body.put("isFacialTan",req.getIsFacialTan());


            requestJson = objectMapper.writeValueAsString(body);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(requestJson, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "860746040494350");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            ResponseEntity<AepsCashWithdrawalResponse> vendorResp =
                    restTemplate.exchange(
                            FINGPAY_CW_URL,
                            HttpMethod.POST,
                            entity,
                            AepsCashWithdrawalResponse.class
                    );

            responseJson = objectMapper.writeValueAsString(vendorResp.getBody());

            //  SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return new CommonResponse<>(200, "Vendor Response Received", vendorResp.getBody());

        } catch (Exception e) {

            errorMsg = e.getMessage();

            try {
                responseJson = objectMapper.writeValueAsString(
                        new AepsCashWithdrawalResponse(false, errorMsg, 500L, null)
                );
            } catch (Exception ignored) {
            }

            //  FAILURE LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    errorMsg
            );

            return new CommonResponse<>(
                    500,
                    "Exception occurred",
                    new AepsCashWithdrawalResponse(false, errorMsg, 500L, null)
            );
        }
    }


    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


        try {
            String json = objectMapper.writeValueAsString(req);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(json, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", req.getDeviceIMEI() != null ? req.getDeviceIMEI() : "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            return restTemplate.exchange(
                    "https://fingpayap.tapits.in/fpaepsservice/api/balanceInquiry/merchant/getBalance",
                    HttpMethod.POST,
                    entity,
                    AepsBalanceInquiryResponse.class
            );

        } catch (Exception e) {
            e.printStackTrace();
            AepsBalanceInquiryResponse dto = new AepsBalanceInquiryResponse();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500L);
            return ResponseEntity.ok(dto);
        }
    }

    @Override
    public ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
        String vendorId = null;


        try {
            String json = objectMapper.writeValueAsString(req);

            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream().readAllBytes()
            );

            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(json, certificate);

            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", req.getDeviceIMEI() != null ? req.getDeviceIMEI() : "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            return restTemplate.exchange(
                    "https://fingpayap.tapits.in/fpaepsservice/api/miniStatement/merchant/statement",
                    HttpMethod.POST,
                    entity,
                    AepsMiniStatementResponse.class
            );

        } catch (Exception e) {
            e.printStackTrace();
            AepsMiniStatementResponse dto = new AepsMiniStatementResponse();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500L);
            return ResponseEntity.ok(dto);
        }
    }

    @Override
    public CommonResponse<AepsMerchantResponseDto> onboardMerchant(AepsMerchantRequestDto aepsMerchantRequestDto) {


        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(aepsMerchantRequestDto.getSubProductId()); // subProductId dynamically
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

            LocalDateTime requestDateTime = LocalDateTime.now();
            String requestJson = "";
            String responseJson = "";
            String vendorId = vendor.getId();


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
                mkyc.put("shopDistrict", safeString(r.getOutletAddress().getCity(), "Mumbai"));
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

            requestJson = json;

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


            // ===== SAVE VENDOR LOGS =====
            responseJson = mapper.writeValueAsString(res);

            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
// ==================================
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
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req, VendorCredentials credentials) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        // ====== STEP 2: Fetch Retailer ======
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // ====== Vendor Log Variables (MUST be outside try) ======
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId();

        try {

            // ====== STEP 3: Build JSON Payload ======
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("superMerchantId", SUPER_MERCHANT_ID);
            json.put("merchantLoginId", r.getRetailerId());
            json.put("transactionType", EKY_TRANSACTION_TYPE);
            json.put("mobileNumber", r.getMobileNumber());
            json.put("aadharNumber", r.getAadhaarNumber());
            json.put("panNumber", r.getPanNumber());
            json.put("matmSerialNumber", "");
            json.put("latitude", r.getLatitude());
            json.put("longitude", r.getLongitude());

            ObjectMapper mapper = new ObjectMapper();
            String plainJson = mapper.writeValueAsString(json);
            requestJson = plainJson;

            // ====== STEP 4: Load Certificate ======
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // ====== STEP 5: Encrypt Payload ======
            FingpayEncryptionUtil.FingpayRequestPayload payload =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // ====== STEP 6: Prepare Headers ======
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", LocalDateTime.now().toString());
            headers.set("hash", payload.hash);
            headers.set("eskey", payload.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);

            // ====== STEP 7: Call Fingpay Send OTP API ======
            ResponseEntity<AepsEkycResponseDto> vendorResp =
                    restTemplate.exchange(
                            SEND_OTP_URL,
                            HttpMethod.POST,
                            entity,
                            AepsEkycResponseDto.class
                    );

            responseJson = mapper.writeValueAsString(vendorResp.getBody());

            //  SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            CommonResponse<AepsEkycResponseDto> response = new CommonResponse<>();
            response.setStatusCode(vendorResp.getStatusCodeValue());
            response.setMessage("Success");
            response.setData(vendorResp.getBody());

            return ResponseEntity
                    .status(vendorResp.getStatusCode())
                    .body(response);

        } catch (Exception e) {

            CommonResponse<AepsEkycResponseDto> error = new CommonResponse<>();
            error.setStatusCode(500);
            error.setMessage("Vendor API failed: " + e.getMessage());
            error.setData(null);

            try {
                responseJson = new ObjectMapper().writeValueAsString(error);
            } catch (Exception ignored) {
                responseJson = e.getMessage();
            }

            //  FAILURE LOG (MANDATORY)
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
        }
    }

    @Override
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req, VendorCredentials credentials) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId()); // subProductId dynamically aayega
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        // 1. Fetch Retailer
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        //  Declare OUTSIDE try (IMPORTANT)
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId();

        try {

            // 2. Build Plain JSON (as per Fingpay spec)
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("superMerchantId", SUPER_MERCHANT_ID);
            json.put("merchantLoginId", r.getRetailerId());
            json.put("otp", req.getOtp());
            json.put("primaryKeyId", req.getPrimaryKeyId());
            json.put("encodeFPTxnId", req.getEncodeFPTxnId());

            ObjectMapper mapper = new ObjectMapper();
            String plainJson = mapper.writeValueAsString(json);
            requestJson = plainJson;

            // 3. Load certificate
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // 4. Encrypt JSON
            FingpayEncryptionUtil.FingpayRequestPayload payload =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // 5. Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", LocalDateTime.now().toString());
            headers.set("hash", payload.hash);
            headers.set("eskey", payload.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);

            // 6. Hit Validate OTP API
            String VALIDATE_OTP_URL =
                    "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/validateotp";

            ResponseEntity<AepsValidateOtpResponseDto> response =
                    restTemplate.exchange(
                            VALIDATE_OTP_URL,
                            HttpMethod.POST,
                            entity,
                            AepsValidateOtpResponseDto.class
                    );

            responseJson = mapper.writeValueAsString(response.getBody());

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return response;

        } catch (Exception ex) {

            try {
                ObjectMapper mapper = new ObjectMapper();
                responseJson = mapper.writeValueAsString(
                        new CommonResponse<>(500, ex.getMessage(), null)
                );
            } catch (Exception ignored) {
                responseJson = ex.getMessage();
            }

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    ex.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }


    @Override
    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req, VendorCredentials credentials) {


        // STEP 1: Load Vendor Credentials
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        // STEP 1: Fetch Retailer
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // ===== Vendor Log Variables (outside try) =====
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId(); // or vendor.getId() if dynamic

        try {
            // STEP 2: Build JSON Payload
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("superMerchantId", SUPER_MERCHANT_ID);
            json.put("merchantLoginId", r.getRetailerId());
            json.put("primaryKeyId", req.getPrimaryKeyId());
            json.put("encodeFPTxnId", req.getEncodeFPTxnId());

            ObjectMapper mapper = new ObjectMapper();
            String plainJson = mapper.writeValueAsString(json);
            requestJson = plainJson;

            // STEP 3: Read Certificate
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // STEP 4: Encrypt JSON
            FingpayEncryptionUtil.FingpayRequestPayload payload =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // STEP 5: Create Headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", LocalDateTime.now().toString());
            headers.set("hash", payload.hash);
            headers.set("eskey", payload.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);

            // STEP 6: Vendor API Call
            ResponseEntity<AepsResendOtpResponseDto> vendorResp =
                    restTemplate.exchange(
                            RESEND_OTP_URL,
                            HttpMethod.POST,
                            entity,
                            AepsResendOtpResponseDto.class
                    );

            responseJson = mapper.writeValueAsString(vendorResp.getBody());

            //  SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            CommonResponse<AepsResendOtpResponseDto> response = new CommonResponse<>();
            response.setStatusCode(vendorResp.getStatusCodeValue());
            response.setMessage("Success");
            response.setData(vendorResp.getBody());

            return ResponseEntity.status(vendorResp.getStatusCode()).body(response);

        } catch (Exception e) {

            CommonResponse<AepsResendOtpResponseDto> error = new CommonResponse<>();
            error.setStatusCode(500);
            error.setMessage("Vendor API failed: " + e.getMessage());
            error.setData(null);

            try {
                responseJson = new ObjectMapper().writeValueAsString(error);
            } catch (Exception ignored) {
                responseJson = e.getMessage();
            }

            //  FAILURE LOG (MANDATORY)
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
        }
    }

    @Override
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req, VendorCredentials credentials) {

        // ====== STEP 1: Load Vendor Credentials Dynamically ======
        VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId()); // subProductId dynamically aayega
        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        //  Declare outside try for catch access
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId();

        try {
            // STEP 1: Backend-managed fields
            req.setSuperMerchantId(SUPER_MERCHANT_ID);

            // STEP 2: Build final JSON
            String plainJson = objectMapper.writeValueAsString(req);
            requestJson = plainJson;

            // STEP 3: Load Public Key Certificate
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // STEP 4: Encrypt JSON
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // STEP 5: Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "860746040494350");
            headers.setContentType(MediaType.TEXT_PLAIN);

            // STEP 6: Build request
            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            // STEP 7: Call API
            ResponseEntity<AepsBiometricEkycResponseDto> response =
                    restTemplate.exchange(
                            BIOMETRIC_EKYC_URL,
                            HttpMethod.POST,
                            entity,
                            AepsBiometricEkycResponseDto.class
                    );

            responseJson = objectMapper.writeValueAsString(response.getBody());

            //  SUCCESS LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return response;

        } catch (Exception e) {

            AepsBiometricEkycResponseDto dto = new AepsBiometricEkycResponseDto();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500);

            try {
                responseJson = objectMapper.writeValueAsString(dto);
            } catch (Exception ignored) {
                responseJson = e.getMessage();
            }

            //  FAILURE LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    e.getMessage()
            );

            return ResponseEntity.ok(dto);
        }
    }

    @Override
    public ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(AepsCombinedRequest req, VendorCredentials credentials) {

        CommonResponse<AepsBiometricEkycResponseDto> common = new CommonResponse<>();

        try {
            // STEP 1: Load Vendor Credentials
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentialsNew = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ===== Step 1: Validate OTP =====
            AepsValidateOtpRequest validateReq = new AepsValidateOtpRequest();
            validateReq.setRetailerId(req.getRetailerId());
            validateReq.setOtp(req.getOtp());
            validateReq.setPrimaryKeyId(req.getPrimaryKeyId());
            validateReq.setEncodeFPTxnId(req.getEncodeFPTxnId());
            validateReq.setSubProductId(req.getSubProductId());

            ResponseEntity<AepsValidateOtpResponseDto> validateResponse = validateOtp(validateReq,credentials);
            AepsValidateOtpResponseDto otpRes = validateResponse.getBody();

            // OTP Failed
            if (otpRes == null || !otpRes.isStatus() || otpRes.getData() == null) {

                common.setStatusCode(400);
                common.setMessage("OTP Validation Failed: " + (otpRes != null ? otpRes.getMessage() : "No response from vendor"));
                common.setData(null);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(common);
            }

            // ===== Step 2: Get Aadhaar from DB =====
            Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + req.getRetailerId()));

            String aadhaar = retailer.getAadhaarNumber();

            // ===== Step 3: Prepare Biometric Request =====
            AepsBiometricEkycRequest bioReq = new AepsBiometricEkycRequest();
//            bioReq.setRetailerId(req.getRetailerId());
            bioReq.setRequestRemarks("test");
            bioReq.setSubProductId(req.getSubProductId());
            bioReq.setIsFacialTan(req.getIsFacialTan());

            // ==== Convert CaptureResponse to correct DTO ====
            AepsBiometricEkycRequest.CaptureResponse cap = new AepsBiometricEkycRequest.CaptureResponse();
            AepsCombinedRequest.CaptureResponse rc = req.getCaptureResponse();

            if (rc != null) {
                cap.setErrCode(rc.getErrCode());
                cap.setErrInfo(rc.getErrInfo());
                cap.setFCount(rc.getFCount());
                cap.setFType(rc.getFType());
                cap.setICount(rc.getICount());
                cap.setIType(rc.getIType());
                cap.setPCount(rc.getPCount());
                cap.setPType(rc.getPType());
                cap.setNmPoints(rc.getNmPoints());
                cap.setQScore(rc.getQScore());
                cap.setDpID(rc.getDpID());
                cap.setRdsID(rc.getRdsID());
                cap.setRdsVer(rc.getRdsVer());
                cap.setDc(rc.getDc());
                cap.setMi(rc.getMi());
                cap.setMc(rc.getMc());
                cap.setCi(rc.getCi());
                cap.setSessionKey(rc.getSessionKey());
                cap.setHmac(rc.getHmac());
                cap.setPidDatatype(rc.getPidDatatype());

                // Final piddata selection
                String finalPidData = (rc.getPiddata() != null && !rc.getPiddata().isEmpty())
                        ? rc.getPiddata()
                        : rc.getPiddata();
                cap.setPiddata(finalPidData);
            }

            bioReq.setCaptureResponse(cap);

            // UID
            AepsBiometricEkycRequest.P2CardnumberORUID uid = new AepsBiometricEkycRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(aadhaar);
            uid.setIndicatorforUID("0");
            uid.setNationalBankIdentificationNumber("");
            bioReq.setCardnumberORUID(uid);

            // Carry forward PK ID & Encoded FP Txn ID
            bioReq.setPrimaryKeyId(otpRes.getData().getPrimaryKeyId());
            bioReq.setEncodeFPTxnId(otpRes.getData().getEncodeFPTxnId());

            bioReq.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            bioReq.getCardnumberORUID().setAdhaarNumber(retailer.getAadhaarNumber());


            // ===== Step 4: Call Biometric API =====
            ResponseEntity<AepsBiometricEkycResponseDto> bioResponse = biometricEkyc(bioReq,credentials);
            AepsBiometricEkycResponseDto biometricRes = bioResponse.getBody();

            // ===== Step 5: Update Status Table On Success =====
            if (biometricRes != null && biometricRes.isStatus()) {

                Optional<AepsMerchantStatus> opt = aepsMerchantStatusRepository.findByRetailerId(req.getRetailerId());

                if (opt.isPresent()) {
                    AepsMerchantStatus status = opt.get();

                    status.setEkycVerified(true);
                    status.setBiometricVerified(true);
                    status.setUpdatedAt(LocalDateTime.now());
                    status.setStatus("ACTIVE");

                    aepsMerchantStatusRepository.save(status);
                }
            }

            // SUCCESS RESPONSE
            common.setStatusCode(bioResponse.getStatusCodeValue());
            common.setMessage("Success");
            common.setData(biometricRes);

            return ResponseEntity.status(bioResponse.getStatusCode()).body(common);

        } catch (Exception e) {

            e.printStackTrace();

            // ERROR RESPONSE
            common.setStatusCode(500);
            common.setMessage("Exception: " + e.getMessage());
            common.setData(null);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(common);
        }
    }

    @Override
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req, VendorCredentials credentials) {

        // ====== STEP 1: Load Vendor Credentials ======
        VendorRouting vendorRouting =
                vendorRoutingRepository.findBySubProductId(req.getSubProductId());

        Vendors vendor = CommonMethods.getVendors(vendorRouting);

        VendorCredentials credentialsNew = vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() ->
                        new RuntimeException("No credentials found for selected vendor"));

        CommonResponse<Aeps2FaResponseDto> common = new CommonResponse<>();

        // ===== Vendor Log Variables (outside try) =====
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";
        String vendorId = vendor.getId();

        try {
            // ====== STEP 2: Load Retailer ======
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // ====== STEP 3: Populate Backend Fields ======
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            req.setMerchantUserName(retailer.getRetailerId());
            req.setMerchantPin(MERCHANT_MD5_PIN);
            req.setTransactionType("AUO");
            req.setServiceType("AEPS");
            req.setTimestamp(System.currentTimeMillis());
            req.setMerchantTranId("TXN" + System.currentTimeMillis());
            req.setLatitude(retailer.getLatitude());
            req.setLongitude(retailer.getLongitude());
            req.setMobileNumber(retailer.getMobileNumber());

            // ====== STEP 4: UID Block ======
            Aeps2FaRequest.P2CardnumberORUID uid =
                    new Aeps2FaRequest.P2CardnumberORUID();

            uid.setAdhaarNumber(req.getCardnumberORUID().getAdhaarNumber());
            uid.setIndicatorforUID("0");
            uid.setNationalBankIdentificationNumber(req.getCardnumberORUID().getNationalBankIdentificationNumber());

            req.setCardnumberORUID(uid);

            // ====== STEP 5: Convert Request to JSON ======
            String plainJson = objectMapper.writeValueAsString(req);
            requestJson = plainJson;

            // ====== STEP 6: Load Fingpay Public Key ======
            String certificate = new String(
                    resourceLoader.getResource("classpath:fingpay_public.cer")
                            .getInputStream()
                            .readAllBytes()
            );

            // ====== STEP 7: Encrypt Request ======
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(
                            plainJson, certificate
                    );

            // ====== STEP 8: Prepare Headers ======
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity =
                    new HttpEntity<>(encrypted.body, headers);

            // ====== STEP 9: Vendor API Call ======
            ResponseEntity<Aeps2FaResponseDto> vendorResp =
                    restTemplate.exchange(
                            FINGPAY_2FA_URL,
                            HttpMethod.POST,
                            entity,
                            Aeps2FaResponseDto.class
                    );

            Aeps2FaResponseDto body = vendorResp.getBody();
            responseJson = objectMapper.writeValueAsString(body);

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            // ====== STEP 10: Save Business Log ONLY on SUCCESS ======
            if (body != null && body.isStatus()
                    && body.getData() != null
                    && "00".equals(body.getData().getResponseCode())) {

//                AepsTwoFa log = new AepsTwoFa();
//                log.setTefPkId(body.getData().getTefPkId());
//                log.setStan(body.getData().getStan());
//                log.setBankRrn(body.getData().getBankRrn());
//                log.setFpRrn(body.getData().getFpRrn());
//                log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
//                log.setMerchantTranId(body.getData().getMerchantTranId());
//                log.setResponseCode(body.getData().getResponseCode());
//                log.setResponseMessage(body.getData().getResponseMessage());
//                log.setMobileNumber(body.getData().getMobileNumber());
//                log.setTransactionTimestamp(body.getData().getTransactionTimestamp());
//                log.setRetailerId(req.getRetailerId());
//                log.setCreatedAt(LocalDateTime.now());
//                log.setVendor(vendor);
//
//                aAeps2FaRepository.save(log);

                Optional<AepsTwoFa> existingOpt =
                        aAeps2FaRepository.findByRetailerId(req.getRetailerId());

                AepsTwoFa log;

                if (existingOpt.isPresent()) {
                    // UPDATE
                    log = existingOpt.get();

                    log.setTefPkId(body.getData().getTefPkId());
                    log.setStan(body.getData().getStan());
                    log.setBankRrn(body.getData().getBankRrn());
                    log.setFpRrn(body.getData().getFpRrn());
                    log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
                    log.setMerchantTranId(body.getData().getMerchantTranId());
                    log.setResponseCode(body.getData().getResponseCode());
                    log.setResponseMessage(body.getData().getResponseMessage());
                    log.setMobileNumber(body.getData().getMobileNumber());
                    log.setTransactionTimestamp(body.getData().getTransactionTimestamp());
//                    log.setUpdatedAt(LocalDateTime.now());

                    aAeps2FaRepository.save(log);

                } else {
                    // INSERT
                    log = new AepsTwoFa();
                    log.setRetailerId(req.getRetailerId());
                    log.setVendor(vendor);
                    log.setCreatedAt(LocalDateTime.now());

                    log.setTefPkId(body.getData().getTefPkId());
                    log.setStan(body.getData().getStan());
                    log.setBankRrn(body.getData().getBankRrn());
                    log.setFpRrn(body.getData().getFpRrn());
                    log.setFingpayTransactionId(body.getData().getFingpayTransactionId());
                    log.setMerchantTranId(body.getData().getMerchantTranId());
                    log.setResponseCode(body.getData().getResponseCode());
                    log.setResponseMessage(body.getData().getResponseMessage());
                    log.setMobileNumber(body.getData().getMobileNumber());
                    log.setTransactionTimestamp(body.getData().getTransactionTimestamp());
//                    log.setUpdatedAt(LocalDateTime.now());

                    aAeps2FaRepository.save(log);

                }
            }

            // ====== STEP 11: FINAL RESPONSE ======
            if (body != null && body.isStatus()
                    && body.getData() != null
                    && "00".equals(body.getData().getResponseCode())) {

                common.setStatusCode(200);
                common.setMessage(body.getData().getResponseMessage());
                common.setData(body);
                return ResponseEntity.ok(common);

            } else {

                String vendorMsg = (body != null)
                        ? body.getMessage()
                        : "Vendor response empty";

                common.setStatusCode(400);
                common.setMessage(vendorMsg);
                common.setData(body);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(common);
            }

        } catch (Exception e) {

            common.setStatusCode(500);
            common.setMessage("Vendor API failed: " + e.getMessage());
            common.setData(null);

            try {
                responseJson = objectMapper.writeValueAsString(common);
            } catch (Exception ignored) {
                responseJson = e.getMessage();
            }

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestDateTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    e.getMessage()
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(common);
        }
    }
    }


