package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.dto.SendOtpRequest;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.response.TransactionRequest;
import com.im.admin.service.DmtVendorService;
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
import java.util.Map;
import java.time.Instant;

@Slf4j
@Service
//@RequiredArgsConstructor
public class PaysprintDmtServiceImpl implements DmtVendorService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonFunctions commonFunctions;

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

            String responseJson = objectMapper.writeValueAsString(response.getBody());

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    credentials.getVendor().getId(),
                    LocalDateTime.now(),
                    responseJson,
                    null
            );

            return response.getBody();

        }
        catch (HttpStatusCodeException e) {

            String vendorErrorBody = e.getResponseBodyAsString();

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
            WalletType walletType) {

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
                        null, null, null
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
                    null, null, null
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
}
