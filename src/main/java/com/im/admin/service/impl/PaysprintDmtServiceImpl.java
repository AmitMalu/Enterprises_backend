package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.DmtBankList;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.repositories.DmtBankListRepository;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.DmtVendorService;
import com.im.admin.service.NetworkBankService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@Slf4j
@Service
//@RequiredArgsConstructor
public class PaysprintDmtServiceImpl implements DmtVendorService {
    @Override
    public Map verifyDeleteOtp(VerifyDeleteOtpRequest req, VendorCredentials credentials) throws Exception {
        return Map.of();
    }

    @Override
    public Map verifyBeneficiaryOtp(VerifyBeneOtpRequest req, VendorCredentials credentials) throws Exception {
        return Map.of();
    }

    @Override
    public RemitterRegisterResponse verifyRemitterOtp(RemitterEkycRequest request, VendorCredentials credentials) throws Exception {
        return null;
    }

    @Autowired
    private RestTemplate restTemplate;

//    @Autowired
//    private BankService bankService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NetworkBankService bankService;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private DmtBankListRepository dmtBankListRepository;

    @Override
    public String getVendorCode() {
        return "Paysprint DMT";
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest req,
                                                      VendorCredentials credentials,
                                                      String prodUrl) {
        try {

            //uat
//            String url = baseUrl +
//                    "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/benenameverify";

            //live
            String url = prodUrl +
                    "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/benenameverify";

            //  Generate token dynamically
            String token = generateJwtToken();

            // ------------------------------------------------------
            //  Build JSON body using Map (as per Paysprint Docs)
            // ------------------------------------------------------
            Map<String, Object> body = new HashMap<>();
            body.put("mobile", req.getMobile());
            body.put("accno", req.getAccno());
            body.put("bankid", req.getBankid());
            body.put("benename", req.getBenename());
            body.put("referenceid", req.getReferenceid());
            body.put("pincode", req.getPincode());
            body.put("address", req.getAddress());
            body.put("dob", req.getDob());
            body.put("gst_state", req.getGst_state());
            body.put("bene_id", req.getBene_id());
            body.put("lat", req.getLat());
            body.put("long", req.getLng());   // IMPORTANT

            String bodyJson = objectMapper.writeValueAsString(body);
            log.info("[PAYSPRINT] PennyDrop Body: {}", bodyJson);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, getHeaders(credentials,token));

            ResponseEntity<PaysprintPennyDropResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaysprintPennyDropResponse.class
            );

            return response.getBody() != null
                    ? response.getBody()
                    : PaysprintPennyDropResponse.builder()
                    .status(false)
                    .response_code(0)
                    .message("Empty response from vendor")
                    .build();

        } catch (Exception e) {
            log.error("[PAYSPRINT] Penny Drop Exception: {}", e.getMessage(), e);

            return PaysprintPennyDropResponse.builder()
                    .status(false)
                    .response_code(500)
                    .message("Exception: " + e.getMessage())
                    .build();
        }
    }



    @Override
    public PaysprintSendOtpResponse sendOtp(SendDmtRequest req, VendorCredentials credentials) throws JsonProcessingException {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        try {
            //uat
//            String sendOtpUrl = credentials.getBaseUrlUat() +
//                    "/service-api/api/v1/service/dmt/kyc/transact/transact/send_otp";

            //live
            String sendOtpUrl = credentials.getBaseUrlProd() +
                    "/api/v1/service/dmt/kyc/transact/transact/send_otp";

            //  Generate token dynamically
             String token = generateJwtToken();

            // -------------------------
            // 1. Prepare Request Body Using Map
            // -------------------------
            Map<String, String> body = new HashMap<>();
            body.put("mobile", req.getMobile());
            body.put("bene_id", req.getBeneId());
            body.put("txntype", req.getTxnType());
            body.put("amount", String.valueOf(req.getAmount()));
            body.put("pincode", req.getPincode());
            body.put("address", req.getAddress());
            body.put("dob", req.getDob());
            body.put("gst_state", req.getGstState());
            body.put("lat", req.getLat());
            body.put("long", req.getLng());    // IMPORTANT: key name EXACT "long"
            body.put("referenceid", req.getReferenceId());

            // Store request JSON (optional, for logs)
             requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, String>> requestEntity =
                    new HttpEntity<>(body, getHeaders(credentials,token));

            // -------------------------
            // 2. Call Paysprint API
            // -------------------------
            ResponseEntity<PaysprintSendOtpResponse> response = restTemplate.exchange(
                    sendOtpUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PaysprintSendOtpResponse.class
            );

            PaysprintSendOtpResponse resp = response.getBody();

            String responseJson = objectMapper.writeValueAsString(response.getBody());

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

          //old
//            return response.getBody();
            //new
            if (resp != null) {

                if (resp.isStatus()) {
                    resp.setResponse_code(1);   //  Paysprint SUCCESS
                } else {
                    resp.setResponse_code(0);   //  FAILURE
                }
            }
            return resp;

        }
        catch (HttpStatusCodeException e) {

            String vendorErrorBody = e.getResponseBodyAsString();


            if (vendorErrorBody == null || vendorErrorBody.isBlank()) {
                vendorErrorBody = e.getStatusCode() + " : " + e.getStatusText();
            }

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    vendorErrorBody,
                    null
            );

            throw new RuntimeException(vendorErrorBody);
        }
        catch (Exception e) {

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    null,
                    e.getMessage()
            );

            throw e;
        }
    }

    @Override
    public RemitterTransactionResponse processDmt(
            SendDmtRequest req,
            VendorCredentials credentials,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            Double latitude, Double longitude) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            //  Generate token dynamically
               String token = generateJwtToken();

            // 1. Headers
            HttpHeaders headers = getHeaders(credentials,token);

            // 2. Body
            Map<String, Object> body = new HashMap<>();
            body.put("mobile", req.getMobile());
            body.put("referenceid", req.getReferenceId());
            body.put("bene_id", req.getBeneId());
            body.put("txntype", req.getTxnType());
            body.put("amount", String.valueOf(req.getAmount()));
            body.put("pincode", req.getPincode());
            body.put("address", req.getAddress());
            body.put("dob", req.getDob());
            body.put("gst_state", req.getGstState());
            body.put("lat", req.getLat());
            body.put("long", req.getLng());
            body.put("otp", req.getOtp());
            body.put("stateresp", req.getStateresp());


             requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, Object>> txnEntity = new HttpEntity<>(body, headers);

            //uat
