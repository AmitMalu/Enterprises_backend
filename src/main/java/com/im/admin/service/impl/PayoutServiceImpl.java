package com.im.admin.service.impl;

import com.im.admin.dto.PayoutTokenResponseDTO;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.service.PayoutService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@Slf4j
public class PayoutServiceImpl implements PayoutService {

    @Autowired
    private CommonMethods commonMethods;

    RestTemplate restTemplate = new RestTemplate();

    @Value("${payout.base.url.uat}")
    private String baseUrl;

    @Value("${payout.client_id}")
    private String clientId;

    @Value("${payout.client_secret}")
    private String clientSecret;

    @Override
    public String generateToken() {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_secret", clientSecret);
        headers.set("client_id", clientId);

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = baseUrl + "/token";
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PayoutTokenResponseDTO> response = restTemplate.postForEntity(
                url,
                requestEntity,
                PayoutTokenResponseDTO.class
        );

        PayoutTokenResponseDTO tokenResponse = response.getBody();
        log.info("Bearer token response: {}", tokenResponse);

        if (tokenResponse != null && tokenResponse.getData() != null) {
            return tokenResponse.getData().getToken();
        } else {
            throw new RuntimeException("Failed To Get Token From Payout Vendor");
        }
    }

    @Override
    public String sendPayoutRequest(SendPayoutRequest sendPayoutRequest) {
        // Step 1: Generate token
        String token = generateToken();

        // Step 2: Build request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", token);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("account_number", sendPayoutRequest.getAccountNumber());
        body.add("ifsc_code", sendPayoutRequest.getIfscCode());
        body.add("amount", sendPayoutRequest.getAmount());

        String baseTransactionId = commonMethods.generateBaseTransactionId();
        body.add("order_id", "ORD" + baseTransactionId);

        body.add("beneficiary_name", sendPayoutRequest.getBeneficiaryName());
        body.add("payment_mode", sendPayoutRequest.getPaymentMode());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Payout Request: headers={}, body={}", headers, body);

        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/payment", requestEntity, String.class);

        log.info("RunPaisa payout response: {}", response.getBody());

        return response.getBody();
    }

    @Override
    public PayoutMerchantCallbackResponse updateMerchantCallbackUrl(PayoutMerchantCallbackRequest request) {
        String token = generateToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("token", token);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("callback_url", request.getCallbackUrl());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        log.info("Update Merchant Callback Url: headers={}, body={}", headers, body);

        ResponseEntity<PayoutMerchantCallbackResponse> response = restTemplate.exchange(
                baseUrl + "/updatemerchantcallback",
                HttpMethod.POST,
                requestEntity,
                PayoutMerchantCallbackResponse.class
        );

        return response.getBody();
    }

    @Override
    public String handlePayoutCallback(Map<String, Object> payload) {
        log.info("Received payout callback: {}", payload);

        // Sample processing logic
        String code = (String) payload.get("code");
        String orderId = (String) payload.get("order_id");
        String status = (String) payload.get("status");

        log.info("Payout Callback - OrderID: {}, Status: {}, Code: {}", orderId, status, code);

        // Save or update your transaction record here

        return "Callback received";
    }

}
