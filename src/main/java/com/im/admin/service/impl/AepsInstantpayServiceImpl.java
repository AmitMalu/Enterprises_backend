package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.InstantPayMerchantRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.Instantpay2FaRequestDto;
import com.im.admin.request.InstantpayBiometricKycRequestDto;
import com.im.admin.request.InstantpayKycStatusRequestDto;
import com.im.admin.response.*;
import com.im.admin.service.AepsInstantpayService;
import com.im.admin.service.AepsMerchantService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.InstantpayEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AepsInstantpayServiceImpl implements AepsInstantpayService {


    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private AepsMerchantStatusRepository aepsMerchantStatusRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private AepsOutletLoginLogRepository aepsOutletLoginLogRepository;


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



    @Override
    public CommonResponse<InstantpayMerchantResponseDto> onboardMerchant(
            InstantPayMerchantRequestDto req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            // ===== STEP 1 : GET VENDOR USING SUB PRODUCT =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            // ===== STEP 2 : GET VENDOR CREDENTIALS =====
            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String baseUrl = credentials.getBaseUrlProd();
            String clientId = credentials.getClientId();
            String clientSecret = credentials.getClientSecret();
            String saltKey = credentials.getSaltKey();

            // ===== STEP 3 : GET RETAILER =====
            Optional<Retailer> opt = retailerRepository.findById(req.getRetailerId());

            if (opt.isEmpty()) {
                return CommonResponse.error(
                        400,
                        "Invalid retailerId: Retailer not found"
                );
            }

            Retailer r = opt.get();

            String url = baseUrl + "/user/outlet/signup/minKyc";

            // ===== STEP 4 : ENCRYPT AADHAAR =====
            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            r.getAadhaarNumber(),
                            saltKey
                    );

            // ===== STEP 5 : CREATE REQUEST BODY =====
            Map<String, Object> body = new LinkedHashMap<>();

            body.put("mobile", r.getMobileNumber());
            body.put("name", r.getName());
            body.put("gender", "M");
            body.put("pan", r.getPanNumber());
            body.put("email", r.getEmailId());

            Map<String, Object> address = new LinkedHashMap<>();
            address.put("full", r.getAddress().getAddress());
            address.put("city", r.getAddress().getCity());
            address.put("pincode", r.getAddress().getZipCode());

            body.put("address", address);
            body.put("aadhaar", encryptedAadhaar);
            body.put("dateOfBirth", r.getDob().toString());
            body.put("latitude", r.getLatitude());
            body.put("longitude", r.getLongitude());

            ObjectMapper mapper = new ObjectMapper();
            requestJson = mapper.writeValueAsString(body);

            // ===== STEP 6 : HEADERS =====
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", clientId);
            headers.set("X-Ipay-Client-Secret", clientSecret);
            headers.set("X-Ipay-Endpoint-Ip", r.getCreatedByIPAddress());

            HttpEntity<String> entity =
                    new HttpEntity<>(requestJson, headers);

            // ===== STEP 7 : CALL INSTANTPAY API =====
            ResponseEntity<InstantpayMerchantResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayMerchantResponseDto.class
                    );

            InstantpayMerchantResponseDto vendorRes = response.getBody();

            responseJson = mapper.writeValueAsString(vendorRes);

            // ===== STEP 8 : SAVE VENDOR LOGS =====
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            //  CHECK EXISTING MERCHANT =====
            Optional<AepsMerchantStatus> existing =
                    aepsMerchantStatusRepository.findByRetailerId(r.getRetailerId());

            if (existing.isPresent()) {

                AepsMerchantStatus status = existing.get();

                if (vendorRes.getData() != null) {
                    status.setMerchantLoginId(
                            String.valueOf(vendorRes.getData().getOutletId())
                    );
                }

                status.setUpdatedAt(LocalDateTime.now());
                status.setOnboarded(true);
                status.setVendor(vendor);

                aepsMerchantStatusRepository.save(status);

                return new CommonResponse<>(
                        200,
                        "Merchant updated successfully",
                        vendorRes
                );
            }

            // ===== STEP 10 : SUCCESS RESPONSE =====
            if (vendorRes != null &&
                    "TXN".equalsIgnoreCase(vendorRes.getStatuscode())) {

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
                        vendorRes
                );
            }

            // ===== STEP 11 : FAILURE =====
            return new CommonResponse<>(
                    400,
                    "Vendor onboarding failed",
                    vendorRes
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
    public CommonResponse<InstantpayKycStatusResponseDto> checkKycStatus(
            InstantpayKycStatusRequestDto req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            // GET VENDOR =====
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String baseUrl = credentials.getBaseUrlProd();
            String clientId = credentials.getClientId();
            String clientSecret = credentials.getClientSecret();

            //  GET MERCHANT =====
            AepsMerchantStatus merchant =
                    aepsMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            String outletId = merchant.getMerchantLoginId();

            String url = baseUrl + "/user/outlet/signup/biometricKycStatus";

            //  BODY
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("spkey", "WAP"); // AEPS

            ObjectMapper mapper = new ObjectMapper();
            requestJson = mapper.writeValueAsString(body);

            // HEADERS
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", clientId);
            headers.set("X-Ipay-Client-Secret", clientSecret);
            headers.set("X-Ipay-Endpoint-Ip", "127.0.0.1");
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity =
                    new HttpEntity<>(requestJson, headers);

            //  CALL API
            ResponseEntity<InstantpayKycStatusResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayKycStatusResponseDto.class
                    );

            InstantpayKycStatusResponseDto vendorRes = response.getBody();

            responseJson = mapper.writeValueAsString(vendorRes);

            // SAVE VENDOR LOGS
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (vendorRes == null || vendorRes.getData() == null) {
                return CommonResponse.error(400, "Invalid vendor response");
            }

            String action = vendorRes.getData().getAction();
            String referenceKey = vendorRes.getData().getReferenceKey();


            merchant.setReferenceKey(referenceKey);
            merchant.setUpdatedAt(LocalDateTime.now());

            aepsMerchantStatusRepository.save(merchant);

            //  IF NO ACTION REQUIRED
            if ("NO-ACTION-REQUIRED".equalsIgnoreCase(action)) {

                Optional<AepsMerchantStatus> opt =
                        aepsMerchantStatusRepository
                                .findByRetailerId(req.getRetailerId());

                if (opt.isPresent()) {

                    AepsMerchantStatus status = opt.get();

                    status.setEkycVerified(true);
                    status.setBiometricVerified(true);
                    status.setUpdatedAt(LocalDateTime.now());
                    status.setStatus("ACTIVE");

                    aepsMerchantStatusRepository.save(status);
                }

                return new CommonResponse<>(
                        200,
                        "Merchant KYC already completed",
                        vendorRes
                );
            }

            // ACTION REQUIRED
            if ("ACTION-REQUIRED".equalsIgnoreCase(action)) {

                return new CommonResponse<>(
                        200,
                        "Biometric KYC required",
                        vendorRes
                );
            }

            return new CommonResponse<>(
                    200,
                    "Status fetched",
                    vendorRes
            );

        } catch (Exception e) {

            return CommonResponse.error(
                    500,
                    "Exception : " + e.getMessage()
            );
        }
    }

    @Override
    public CommonResponse<InstantpayBiometricKycResponseDto> biometricKyc(
            InstantpayBiometricKycRequestDto req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            // GET VENDOR
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String baseUrl = credentials.getBaseUrlProd();
            String clientId = credentials.getClientId();
            String clientSecret = credentials.getClientSecret();
            String saltKey = credentials.getSaltKey();
            String externalRef = "AEPS-" + req.getRetailerId() + "-" + System.currentTimeMillis();

            // GET MERCHANT
            AepsMerchantStatus merchant =
                    aepsMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            String outletId = merchant.getMerchantLoginId();
            String referenceKey = merchant.getReferenceKey();

            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            req.getAadhaarNumber(),
                            saltKey
                    );


            String url = baseUrl + "/user/outlet/signup/biometricKyc";

            // BUILD BODY
            Map<String, Object> body = new LinkedHashMap<>();

            body.put("referenceKey", referenceKey);
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("externalRef", externalRef);

            Map<String, Object> bio = new LinkedHashMap<>();

            bio.put("encryptedAadhaar", encryptedAadhaar);
            bio.put("dc", req.getBiometricData().getDc());
            bio.put("ci", req.getBiometricData().getCi());
            bio.put("hmac", req.getBiometricData().getHmac());
            bio.put("dpId", req.getBiometricData().getDpId());
            bio.put("mc", req.getBiometricData().getMc());
            bio.put("pidDataType", req.getBiometricData().getPidDataType());
            bio.put("rdsId", req.getBiometricData().getRdsId());
            bio.put("sessionKey", req.getBiometricData().getSessionKey());
            bio.put("mi", req.getBiometricData().getMi());
            bio.put("errCode", req.getBiometricData().getErrCode());
            bio.put("errInfo", req.getBiometricData().getErrInfo());
            bio.put("fCount", req.getBiometricData().getFCount());
            bio.put("fType", req.getBiometricData().getFType());
            bio.put("iCount", req.getBiometricData().getICount());
            bio.put("iType", req.getBiometricData().getIType());
            bio.put("pCount", req.getBiometricData().getPCount());
            bio.put("pType", req.getBiometricData().getPType());
            bio.put("srno", req.getBiometricData().getSrno());
            bio.put("sysid", req.getBiometricData().getSysid());
            bio.put("ts", req.getBiometricData().getTs());
            bio.put("pidData", req.getBiometricData().getPidData());
            bio.put("qScore", req.getBiometricData().getQScore());
            bio.put("nmPoints", req.getBiometricData().getNmPoints());
            bio.put("rdsVer", req.getBiometricData().getRdsVer());

            body.put("biometricData", bio);

            ObjectMapper mapper = new ObjectMapper();
            requestJson = mapper.writeValueAsString(body);

            // HEADERS
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", clientId);
            headers.set("X-Ipay-Client-Secret", clientSecret);
            headers.set("X-Ipay-Endpoint-Ip", "127.0.0.1");
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // CALL API
            ResponseEntity<InstantpayBiometricKycResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            InstantpayBiometricKycResponseDto.class
                    );

            InstantpayBiometricKycResponseDto vendorRes = response.getBody();

            responseJson = mapper.writeValueAsString(vendorRes);

            // SAVE LOGS
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (vendorRes == null) {
                return CommonResponse.error(400, "Invalid vendor response");
            }

            return new CommonResponse<>(
                    200,
                    "Biometric KYC API called successfully",
                    vendorRes
            );

        } catch (Exception e) {

            return CommonResponse.error(
                    500,
                    "Exception : " + e.getMessage()
            );
        }
    }


    @Override
    public CommonResponse<Instantpay2FaResponseDto> outletLoginStatus(Instantpay2FaRequestDto req) {

        try {

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String baseUrl = credentials.getBaseUrlProd();

            AepsMerchantStatus merchant =
                    aepsMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            String outletId = merchant.getMerchantLoginId();

            String url = baseUrl + "/fi/aeps/outletLoginStatus";

            Map<String, Object> body = new HashMap<>();
            body.put("type", "CashDeposit");

            ObjectMapper mapper = new ObjectMapper();
            String requestJson = mapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", "127.0.0.1");
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Instantpay2FaResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Instantpay2FaResponseDto.class
                    );

            Instantpay2FaResponseDto vendorRes = response.getBody();

            return new CommonResponse<>(
                    200,
                    "Outlet Login Status fetched successfully",
                    vendorRes
            );

        } catch (Exception e) {

            return CommonResponse.error(
                    500,
                    "Error : " + e.getMessage()
            );
        }

    }

    @Override
    public CommonResponse<Instantpay2FaResponseDto> outletLogin(Instantpay2FaRequestDto req) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            String baseUrl = credentials.getBaseUrlProd();
            String saltKey = credentials.getSaltKey();

            AepsMerchantStatus merchant =
                    aepsMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            String outletId = merchant.getMerchantLoginId();

            String encryptedAadhaar =
                    InstantpayEncryptionUtil.encryptAadhaar(
                            req.getAadhaarNumber(),
                            saltKey
                    );

            String externalRef = "AEPS-" + req.getRetailerId()
                    + "-" + System.currentTimeMillis();

            String url = baseUrl + "/fi/aeps/outletLogin";

            Map<String, Object> body = new LinkedHashMap<>();

            body.put("type", "DAILY_LOGIN");
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("externalRef", externalRef);
            body.put("captureType", "FINGER");

            Map<String, Object> bio = new LinkedHashMap<>();

            bio.put("encryptedAadhaar", encryptedAadhaar);
            bio.put("dc", req.getBiometricData().getDc());
            bio.put("ci", req.getBiometricData().getCi());
            bio.put("hmac", req.getBiometricData().getHmac());
            bio.put("dpId", req.getBiometricData().getDpId());
            bio.put("mc", req.getBiometricData().getMc());
            bio.put("pidDataType", req.getBiometricData().getPidDataType());
            bio.put("sessionKey", req.getBiometricData().getSessionKey());
            bio.put("mi", req.getBiometricData().getMi());
            bio.put("rdsId", req.getBiometricData().getRdsId());
            bio.put("errCode", req.getBiometricData().getErrCode());
            bio.put("errInfo", req.getBiometricData().getErrInfo());
            bio.put("fCount", req.getBiometricData().getFCount());
            bio.put("fType", req.getBiometricData().getFType());
            bio.put("iCount", req.getBiometricData().getICount());
            bio.put("iType", req.getBiometricData().getIType());
            bio.put("pCount", req.getBiometricData().getPCount());
            bio.put("pType", req.getBiometricData().getPType());
            bio.put("srno", req.getBiometricData().getSrno());
            bio.put("sysid", req.getBiometricData().getSysid());
            bio.put("ts", req.getBiometricData().getTs());
            bio.put("pidData", req.getBiometricData().getPidData());
            bio.put("qScore", req.getBiometricData().getQScore());
            bio.put("nmPoints", req.getBiometricData().getNmPoints());
            bio.put("rdsVer", req.getBiometricData().getRdsVer());

            body.put("biometricData", bio);

            ObjectMapper mapper = new ObjectMapper();
            requestJson = mapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", "127.0.0.1");
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<Instantpay2FaResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Instantpay2FaResponseDto.class
                    );

            Instantpay2FaResponseDto vendorRes = response.getBody();

            responseJson = mapper.writeValueAsString(vendorRes);

            // ==============================
            // SAVE BUSINESS LOG IN DB
            // ==============================

            AepsOutletLoginLog log = new AepsOutletLoginLog();

            log.setRetailerId(req.getRetailerId());
            log.setVendorId(vendor.getId());
            log.setOutletId(outletId);
            log.setExternalRef(externalRef);

            if (vendorRes != null) {
                log.setStatuscode(vendorRes.getStatuscode());
                log.setActcode(vendorRes.getActcode());
                log.setMessage(vendorRes.getStatus());
            }

            log.setCreatedAt(LocalDateTime.now());

            aepsOutletLoginLogRepository.save(log);

            // ==============================
            // SAVE VENDOR LOGS
            // ==============================

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendor.getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return new CommonResponse<>(
                    200,
                    "Outlet Login 2FA API called successfully",
                    vendorRes
            );

        } catch (Exception e) {

            return CommonResponse.error(
                    500,
                    "Exception : " + e.getMessage()
            );
        }
    }

    @Override
    public CommonResponse<InstantpayBankListResponseDto> getBanks(InstantPayMerchantRequestDto req) {

        try {

            // GET VENDOR ROUTING
            VendorRouting vendorRouting =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());


            Vendors vendor = CommonMethods.getVendors(vendorRouting);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(
                                    vendor.getId(),
                                    vendor.getProduct().getId())
                            .orElseThrow(() ->
                                    new RuntimeException("Vendor credentials not found"));

            // GET MERCHANT
            AepsMerchantStatus merchant =
                    aepsMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId())
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded"));

            String outletId = merchant.getMerchantLoginId();

            String url = credentials.getBaseUrlProd() + "/fi/aeps/banks";

            // HEADERS
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Endpoint-Ip", "127.0.0.1");
            headers.set("X-Ipay-Outlet-Id", outletId);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<InstantpayBankListResponseDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            InstantpayBankListResponseDto.class
                    );

            InstantpayBankListResponseDto vendorResp = response.getBody();

            if (vendorResp == null) {
                return new CommonResponse<>(500, "Vendor returned empty response", null);
            }

            return CommonResponse.success(vendorResp, "Banks fetched successfully");

        } catch (Exception e) {

            log.error("InstantPay Bank List Error", e);

            return new CommonResponse<>(
                    500,
                    "Exception: " + e.getMessage(),
                    null
            );
        }
    }

}