//            String txnUrl = credentials.getBaseUrlUat()
//                    + "/service-api/api/v1/service/dmt/kyc/transact/transact";

            //live
            String txnUrl = credentials.getBaseUrlProd()
                    + "/api/v1/service/dmt/kyc/transact/transact";

            log.info("[PAYSPRINT] Performing Transaction → URL: {} | Body: {}", txnUrl, body);

            // 3. Call Vendor API
            ResponseEntity<RemitterTransactionResponse> txnResponseEntity =
                    restTemplate.exchange(
                            txnUrl,
                            HttpMethod.POST,
                            txnEntity,
                            RemitterTransactionResponse.class
                    );

            RemitterTransactionResponse txnResponse = txnResponseEntity.getBody();
            log.info("[PAYSPRINT] Vendor SUCCESS Response: {}", txnResponse);

             responseJson = objectMapper.writeValueAsString(txnResponse);

            //  SUCCESS VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return txnResponse;

        } catch (HttpClientErrorException e) {

            String errorJson = e.getResponseBodyAsString();
            log.error("[PAYSPRINT] TXN ERROR | orderId={} | response={}",
                    orderId, errorJson);

            //  FAILURE VENDOR LOG
            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    errorJson,
                    e.getMessage()
            );

            try {
                return objectMapper.readValue(
                        errorJson,
                        RemitterTransactionResponse.class
                );
            } catch (Exception ex) {
                return new RemitterTransactionResponse(
                        false,
                        0,
                        null, null, null, null, null,
                        "Vendor error parse failed",
                        null, null, null, null, null, null, null,
                        null, null, null,null
                );
            }

        } catch (Exception e) {

            log.error("[PAYSPRINT] TXN UNKNOWN ERROR | orderId={}", orderId, e);

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
                    "Unknown exception",
                    null, null, null, null, null, null, null,
                    null, null, null,null
            );
        }
    }



    // 🔹 Helper method for common headers
    private HttpHeaders getHeaders(VendorCredentials credentials,String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
//        headers.set("Token", credentials.getToken());
        headers.set("Token", token);
        headers.set("Authorisedkey", credentials.getSaltKey());
        return headers;
    }

    public String generateJwtToken() {

        int random = getRandom();
        long timestamp = getTimestamp();

        String jwtKey =
                "UFMwMDY0MDQ5YTk5MTYxZmI4YjgxMjYyNzBlODY2NDAxMTg3NzIxMjE3NTczMzkxMDA=";

        System.out.println("JWT timestamp = " + timestamp);
        System.out.println("JWT reqid     = " + random);

        String token = Jwts.builder()
                .setIssuer("PSPRINT")
//                .setHeaderParam("typ", "JWT")
//                .setHeaderParam("alg", "HS256")
                .claim("timestamp", timestamp)
                .claim("iat", timestamp - 720)
                .claim("partnerId", "PS006404")
                .claim("product","WALLET")
                .claim("reqid", random)
                .signWith(SignatureAlgorithm.HS256,
                        jwtKey.getBytes(StandardCharsets.UTF_8))
                .compact();

        return token;
    }

    private static int getRandom() {
        int random = (int) Math.round(Math.random() * 1000000000);
        return random;
    }

    private static long getTimestamp() {
        long timestamp = Instant.now().getEpochSecond();
        return timestamp;
    }



    @Override
    public List<DmtBankList> getdmtbanklist(DmtBankListRequest request,
                                            VendorCredentials credentials) {

        List<NetworkBankMasterResponseDto> networkBanks =
                bankService.getAllNetworkBanks();

        return networkBanks.stream()
                .map(bank -> new DmtBankList(
                        bank.getId() != null ? Math.toIntExact(bank.getId()) : null,
                        bank.getBankName(),
                        bank.getIfscCode()
                ))
                .toList();
    }

    @Override
    public RemitterQueryResponse checkRemitter(
//            String mobile,
            RemitterQueryRequest request,
            VendorCredentials credentials) throws Exception {

        Map<String, Object> reqBody = Map.of("mobile", request.getMobile());

        String token = generateJwtToken();

        HttpHeaders headers = getHeaders(credentials, token);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(reqBody, headers);

        ResponseEntity<RemitterQueryResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/remitter/queryremitter",
                        HttpMethod.POST,
                        entity,
                        RemitterQueryResponse.class
                );

        return response.getBody();
    }


    @Override
    public RemitterRegisterResponse registerRemitter(
            RemitterRegisterRequest req,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        HttpHeaders headers = getHeaders(credentials, token);

        HttpEntity<RemitterRegisterRequest> entity =
                new HttpEntity<>(req, headers);

        ResponseEntity<RemitterRegisterResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/remitter/registerremitter",
                        HttpMethod.POST,
                        entity,
                        RemitterRegisterResponse.class
                );

        return response.getBody();
    }

    @Override
    public Map doEkyc(
            RemitterEkycRequest request,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        Map<String, Object> body = new HashMap<>();

        body.put("mobile", request.getMobile());
        body.put("lat", "28.123456");
        body.put("long", "78.123456");
        body.put("aadhaar_number", request.getAadhaarNumber());
        body.put("data", request.getData());
        body.put("is_iris", "2");

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", token);
        headers.set("Authorisedkey", credentials.getSaltKey());

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/remitter/queryremitter/kyc",
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

        return response.getBody();
    }


    @Override
    public BeneficiaryRegisterResponse registerBeneficiary(
            BeneficiaryRegisterRequest req,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        HttpHeaders headers = getHeaders(credentials, token);

        HttpEntity<BeneficiaryRegisterRequest> entity =
                new HttpEntity<>(req, headers);

        ResponseEntity<BeneficiaryRegisterResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary",
                        HttpMethod.POST,
                        entity,
                        BeneficiaryRegisterResponse.class
                );
        //old
