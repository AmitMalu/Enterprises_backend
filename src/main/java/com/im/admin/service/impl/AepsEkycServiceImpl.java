package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.AepsMerchantStatus;
import com.im.admin.entities.Retailer;
import com.im.admin.repositories.AepsMerchantStatusRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsEkycService;
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



    @Override
    public ResponseEntity<AepsEkycResponseDto> sendOtp(AepsSendOtpRequest req) {
        // STEP 1: Fetch Retailer
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        try {
            // STEP 2: Build JSON Payload
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

            // STEP 3: Read Public Key Certificate
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // STEP 4: Encrypt JSON
            FingpayEncryptionUtil.FingpayRequestPayload payload =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // STEP 5: Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", LocalDateTime.now().toString());
            headers.set("hash", payload.hash);
            headers.set("eskey", payload.eskey);
            headers.set("deviceIMEI", "WEB1234567");   // static IMEI for web
            headers.setContentType(MediaType.TEXT_PLAIN);

            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);

            // STEP 6: Call Fingpay Send OTP API
            ResponseEntity<AepsEkycResponseDto> response = restTemplate.exchange(
                    SEND_OTP_URL,
                    HttpMethod.POST,
                    entity,
                    AepsEkycResponseDto.class
            );

            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req) {
        // 1. Fetch Retailer
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

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

            ResponseEntity<AepsValidateOtpResponseDto> response = restTemplate.exchange(
                    VALIDATE_OTP_URL,
                    HttpMethod.POST,
                    entity,
                    AepsValidateOtpResponseDto.class
            );

            return response;

        } catch (Exception ex) {
            ex.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<AepsResendOtpResponseDto> resendOtp(AepsResendOtpRequest req) {
        // STEP 1: Fetch Retailer
        Retailer r = retailerRepository.findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        try {
            // STEP 2: Build JSON Payload
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("superMerchantId", SUPER_MERCHANT_ID);
            json.put("merchantLoginId", r.getRetailerId());
            json.put("primaryKeyId", req.getPrimaryKeyId());
            json.put("encodeFPTxnId", req.getEncodeFPTxnId());

            ObjectMapper mapper = new ObjectMapper();
            String plainJson = mapper.writeValueAsString(json);

            // STEP 3: Read Public Key Certificate
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

            // STEP 6: Call Fingpay API
            HttpEntity<String> entity = new HttpEntity<>(payload.body, headers);

            ResponseEntity<AepsResendOtpResponseDto> response = restTemplate.exchange(
                    RESEND_OTP_URL,
                    HttpMethod.POST,
                    entity,
                    AepsResendOtpResponseDto.class
            );

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @Override
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req) {

        try {
            // STEP 1: Load Retailer
            Retailer retailer = retailerRepository.findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // STEP 2: Set backend-managed fields
            req.setSuperMerchantId(SUPER_MERCHANT_ID);
            req.setMerchantLoginId(String.valueOf(retailer.getRetailerId()));
            req.getCardnumberORUID().setAdhaarNumber(retailer.getAadhaarNumber());

            // STEP 3: Build final JSON
            String plainJson = objectMapper.writeValueAsString(req);

            // STEP 4: Load Public Key Certificate (required!)
            Resource resource = resourceLoader.getResource("classpath:fingpay_public.cer");
            String certificate = new String(resource.getInputStream().readAllBytes());

            // STEP 5: Encrypt JSON with certificate
            FingpayEncryptionUtil.FingpayRequestPayload encrypted =
                    FingpayEncryptionUtil.buildFinalRequestFromRawJson(plainJson, certificate);

            // STEP 6: Prepare headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("trnTimestamp", String.valueOf(System.currentTimeMillis()));
            headers.set("hash", encrypted.hash);
            headers.set("eskey", encrypted.eskey);
            headers.set("deviceIMEI", "WEB1234567");
            headers.setContentType(MediaType.TEXT_PLAIN);

            // STEP 7: Build request
            HttpEntity<String> entity = new HttpEntity<>(encrypted.body, headers);

            // STEP 8: Call API
            ResponseEntity<AepsBiometricEkycResponseDto> response =
                    restTemplate.exchange(
                            BIOMETRIC_EKYC_URL,
                            HttpMethod.POST,
                            entity,
                            AepsBiometricEkycResponseDto.class
                    );

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            AepsBiometricEkycResponseDto dto = new AepsBiometricEkycResponseDto();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500);
            return ResponseEntity.ok(dto);
        }
    }


    @Override
    public ResponseEntity<AepsBiometricEkycResponseDto> validateOtpThenBiometric(
            AepsCombinedRequest req) {

        try {

            // STEP 1: Build Validate OTP Request
            AepsValidateOtpRequest validateReq = new AepsValidateOtpRequest();
            validateReq.setRetailerId(req.getRetailerId());
            validateReq.setOtp(req.getOtp());
            validateReq.setPrimaryKeyId(req.getPrimaryKeyId());
            validateReq.setEncodeFPTxnId(req.getEncodeFPTxnId());

            // STEP 2: Call Validate OTP API internally
            ResponseEntity<AepsValidateOtpResponseDto> validateResponse = validateOtp(validateReq);

            AepsValidateOtpResponseDto body = validateResponse.getBody();

            if (body == null || !body.isStatus() || body.getData() == null) {

                AepsBiometricEkycResponseDto dto = new AepsBiometricEkycResponseDto();
                dto.setStatus(false);
                dto.setMessage("OTP Validation Failed: " + (body != null ? body.getMessage() : "No response from FingPay"));
                dto.setStatusCode(400);
                dto.setData(null);
                return ResponseEntity.ok(dto);
            }


            // Fetch Aadhaar from DB
            Optional<Retailer> retailerOpt = retailerRepository.findByRetailerId(req.getRetailerId());

            Retailer retailer = retailerOpt.orElseThrow(
                    () -> new RuntimeException("Retailer not found with ID: " + req.getRetailerId())
            );
                   // Aadhaar from DB
            String aadhaar = retailer.getAadhaarNumber();

            // STEP 3: Build Biometric Request (AUTO SET cardnumberORUID)
            AepsBiometricEkycRequest bioReq = new AepsBiometricEkycRequest();
            bioReq.setRetailerId(req.getRetailerId());
            bioReq.setRequestRemarks(req.getRequestRemarks());
            bioReq.setCaptureResponse(req.getCaptureResponse());

            // AUTO BUILD cardnumberORUID
            AepsBiometricEkycRequest.P2CardnumberORUID uid = new AepsBiometricEkycRequest.P2CardnumberORUID();
            uid.setAdhaarNumber(aadhaar); // from DB
            uid.setIndicatorforUID(0); // default
            uid.setNationalBankIdentificationNumber(null); // default

            bioReq.setCardnumberORUID(uid);

            // Use same values from validate API
            bioReq.setPrimaryKeyId(validateResponse.getBody().getData().getPrimaryKeyId());
            bioReq.setEncodeFPTxnId(validateResponse.getBody().getData().getEncodeFPTxnId());

            // STEP 4: Call Biometric API internally
            ResponseEntity<AepsBiometricEkycResponseDto> aepsBiometricEkycResponseDtoResponseEntity = biometricEkyc(bioReq);

            // STEP 5: UPDATE STATUS TABLE AFTER SUCCESSFUL BIOMETRIC EKYC
            AepsBiometricEkycResponseDto biometricRes =
                    aepsBiometricEkycResponseDtoResponseEntity.getBody();

            if (biometricRes != null && biometricRes.isStatus()) {

                Optional<AepsMerchantStatus> optStatus =
                        aepsMerchantStatusRepository.findByRetailerId(req.getRetailerId());

                if (optStatus.isPresent()) {

                    AepsMerchantStatus status = optStatus.get();

                    status.setEkycVerified(true);
                    status.setBiometricVerified(true);
                    status.setUpdatedAt(LocalDateTime.now());
                    status.setStatus("ACTIVE");

                    // NOTE: Vendor biometric response does NOT return PK or TXN ID
                    // So do NOT set: fingpayPrimaryKey / fingpayTxnId

                    aepsMerchantStatusRepository.save(status);
                }
            }


            return aepsBiometricEkycResponseDtoResponseEntity;

        } catch (Exception e) {
            AepsBiometricEkycResponseDto dto = new AepsBiometricEkycResponseDto();
            dto.setStatus(false);
            dto.setMessage("Exception: " + e.getMessage());
            dto.setStatusCode(500);
            dto.setData(null);
            return ResponseEntity.ok(dto);
        }
    }

}
