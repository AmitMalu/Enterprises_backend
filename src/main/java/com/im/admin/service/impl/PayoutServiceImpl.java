package com.im.admin.service.impl;

import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.dto.PayoutTokenResponseDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.repositories.BeneficiaryBankAccountRepository;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.service.PayoutService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class PayoutServiceImpl implements PayoutService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

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

    @Override
    public CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto) {
        BeneficiaryBankAccount entity = new BeneficiaryBankAccount();
        entity.setRetailerId(dto.getRetailerId());
        entity.setBankName(dto.getBankName());
        entity.setBankHolderName(dto.getBankHolderName());
        entity.setAccountNumber(dto.getAccountNumber());
        entity.setIfscCode(dto.getIfscCode());
        entity.setCreatedBy(dto.getCreatedBy());

        BeneficiaryBankAccount saved = beneficiaryBankAccountRepository.save(entity);
        return CommonResponse.success(saved, "Bank account saved successfully");
    }

    @Override
    public CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(String search, Pageable pageable) {

        Page<BeneficiaryBankAccount> results = beneficiaryBankAccountRepository.findAll((root, query, cb) -> {
            if (search != null && !search.isBlank()) {
                String likeSearch = "%" + search.toLowerCase() + "%";
                Predicate p1 = cb.like(cb.lower(root.get("bankHolderName")), likeSearch);
                Predicate p2 = cb.like(cb.lower(root.get("accountNumber")), likeSearch);
                Predicate p3 = cb.like(cb.lower(root.get("ifscCode")), likeSearch);
                Predicate p4 = cb.like(cb.lower(root.get("bankName")), likeSearch);

                return cb.or(p1, p2, p3, p4);
            }
            return cb.conjunction(); // no filtering
        }, pageable);

        return CommonResponse.success(results,"Filtered results");
    }


    @Override
    public CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids) {
        List<Long> notFound = new ArrayList<>();

        for (Long id : ids) {
            if (beneficiaryBankAccountRepository.existsById(id)) {
                beneficiaryBankAccountRepository.deleteById(id);
            } else {
                notFound.add(id);
            }
        }

        String message = "Deleted successfully.";
        if (!notFound.isEmpty()) {
            message += " But some IDs not found: " + notFound;
        }

        return CommonResponse.success(message, "Processed IDs: " + ids);
    }


}