//        return response.getBody();

        // new
        BeneficiaryRegisterResponse resp = response.getBody();

        if (resp == null) {
            throw new RuntimeException("Empty response from Paysprint");
        }

        //  STANDARDIZE RESPONSE
        if (resp != null && resp.isStatus()) {
            resp.setResponse_code(1);  // success
        } else {
            resp.setResponse_code(0);  // failure
        }

        return resp;

    }

    @Override
    public DmtBeneficiaryDeleteResponse deleteBeneficiary(
            DmtBeneficiaryDeleteRequest req,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", token);
        headers.set("Authorisedkey", credentials.getSaltKey());

        HttpEntity<DmtBeneficiaryDeleteRequest> entity =
                new HttpEntity<>(req, headers);

        ResponseEntity<DmtBeneficiaryDeleteResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/deletebeneficiary",
                        HttpMethod.POST,
                        entity,
                        DmtBeneficiaryDeleteResponse.class
                );

        DmtBeneficiaryDeleteResponse resp = response.getBody();

        //  IMPORTANT FIX (SET RESPONSE CODE)
        //  //new.
        if (resp != null) {

            if (resp.isStatus()) {
                resp.setResponse_code(1);   // SUCCESS
            } else {
                resp.setResponse_code(0);   // FAILURE
            }
        }

        return resp;
        //old
//        return response.getBody();
    }


    @Override
    public DmtBeneficiaryFetchResponse fetchBeneficiary(
            DmtBeneficiaryFetchRequest req,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        Map<String, Object> reqBody = Map.of("mobile", req.getMobile());

        HttpHeaders headers = getHeaders(credentials, token);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(reqBody, headers);

        ResponseEntity<DmtBeneficiaryFetchResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiary",
                        HttpMethod.POST,
                        entity,
                        DmtBeneficiaryFetchResponse.class
                );

        return response.getBody();
    }

    @Override
    public DmtBeneficiaryFetchByBeneIdResponse fetchBeneficiaryByBeneId(
            DmtBeneficiaryFetchByBeneIdRequest req,
            VendorCredentials credentials) throws Exception {

        String token = generateJwtToken();

        Map<String, Object> reqBody = Map.of(
                "mobile", req.getMobile(),
                "beneid", req.getBeneid()
        );

        HttpHeaders headers = getHeaders(credentials, token);

        HttpEntity<Map<String, Object>> entity =
                new HttpEntity<>(reqBody, headers);

        ResponseEntity<DmtBeneficiaryFetchByBeneIdResponse> response =
                restTemplate.exchange(
                        credentials.getBaseUrlProd()
                                + "/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/fetchbeneficiarybybeneid",
                        HttpMethod.POST,
                        entity,
                        DmtBeneficiaryFetchByBeneIdResponse.class
                );

        return response.getBody();
    }
}

