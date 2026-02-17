package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.AepsMerchantStatus;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.AepsMerchantStatusRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsEkycService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.FingpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AepsEkycServiceImpl implements AepsEkycService {

    private static final Integer SUPER_MERCHANT_ID = 1396;   // your company ID
    private static final String EKY_TRANSACTION_TYPE = "EKY";
    private static final String SEND_OTP_URL = "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/sendotp";
    private static final String RESEND_OTP_URL = "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/resendotp";
    private static final String BIOMETRIC_EKYC_URL =
            "https://fpekyc.tapits.in/fpekyc/api/ekyc/merchant/biometric";


    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;



    @Override
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req) {

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
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req) {

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
    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req) {

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
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req) {

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
    public ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(
            AepsCombinedRequest req) {

        CommonResponse<AepsBiometricEkycResponseDto> common = new CommonResponse<>();

        try {
            // STEP 1: Load Vendor Credentials
            VendorRouting vendorRouting = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            // ===== Step 1: Validate OTP =====
            AepsValidateOtpRequest validateReq = new AepsValidateOtpRequest();
            validateReq.setRetailerId(req.getRetailerId());
            validateReq.setOtp(req.getOtp());
            validateReq.setPrimaryKeyId(req.getPrimaryKeyId());
            validateReq.setEncodeFPTxnId(req.getEncodeFPTxnId());
            validateReq.setSubProductId(req.getSubProductId());

            ResponseEntity<AepsValidateOtpResponseDto> validateResponse = validateOtp(validateReq);
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
            ResponseEntity<AepsBiometricEkycResponseDto> bioResponse = biometricEkyc(bioReq);
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


}
