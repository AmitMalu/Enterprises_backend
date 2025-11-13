package com.im.admin.service.impl;

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
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class PaysprintDmtServiceImpl implements DmtVendorService {

    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String getVendorCode() {
        return "Paysprint DMT";
    }

    @Override
    public PaysprintPennyDropResponse pennyDropVerify(PennyDropRequest req, VendorCredentials credentials, String baseUrl) {
        try {
            String url = baseUrl + "/service-api/api/v1/service/dmt/kyc/beneficiary/registerbeneficiary/benenameverify";
            HttpHeaders headers = getHeaders(credentials);

            HttpEntity<PennyDropRequest> entity = new HttpEntity<>(req, headers);
            log.info("[PAYSPRINT] Penny Drop Verify → URL: {} | Body: {}", url, req);

            ResponseEntity<PaysprintPennyDropResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    PaysprintPennyDropResponse.class
            );

            PaysprintPennyDropResponse body = response.getBody();

            //  Use builder for fallback response
            return body != null ? body : PaysprintPennyDropResponse.builder()
                    .status(false)
                    .response_code(0)
                    .message("Empty response from vendor")
                    .build();

        } catch (Exception e) {
            log.error("[PAYSPRINT] Exception in pennyDropVerify: {}", e.getMessage(), e);

            //  Use builder here too
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

            SendOtpRequest otpRequestBody = new SendOtpRequest(
                    req.getMobile(),
                    req.getTxntype(),
                    String.valueOf(req.getAmount()),
                    req.getPincode(),
                    req.getAddress(),
                    req.getDob(),
                    req.getGstState(),
                    req.getLat(),
                    req.getLng(),
                    req.getReferenceId()
            );

            HttpEntity<SendOtpRequest> entity = new HttpEntity<>(otpRequestBody, getHeaders(credentials));

            ResponseEntity<PaysprintSendOtpResponse> response = restTemplate.exchange(
                    sendOtpUrl,
                    HttpMethod.POST,
                    entity,
                    PaysprintSendOtpResponse.class
            );

            return response.getBody() != null
                    ? response.getBody()
                    : new PaysprintSendOtpResponse(false, 0, "Empty response from vendor", null);

        } catch (Exception e) {
            log.error("[PAYSPRINT] Exception in sendOtp: {}", e.getMessage(), e);
            return new PaysprintSendOtpResponse(false, 0, "Error while sending OTP", null);
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
            //  STEP 1: Prepare Headers
            HttpHeaders headers = getHeaders(credentials);

            //  STEP 2: Call Send OTP API
            String sendOtpUrl = credentials.getBaseUrlUat() +
                    "/service-api/api/v1/service/dmt/kyc/transact/transact/send_otp";

            SendOtpRequest otpRequestBody = new SendOtpRequest(
                    req.getMobile(),
                    req.getTxntype(),
                    String.valueOf(req.getAmount()),
                    req.getPincode(),
                    req.getAddress(),
                    req.getDob(),
                    req.getGstState(),
                    req.getLat(),
                    req.getLng(),
                    req.getReferenceId()
            );

            HttpEntity<SendOtpRequest> otpEntity = new HttpEntity<>(otpRequestBody, headers);
            log.info("[PAYSPRINT] Sending OTP → URL: {} | Body: {}", sendOtpUrl, otpRequestBody);

            ResponseEntity<PaysprintSendOtpResponse> otpResponseEntity = restTemplate.exchange(
                    sendOtpUrl,
                    HttpMethod.POST,
                    otpEntity,
                    PaysprintSendOtpResponse.class
            );

            PaysprintSendOtpResponse otpResp = otpResponseEntity.getBody();
            log.info("[PAYSPRINT] Send OTP Response: {}", otpResp);

            if (otpResp == null || !otpResp.isStatus() || otpResp.getResponse_code() != 1) {
                return new RemitterTransactionResponse(false, "Send OTP failed", null);
            }

            //  STEP 3: Call Transaction API
            String txnUrl = credentials.getBaseUrlUat() +
                    "/service-api/api/v1/service/dmt/kyc/transact/transact";

            TransactionRequest txnRequestBody = new TransactionRequest(
                    req.getMobile(),
                    req.getReferenceId(),
                    req.getBeneId(),
                    req.getTxntype(),
                    String.valueOf(req.getAmount()),
                    req.getPincode(),
                    req.getAddress(),
                    req.getDob(),
                    req.getGstState(),
                    req.getLat(),
                    req.getLng(),
                    req.getOtp(),
                    otpResp.getStateresp()
            );

            HttpEntity<TransactionRequest> txnEntity = new HttpEntity<>(txnRequestBody, headers);
            log.info("[PAYSPRINT] Performing Transaction → URL: {} | Body: {}", txnUrl, txnRequestBody);

            ResponseEntity<RemitterTransactionResponse> txnResponseEntity = restTemplate.exchange(
                    txnUrl,
                    HttpMethod.POST,
                    txnEntity,
                    RemitterTransactionResponse.class
            );

            RemitterTransactionResponse txnResponse = txnResponseEntity.getBody();
            log.info("[PAYSPRINT] Transaction Response: {}", txnResponse);

            return txnResponse != null
                    ? txnResponse
                    : new RemitterTransactionResponse(false, "Empty vendor response", null);

        } catch (Exception e) {
            log.error("[PAYSPRINT] Exception in processDmt: {}", e.getMessage(), e);
            return new RemitterTransactionResponse(false, "Exception during vendor transaction", null);
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
