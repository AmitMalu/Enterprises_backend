package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.RemitterTransactionResponse;
import com.im.admin.dto.SendOtpRequest;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PennyDropRequest;
import com.im.admin.request.SendDmtRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaysprintPennyDropResponse;
import com.im.admin.response.PaysprintSendOtpResponse;
import com.im.admin.response.TransactionRequest;
import com.im.admin.service.DmtVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class PaysprintDmtServiceImpl implements DmtVendorService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public String getVendorCode() {
        return "Paysprint DMT";
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest req,
                                                      VendorCredentials credentials,
                                                      String baseUrl) {
        try {
            String url = baseUrl +
                    "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/benenameverify";

            // ------------------------------------------------------
            // 💥 Build JSON body using Map (as per Paysprint Docs)
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
                    new HttpEntity<>(body, getHeaders(credentials));

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
    public PaysprintSendOtpResponse sendOtp(SendDmtRequest req, VendorCredentials credentials) {
        try {
            String sendOtpUrl = credentials.getBaseUrlUat() +
                    "/service-api/api/v1/service/dmt/kyc/transact/transact/send_otp";

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
            String requestJson = objectMapper.writeValueAsString(body);

            HttpEntity<Map<String, String>> requestEntity =
                    new HttpEntity<>(body, getHeaders(credentials));

            // -------------------------
            // 2. Call Paysprint API
            // -------------------------
            ResponseEntity<PaysprintSendOtpResponse> response = restTemplate.exchange(
                    sendOtpUrl,
                    HttpMethod.POST,
                    requestEntity,
                    PaysprintSendOtpResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public RemitterTransactionResponse processDmt(
            SendDmtRequest req,
            VendorCredentials credentials,
            String orderId,
            SubProducts subProducts,
            WalletType walletType) {

        try {
            // 1. Headers
            HttpHeaders headers = getHeaders(credentials);

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

            HttpEntity<Map<String, Object>> txnEntity = new HttpEntity<>(body, headers);

            String txnUrl = credentials.getBaseUrlUat()
                    + "/service-api/api/v1/service/dmt/kyc/transact/transact";

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

            return txnResponse;

        } catch (HttpClientErrorException e) {

            //  Vendor error body mil raha
            String json = e.getResponseBodyAsString();
            log.error("[PAYSPRINT] Vendor ERROR Response: {}", json);

            try {
                ObjectMapper mapper = new ObjectMapper();
                RemitterTransactionResponse vendorError =
                        mapper.readValue(json, RemitterTransactionResponse.class);

                return vendorError;   //  EXACT vendor response
            } catch (Exception ex) {
                return new RemitterTransactionResponse(
                        false,
                        0,
                        null, null, null, null, null,
                        "Vendor error parse failed",
                        null, null, null, null, null, null, null,
                        null, null,null
                );
            }

        } catch (Exception e) {
            log.error("[PAYSPRINT] UNKNOWN Exception: {}", e.getMessage(), e);
            return new RemitterTransactionResponse(
                    false,
                    0,
                    null, null, null, null, null,
                    "Unknown exception",
                    null, null, null, null, null, null, null,
                    null, null,null
            );
        }
    }



    // 🔹 Helper method for common headers
    private HttpHeaders getHeaders(VendorCredentials credentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        headers.set("Token", credentials.getToken());
        headers.set("Authorisedkey", credentials.getSaltKey());
        return headers;
    }
}
