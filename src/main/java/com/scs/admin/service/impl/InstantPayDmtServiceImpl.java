package com.scs.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.dto.*;
import com.scs.admin.entities.*;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.repositories.AepsInstantPayMerchantStatusRepository;
import com.scs.admin.repositories.RemitterRepository;
import com.scs.admin.repositories.RetailerRepository;
import com.scs.admin.request.*;
import com.scs.admin.response.BeneficiaryRegisterResponse;
import com.scs.admin.response.PaysprintPennyDropResponse;
import com.scs.admin.response.PaysprintSendOtpResponse;
import com.scs.admin.service.DmtVendorService;
import com.scs.admin.util.InstantpayEncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class InstantPayDmtServiceImpl implements DmtVendorService {

    @Autowired
    private AepsInstantPayMerchantStatusRepository aepsInstantPayMerchantStatusRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RemitterRepository remitterRepository;


    @Override
    public String getVendorCode() {
        return "InstantPay DMT";
    }

    @Override
    public RemitterTransactionResponse processDmt(
            SendDmtRequest req,
            VendorCredentials credentials,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            Double latitude,
            Double longitude) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;

        try {

            String url = credentials.getBaseUrlProd()
                    + "/fi/remit/out/domestic/v2/transaction";

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId()) // adjust if needed
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded with InstantPay"));

            String outletId = merchant.getOutletId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Outlet-Id", outletId);
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

            //  BODY (InstantPay format)
            Map<String, Object> body = new HashMap<>();
            body.put("remitterMobileNumber", req.getMobile());
            body.put("accountNumber", req.getAccountNumber());
            body.put("ifsc", req.getIfscCode());
            body.put("transferMode", req.getTxnType()); // IMPS / NEFT
            body.put("transferAmount", String.valueOf(req.getAmount()));
            body.put("latitude", retailer.getLatitude());
            body.put("longitude", retailer.getLongitude());
            body.put("referenceKey", req.getReferenceKey()); // from OTP step
            body.put("otp", req.getOtp());
            body.put("externalRef", orderId); // your unique txn id

            requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> resp = response.getBody();

            String responseJson = objectMapper.writeValueAsString(resp);

            // LOGGING
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (resp == null) {
                throw new RuntimeException("Empty response from InstantPay");
            }

            String statusCode = (String) resp.get("statuscode");
            String status = (String) resp.get("status");

            RemitterTransactionResponse finalRes = new RemitterTransactionResponse();
            String ipay_uuid = (String) resp.get("ipay_uuid");
            finalRes.setIpay_uuid(ipay_uuid);

            // =========================
            //  SUCCESS
            // =========================
            if ("TXN".equalsIgnoreCase(statusCode)) {

                Map data = (Map) resp.get("data");

                finalRes.setStatus(true);
                finalRes.setResponse_code(1);
                finalRes.setMessage(status);
                finalRes.setTxn_status(1);

                if (data != null) {
                    finalRes.setAckno((String) resp.get("orderid")); // vendor txn id
                    finalRes.setUtr((String) data.get("txnReferenceId"));
                    finalRes.setAccount_number((String) data.get("beneficiaryAccount"));
                    finalRes.setBenename((String) data.get("beneficiaryName"));
                    finalRes.setTxn_amount(((String) data.get("txnValue")));
                    finalRes.setRemitter((String) data.get("remitterMobile"));
                    finalRes.setReferenceid((String) data.get("externalRef"));
                }
            }

            // =========================
            // ⏳ PENDING (timeout case)
            // =========================
            else if ("TUP".equalsIgnoreCase(statusCode) || "PENDING".equalsIgnoreCase(statusCode)) {

                finalRes.setStatus(true);
                finalRes.setResponse_code(2);
                finalRes.setMessage(status);
                finalRes.setTxn_status(2);
            }

            // =========================
            // ❌ FAILURE
            // =========================
            else {

                finalRes.setStatus(false);
                finalRes.setResponse_code(0);
                finalRes.setMessage(status);
                finalRes.setTxn_status(0);
            }


            return finalRes;

        } catch (HttpStatusCodeException e) {

            String errorJson = e.getResponseBodyAsString();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    errorJson,
                    e.getMessage()
            );

            return new RemitterTransactionResponse(
                    false,
                    0,
                    null, null, null, null, null,
                    "Vendor Error",
                    null, null, null, null, null, null, null,
                    null, null, null,null
            );

        } catch (Exception e) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    null,
                    e.getMessage()
            );

            return new RemitterTransactionResponse(
                    false,
                    0,
                    null, null, null, null, null,
                    "Unknown Exception",
                    null, null, null, null, null, null, null,
                    null, null, null,null
            );
        }
    }

    @Override
    public PaysprintSendOtpResponse sendOtp(
            SendDmtRequest req,
            VendorCredentials credentials) throws JsonProcessingException {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;

        try {

            String url = credentials.getBaseUrlProd()
                    + "/fi/remit/out/domestic/v2/generateTransactionOtp";

            Retailer retailer = retailerRepository
                    .findById(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(req.getRetailerId()) // adjust if needed
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded with InstantPay"));

            String outletId = merchant.getOutletId();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Outlet-Id", outletId); // IMPORTANT
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

            // BODY (as per InstantPay)
            Map<String, Object> body = new HashMap<>();
            body.put("remitterMobileNumber", req.getMobile());
            body.put("amount", String.valueOf(req.getAmount()));
            body.put("referenceKey", req.getReferenceKey()); // MUST come from previous step

            requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            ResponseEntity<Map> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> resp = response.getBody();

            String responseJson = objectMapper.writeValueAsString(resp);

            //  LOGGING
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            if (resp == null) {
                throw new RuntimeException("Empty response from InstantPay");
            }

            String statusCode = (String) resp.get("statuscode");
            String status = (String) resp.get("status");

            PaysprintSendOtpResponse finalRes = new PaysprintSendOtpResponse();

            //  OTP SENT
            if ("OTP".equalsIgnoreCase(statusCode)) {

                Map data = (Map) resp.get("data");

                finalRes.setStatus(true);
                finalRes.setResponse_code(2);  // what amaan needs 1 or 2
                finalRes.setMessage(status);

                if (data != null) {
                    finalRes.setReferenceKey((String) data.get("referenceKey"));
                }
            }
            // FAILURE
            else {

                finalRes.setStatus(false);
                finalRes.setResponse_code(0);
                finalRes.setMessage(status);
            }

            return finalRes;

        } catch (HttpStatusCodeException e) {

            String vendorError = e.getResponseBodyAsString();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    vendorError,
                    null
            );

            throw new RuntimeException(vendorError);

        } catch (Exception e) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    null,
                    e.getMessage()
            );

            throw new RuntimeException(e);
        }
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest request, VendorCredentials credentials, String baseUrl) {
        return null;
    }

    @Override
    public String generateJwtToken() {
        return "";
    }

    @Override
    public List<DmtBankList> getdmtbanklist(DmtBankListRequest request,VendorCredentials credentials) {

        try {

            Retailer retailer = retailerRepository
                    .findById(request.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found"));

            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(request.getRetailerId()) // adjust if needed
                            .orElseThrow(() ->
                                    new RuntimeException("Merchant not onboarded with InstantPay"));

            String outletId = merchant.getOutletId();

            // ===== STEP 1: URL =====
            String url = credentials.getBaseUrlProd()
                    + "/fi/remit/out/domestic/v2/banks"; // confirm endpoint


            // ===== STEP 2: HEADERS =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
             headers.set("X-Ipay-Outlet-Id", outletId);
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // ===== STEP 3: API CALL =====
            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Map.class
                    );

            Map<String, Object> resp = response.getBody();

            // ===== STEP 4: VALIDATION =====
            if (resp == null || resp.isEmpty()) {
                throw new RuntimeException("Empty response from InstantPay");
            }

            String statusCode = (String) resp.get("statuscode");
            String status = (String) resp.get("status");

            List<DmtBankList> bankList = new ArrayList<>();

            // ===== STEP 5: SUCCESS =====
            if ("TXN".equalsIgnoreCase(statusCode)
                    && status != null
                    && status.toLowerCase().contains("success")) {

                List<Map<String, Object>> data =
                        (List<Map<String, Object>>) resp.get("data");

                if (data != null) {

                    for (Map<String, Object> bankMap : data) {

                        DmtBankList bank = new DmtBankList();

                        //  bankId
                        Object bankIdObj = bankMap.get("bankId");
                        if (bankIdObj != null) {
                            bank.setBankId(Integer.valueOf(bankIdObj.toString()));
                        }

                        //  bank name
                        bank.setBankName((String) bankMap.get("name"));

                        //  ifscAlias (SHORT CODE)
                        bank.setIfscCode((String) bankMap.get("ifscGlobal"));

                        bankList.add(bank);
                    }
                }
            }

            return bankList;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch bank list from InstantPay", e);
        }
    }

    @Override
    public RemitterQueryResponse checkRemitter(
            RemitterQueryRequest request,
            VendorCredentials credentials) throws Exception {

        try {

            log.info("========== INSTANTPAY CHECK REMITTER START ==========");
            log.info("Retailer Id : {}", request.getRetailerId());
            log.info("Mobile Number : {}", request.getMobile());

            // ===== STEP 1 : GET RETAILER =====
            Retailer retailer = retailerRepository
                    .findById(request.getRetailerId())
                    .orElseThrow(() -> {
                        log.error("Retailer not found : {}", request.getRetailerId());
                        return new RuntimeException("Retailer not found");
                    });

            log.info("Retailer Found");
            log.info("Retailer IP : {}", retailer.getCreatedByIPAddress());

            // ===== STEP 2 : GET MERCHANT =====
            AepsInstantPayMerchantStatus merchant =
                    aepsInstantPayMerchantStatusRepository
                            .findByRetailerId(request.getRetailerId())
                            .orElseThrow(() -> {
                                log.error("Merchant not onboarded for Retailer : {}",
                                        request.getRetailerId());
                                return new RuntimeException(
                                        "Merchant not onboarded with InstantPay");
                            });

            log.info("Merchant Found Successfully");

            String outletId = merchant.getOutletId();

            log.info("Outlet Id : {}", outletId);

            // ===== STEP 3 : URL =====
            String url = credentials.getBaseUrlProd()
                    + "/fi/remit/out/domestic/v2/remitterProfile";

            log.info("InstantPay URL : {}", url);

            // ===== STEP 4 : HEADERS =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-Ipay-Auth-Code", "1");
            headers.set("X-Ipay-Client-Id", credentials.getClientId());
            headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
            headers.set("X-Ipay-Outlet-Id", outletId);
            headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

            log.info("Headers Prepared");
            log.info("Client Id : {}", credentials.getClientId());
            log.info("Outlet Id : {}", outletId);
            log.info("Endpoint IP : {}", retailer.getCreatedByIPAddress());

            // ===== STEP 5 : BODY =====
            Map<String, Object> body = new HashMap<>();
            body.put("mobileNumber", request.getMobile());

            log.info("Request Body : {}", body);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            // ===== STEP 6 : API CALL =====
            log.info("Calling InstantPay Remitter Profile API");

            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            Map.class
                    );

            log.info("HTTP Status : {}", response.getStatusCode());

            Map<String, Object> resp = response.getBody();

            log.info("Raw Vendor Response : {}", resp);

            // ===== STEP 7 : VALIDATION =====
            if (resp == null || resp.isEmpty()) {

                log.error("Empty response received from InstantPay");

                throw new RuntimeException("Empty response from InstantPay");
            }

            String statusCode = (String) resp.get("statuscode");
            String status = (String) resp.get("status");

            log.info("Vendor Status Code : {}", statusCode);
            log.info("Vendor Status : {}", status);

            RemitterQueryResponse finalRes = new RemitterQueryResponse();

            // ===== STEP 8 : SUCCESS =====
            if ("TXN".equalsIgnoreCase(statusCode)
                    && "Success".equalsIgnoreCase(status)) {

                log.info("Remitter Found");

                Map<String, Object> data =
                        (Map<String, Object>) resp.get("data");

                log.info("Data Object : {}", data);

                if (data == null) {

                    log.error("Vendor returned null data object");

                    throw new RuntimeException("Invalid response: data is null");
                }

                RemitterQueryResponse.DataObj dataObj =
                        new RemitterQueryResponse.DataObj();

                dataObj.setMobile((String) data.get("mobileNumber"));

                log.info("Mobile Number : {}",
                        data.get("mobileNumber"));

                // ===== LIMIT =====
                Object limit = data.get("limitAvailable");

                if (limit != null) {

                    dataObj.setLimit(Double.valueOf(limit.toString()));

                    log.info("Available Limit : {}", limit);

                } else {

                    dataObj.setLimit(0.0);

                    log.warn("Limit Available not found in response");
                }

                dataObj.setReferenceKey((String) data.get("referenceKey"));

                log.info("Reference Key : {}",
                        data.get("referenceKey"));

                finalRes.setStatus(true);
                finalRes.setResponse_code(1);
                finalRes.setMessage("Remitter found");
                finalRes.setData(dataObj);

                log.info("Returning SUCCESS response");
                log.info("========== INSTANTPAY CHECK REMITTER END ==========");

                return finalRes;
            }

            // ===== STEP 9 : RNF =====
            else if ("RNF".equalsIgnoreCase(statusCode)) {

                log.info("Remitter Not Found (RNF)");

                Map<String, Object> data =
                        (Map<String, Object>) resp.get("data");

                log.info("RNF Data : {}", data);

                String referenceKey = null;

                if (data != null) {

                    referenceKey = (String) data.get("referenceKey");

                    log.info("RNF Reference Key : {}", referenceKey);
                }

                RemitterQueryResponse.DataObj dataObj =
                        new RemitterQueryResponse.DataObj();

                dataObj.setReferenceKey(referenceKey);

                finalRes.setStatus(false);
                finalRes.setResponse_code(3);
                finalRes.setData(dataObj);
                finalRes.setMessage("Remitter not found, registration required");

                log.info("Returning RNF Response");
                log.info("========== INSTANTPAY CHECK REMITTER END ==========");

                return finalRes;
            }

            // ===== STEP 10 : ERROR =====
            else {

                log.error("Vendor Error Response");
                log.error("Status Code : {}", statusCode);
                log.error("Status : {}", status);

                finalRes.setStatus(false);
                finalRes.setResponse_code(0);
                finalRes.setMessage(
                        status != null
                                ? status
                                : "Vendor error occurred"
                );

                log.info("Returning Vendor Error Response");
                log.info("========== INSTANTPAY CHECK REMITTER END ==========");

                return finalRes;
            }

        } catch (Exception ex) {

            log.error(
                    "[INSTANTPAY CHECK REMITTER] Exception | Mobile={}",
                    request.getMobile(),
                    ex
            );

            throw ex;
        }
    }

    @Override
    public RemitterRegisterResponse registerRemitter(
            RemitterRegisterRequest request,
            VendorCredentials credentials) throws Exception {

        // ===== STEP 1 : GET MERCHANT (OUTLET ID) =====
        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(request.getRetailerId())
                        .orElseThrow(() ->
                                new RuntimeException("Merchant not onboarded with InstantPay"));

        String outletId = merchant.getOutletId();

        Retailer retailer = retailerRepository
                .findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // ===== STEP 3 : URL =====
        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/remitterRegistration";

        // ===== STEP 4 : HEADERS =====
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", outletId);
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        // ===== STEP 5 : ENCRYPT AADHAAR =====
        String encryptedAadhaar =
                InstantpayEncryptionUtil.encryptAadhaar(
                        request.getAadhaarNumber(),
                        credentials.getSaltKey()
                );

        // ===== STEP 6 : BODY =====
        Map<String, Object> body = new HashMap<>();
        body.put("mobileNumber", request.getMobile());
        body.put("encryptedAadhaar", encryptedAadhaar);
        body.put("referenceKey", request.getReferenceKey());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        // ===== STEP 7 : API CALL =====
        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

        Map<String, Object> resp = response.getBody();

        if (resp == null || resp.isEmpty()) {
            throw new RuntimeException("Empty response from InstantPay");
        }

        String statusCode = (String) resp.get("statuscode");
        String status = (String) resp.get("status");

        RemitterRegisterResponse finalRes = new RemitterRegisterResponse();

        // ===== STEP 8 : OTP SENT =====
        if ("OTP".equalsIgnoreCase(statusCode)) {

            Map<String, Object> data =
                    (Map<String, Object>) resp.get("data");

            String otpRef = data != null
                    ? (String) data.get("otpReference")
                    : null;

            finalRes.setStatus(true);
            finalRes.setResponse_code(2); // OTP SENT  //3 amaaan
            finalRes.setMessage(status);

            RemitterRegisterResponse.DataObj dataObj =
                    new RemitterRegisterResponse.DataObj();

            dataObj.setOtpReference(otpRef);

            finalRes.setData(dataObj);

            return finalRes;
        }

        // ===== FAILURE =====
        finalRes.setStatus(false);
        finalRes.setResponse_code(0);
        finalRes.setMessage(status != null ? status : "Registration failed");

        return finalRes;
    }

    @Override
    public Map doEkyc(
            RemitterEkycRequest request,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(request.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));

        Retailer retailer = retailerRepository
                .findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        String externalRef = "DmtInstantPay-" + request.getRetailerId() + "-" + System.currentTimeMillis();


        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/remitterKyc";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", merchant.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        Map<String, Object> body = new HashMap<>();

        body.put("mobileNumber", request.getMobile());
        body.put("referenceKey", request.getReferenceKey());
        body.put("latitude", request.getLat());
        body.put("longitude", request.getLongitude());
        body.put("externalRef",externalRef);
        body.put("consentTaken", "Y");
        body.put("captureType", "FINGER");

        Map<String, Object> bio = new HashMap<>();

        bio.put("ci", request.getBiometricData().getCi());
        bio.put("hmac", request.getBiometricData().getHmac());
        bio.put("pidData", request.getBiometricData().getPiddata());
        bio.put("ts", request.getBiometricData().getTs());
        bio.put("dc", request.getBiometricData().getDc());
        bio.put("mi", request.getBiometricData().getMi());
        bio.put("dpId", request.getBiometricData().getDpID());
        bio.put("mc", request.getBiometricData().getMc());
        bio.put("rdsId", request.getBiometricData().getRdsID());
        bio.put("rdsVer", request.getBiometricData().getRdsVer());
        bio.put("Skey", request.getBiometricData().getSessionKey());
        bio.put("srno", request.getBiometricData().getSrno());

        body.put("biometricData", bio);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        return response.getBody();
    }

    @Override
    public BeneficiaryRegisterResponse registerBeneficiary(
            BeneficiaryRegisterRequest req,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(req.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));

        Retailer retailer = retailerRepository
                .findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));


        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/beneficiaryRegistration";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", merchant.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        Map<String, Object> body = new HashMap<>();
        body.put("remitterMobileNumber", req.getMobile());
        body.put("beneficiaryMobileNumber", req.getBenemobile());
        body.put("accountNumber", req.getAccno());
        body.put("ifsc", req.getIfsccode());
        body.put("name", req.getBenename());
        body.put("bankId", req.getBankid());


        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> resp = response.getBody();

        BeneficiaryRegisterResponse finalRes = new BeneficiaryRegisterResponse();

        if (resp == null) {
            throw new RuntimeException("Empty response from InstantPay");
        }

        String statusCode = (String) resp.get("statuscode");
        String status = (String) resp.get("status");

        // =========================
        // OTP CASE
        // =========================
        if ("OTP".equalsIgnoreCase(statusCode)) {

            Map<String, Object> data = (Map<String, Object>) resp.get("data");

            BeneficiaryRegisterResponse.Data d =
                    new BeneficiaryRegisterResponse.Data();

            //  Convert properly to String
            String beneId = data.get("beneficiaryId") != null
                    ? data.get("beneficiaryId").toString()
                    : null;

            d.setBene_id(beneId);

            // optional (keep both if needed)
            d.setBeneficiaryId(beneId);

            d.setReferenceKey((String) data.get("referenceKey"));
            d.setValidity((String) data.get("validity"));

            finalRes.setStatus(true);
            finalRes.setResponse_code(2);   // OTP
            finalRes.setMessage(status);
            finalRes.setData(d);
        }
        // =========================
        // DIRECT SUCCESS (rare)
        // =========================
        else if ("TXN".equalsIgnoreCase(statusCode)) {

            finalRes.setStatus(true);
            finalRes.setResponse_code(1);
            finalRes.setMessage(status);
        }

        // =========================
        //  FAILURE
        // =========================
        else {

            finalRes.setStatus(false);
            finalRes.setResponse_code(0);
            finalRes.setMessage(status);
        }

        return finalRes;
    }
    @Override
    public DmtBeneficiaryDeleteResponse deleteBeneficiary(
            DmtBeneficiaryDeleteRequest req,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(req.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));

        Retailer retailer = retailerRepository
                .findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/beneficiaryDelete";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", merchant.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        Map<String, Object> body = new HashMap<>();
        body.put("remitterMobileNumber", req.getMobile());
        body.put("beneficiaryId", req.getBene_id());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> resp = response.getBody();

        DmtBeneficiaryDeleteResponse finalRes = new DmtBeneficiaryDeleteResponse();

        if (resp == null) {
            throw new RuntimeException("Empty response from InstantPay");
        }

        String statusCode = (String) resp.get("statuscode");
        String status = (String) resp.get("status");

        //  OTP CASE
        if ("OTP".equalsIgnoreCase(statusCode)) {

            Map data = (Map) resp.get("data");

            finalRes.setResponse_code(2);
            finalRes.setStatus(true);
            finalRes.setMessage(status);

            if (data != null) {
                finalRes.setReferenceKey((String) data.get("referenceKey"));
            }
        }
        //  SUCCESS
        else if ("TXN".equalsIgnoreCase(statusCode)) {

            finalRes.setResponse_code(1);
            finalRes.setStatus(true);
            finalRes.setMessage(status);
        }
        //  FAILURE
        else {

            finalRes.setResponse_code(0);
            finalRes.setStatus(false);
            finalRes.setMessage(status);
        }

        return finalRes;
    }

    @Override
    public DmtBeneficiaryFetchResponse fetchBeneficiary(DmtBeneficiaryFetchRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public DmtBeneficiaryFetchByBeneIdResponse fetchBeneficiaryByBeneId(DmtBeneficiaryFetchByBeneIdRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Override
    public RemitterRegisterResponse verifyRemitterOtp(
            RemitterEkycRequest request,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(request.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));

        Retailer retailer = retailerRepository
                .findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/remitterRegistrationVerify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", merchant.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        Map<String, Object> body = new HashMap<>();
        body.put("mobileNumber", request.getMobile());
        body.put("otp", request.getOtp());
        body.put("referenceKey", request.getReferenceKey());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        Map<String, Object> resp = response.getBody();

        RemitterRegisterResponse res = new RemitterRegisterResponse();

        if (resp != null && "KYC".equalsIgnoreCase((String) resp.get("statuscode"))) {
            res.setStatus(true);
            res.setResponse_code(1);
            res.setMessage((String) resp.get("status"));
        } else {
            res.setStatus(false);
            res.setResponse_code(0);
            res.setMessage((String) resp.get("status"));
        }

        return res;
    }

    @Override
    public Map verifyBeneficiaryOtp(
            VerifyBeneOtpRequest req,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(req.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));

        Retailer retailer = retailerRepository
                .findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/beneficiaryRegistrationVerify";


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", req.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());
        headers.set("X-Ipay-Outlet-Id", merchant.getOutletId());

        // CORRECT BODY
        Map<String, Object> body = new HashMap<>();
        body.put("remitterMobileNumber", req.getRemitterMobile());
        body.put("otp", req.getOtp());
        body.put("referenceKey", req.getReferenceKey());
        body.put("beneficiaryId", req.getBeneId());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        return response.getBody();
    }

    @Override
    public Map verifyDeleteOtp(
            VerifyDeleteOtpRequest req,
            VendorCredentials credentials) throws Exception {

        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(req.getRetailerId())
                        .orElseThrow(() -> new RuntimeException("Merchant not onboarded"));


        Retailer retailer = retailerRepository
                .findById(req.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/beneficiaryDeleteVerify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id",merchant.getOutletId());
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        //  BODY
        Map<String, Object> body = new HashMap<>();
        body.put("remitterMobileNumber", req.getMobile());
        body.put("beneficiaryId", req.getBene_id());
        body.put("otp", req.getOtp());
        body.put("referenceKey", req.getReferenceKey());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        return response.getBody();
    }
}
