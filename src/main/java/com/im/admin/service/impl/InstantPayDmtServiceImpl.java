package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.AepsInstantPayMerchantStatusRepository;
import com.im.admin.repositories.RemitterRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.request.*;
import com.im.admin.response.BeneficiaryRegisterResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.service.DmtVendorService;
import com.im.admin.util.InstantpayEncryptionUtil;
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
                    null, null, null
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
                    null, null, null
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
                        bank.setIfscCode((String) bankMap.get("ifscAlias"));

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


        Retailer retailer = retailerRepository
                .findById(request.getRetailerId())
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // ===== STEP 1 : GET MERCHANT (OUTLET ID) =====
        AepsInstantPayMerchantStatus merchant =
                aepsInstantPayMerchantStatusRepository
                        .findByRetailerId(request.getRetailerId()) // adjust if needed
                        .orElseThrow(() ->
                                new RuntimeException("Merchant not onboarded with InstantPay"));

        String outletId = merchant.getOutletId();

        // ===== STEP 2 : URL =====
        String url = credentials.getBaseUrlProd()
                + "/fi/remit/out/domestic/v2/remitterProfile";

        // ===== STEP 3 : HEADERS =====
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("X-Ipay-Auth-Code", "1");
        headers.set("X-Ipay-Client-Id", credentials.getClientId());
        headers.set("X-Ipay-Client-Secret", credentials.getClientSecret());
        headers.set("X-Ipay-Outlet-Id", outletId); // IMPORTANT FIX
        headers.set("X-Ipay-Endpoint-Ip", retailer.getCreatedByIPAddress());

        // ===== STEP 4 : BODY =====
        Map<String, Object> body = new HashMap<>();
        body.put("mobileNumber", request.getMobile());
//        body.put("txnMode", "ALL");
//        body.put("iftEnable", "YES");

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        // ===== STEP 5 : API CALL =====
        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

        Map<String, Object> resp = response.getBody();

        // ===== STEP 6 : VALIDATION =====
        if (resp == null || resp.isEmpty()) {
            throw new RuntimeException("Empty response from InstantPay");
        }

        String statusCode = (String) resp.get("statuscode");
        String status = (String) resp.get("status");

        RemitterQueryResponse finalRes = new RemitterQueryResponse();

        // ===== STEP 7 : SUCCESS =====
        if ("TXN".equalsIgnoreCase(statusCode)
                && "Success".equalsIgnoreCase(status)) {

            Map<String, Object> data =
                    (Map<String, Object>) resp.get("data");

            if (data == null) {
                throw new RuntimeException("Invalid response: data is null");
            }

            RemitterQueryResponse.DataObj dataObj =
                    new RemitterQueryResponse.DataObj();

            dataObj.setMobile((String) data.get("mobileNumber"));

            // ===== LIMIT =====
            Object limit = data.get("limitAvailable");
            if (limit != null) {
                dataObj.setLimit(Double.valueOf(limit.toString()));
            } else {
                dataObj.setLimit(0.0);
            }

            dataObj.setReferenceKey((String) data.get("referenceKey"));

            finalRes.setStatus(true);
            finalRes.setResponse_code(1); // EXISTS
            finalRes.setMessage("Remitter found");
            finalRes.setData(dataObj);

            return finalRes;
        }

// ===== STEP 8 : REMITTER NOT FOUND =====
        // ===== STEP 8 : REMITTER NOT FOUND =====
        else if ("RNF".equalsIgnoreCase(statusCode)) {

            Map<String, Object> data =
                    (Map<String, Object>) resp.get("data");

            String referenceKey = null;

            if (data != null) {
                referenceKey = (String) data.get("referenceKey");
            }

            // ===== SET RESPONSE =====
            RemitterQueryResponse.DataObj dataObj =
                    new RemitterQueryResponse.DataObj();

            dataObj.setReferenceKey(referenceKey); //  IMPORTANT

            finalRes.setStatus(false);
            finalRes.setResponse_code(3); // NOT FOUND
            finalRes.setData(dataObj);
            finalRes.setMessage("Remitter not found, registration required");

            return finalRes;
        }

// ===== STEP 9 : VENDOR ERROR (IMPORTANT FIX) =====
        else {

            finalRes.setStatus(false);
            finalRes.setResponse_code(0); // ERROR
            finalRes.setMessage(
                    status != null ? status : "Vendor error occurred"
            );

            return finalRes;
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

        // ===== STEP 2 : GET REMITTER FROM DB (FOR referenceKey) =====
//        DmtRemitter remitter = remitterRepository
//                .findByMobile(request.getMobile())
//                .orElseThrow(() ->
//                        new RuntimeException("Remitter not found. Call profile API first"));
//
//        String referenceKey = remitter.getReferenceKey();

//        if (referenceKey == null) {
//            throw new RuntimeException("ReferenceKey missing. Call profile API first");
//        }

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
            finalRes.setResponse_code(2); // OTP SENT
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
        bio.put("pidData", request.getBiometricData().getPidData());
        bio.put("ts", request.getBiometricData().getTs());
        bio.put("dc", request.getBiometricData().getDc());
        bio.put("mi", request.getBiometricData().getMi());
        bio.put("dpId", request.getBiometricData().getDpId());
        bio.put("mc", request.getBiometricData().getMc());
        bio.put("rdsId", request.getBiometricData().getRdsId());
        bio.put("rdsVer", request.getBiometricData().getRdsVer());
        bio.put("Skey", request.getBiometricData().getSkey());
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
        body.put("remitterMobileNumber", req.getMobile());
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
        body.put("beneficiaryId", req.getBeneId());
        body.put("otp", req.getOtp());
        body.put("referenceKey", req.getReferenceKey());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        return response.getBody();
    }
}
