package com.im.admin.service.impl;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.dto.PayoutTokenResponseDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.VendorRouting;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.BeneficiaryBankAccountRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.repositories.VendorRoutingRepository;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.PayoutResponseDto;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class PayoutServiceImpl implements PayoutService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

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

    public String generateTokenForPayout(VendorCredentials vendorCredentials) {

        // Set headers only
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", vendorCredentials.getClientId());
        headers.set("username", vendorCredentials.getUsername());
        headers.set("password", vendorCredentials.getPassword());

        HttpEntity<String> requestEntity = new HttpEntity<>(null, headers);

        String url = vendorCredentials.getTokenUrlUat();
        log.info("Sending token request to URL: {}", url);

        ResponseEntity<PayoutTokenResponseDTO> response = restTemplate.postForEntity(
                url + "/token",
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
    @Transactional
    public CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest) {
        try {
            Optional<SubProducts> subProducts = subProductsRepository.findById(sendPayoutRequest.getSubProductId());
            WalletType walletType = WalletType.valueOf(subProducts.get().getWalletType());

            BigDecimal availableBalance = commonMethods.getCurrentBalance(sendPayoutRequest.getRetailerId(), walletType.name());
            if (availableBalance.compareTo(sendPayoutRequest.getAmount()) < 0) {
                return new CommonResponse<>(400, "Insufficient Balance", null);
            }

            String baseTransactionId = commonMethods.generateBaseTransactionId();

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), sendPayoutRequest.getRetailerId());
            commonMethods.updateWallet(walletAmountTable, sendPayoutRequest.getRetailerId(), walletType, sendPayoutRequest.getAmount(), "Debit", "TXN" + baseTransactionId, "VENDOR PENDING", "DEBIT");

            VendorRouting routing = vendorRoutingRepository.findBySubProductId(sendPayoutRequest.getSubProductId());
            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("No credentials found for selected vendor"));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("token", generateTokenForPayout(credentials));

            Map<String, Object> payout = new LinkedHashMap<>();
            payout.put("account_number", String.valueOf(sendPayoutRequest.getAccountNumber()));  // "026291800001191"
            payout.put("ifsc_code", String.valueOf(sendPayoutRequest.getIfscCode()));            // "YESB0000001"
            payout.put("amount", String.format("%.2f", sendPayoutRequest.getAmount()));          // "11.00"
            payout.put("order_id", "TXN" + baseTransactionId);                                   // "TXN25082012270100272"
            payout.put("beneficiary_name", String.valueOf(sendPayoutRequest.getBeneficiaryName()));
            payout.put("payment_mode", String.valueOf(sendPayoutRequest.getPaymentMode()));      // "IMPS"

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payout, headers);

            String url = credentials.getBaseUrlUat() + "/payment";
            log.info("Sending payout request to RunPaisa. URL: {}, Headers: {}, Body: {}", url, headers, payout);

            ResponseEntity<PayoutResponseDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    PayoutResponseDto.class
            );

            PayoutResponseDto payoutResponse = response.getBody();
            assert payoutResponse != null;
            log.info("RunPaisa payout response: {}", payoutResponse.getMessage());


            if (payoutResponse != null && "ACCEPTED".equalsIgnoreCase(payoutResponse.getStatus())) {
                return new CommonResponse<>(200, payoutResponse.getMessage(), payoutResponse);
            } else {
                String errorMsg = payoutResponse != null ? payoutResponse.getMessage() : "Null response from vendor";
                return new CommonResponse<>(400, errorMsg, payoutResponse);
            }

        }  catch (HttpStatusCodeException e) {
        log.error("Vendor returned HTTP error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        return new CommonResponse<>(e.getStatusCode().value(), "Vendor Error", null);
    } catch (RestClientException e) {
        log.error("Vendor communication error: {}", e.getMessage(), e);
        return new CommonResponse<>(500, "Vendor communication failed", null);
    } catch (Exception e) {
        log.error("General exception: {}", e.getMessage(), e);
        return new CommonResponse<>(500, "Exception occurred during payout", null);
    }

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
        log.info("Processing payout callback: {}", payload);

        String code = (String) payload.get("code");
        String orderId = (String) payload.get("order_id");
        String status = (String) payload.get("status");

        log.info("Extracted Data - OrderID: {}, Status: {}, Code: {}", orderId, status, code);

        // Save or update your transaction record here
        log.debug("Transaction record update logic would go here");

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

    @Override
    public CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(Long id, String retailerId, String bankName, String bankHolderName, String accountNumber, String ifscCode, String paymentMode, Boolean isVerified, LocalDate createdOn, String createdBy, Pageable pageable) {

            Page<BeneficiaryBankAccount> results =
                    beneficiaryBankAccountRepository.findAll((root, query, cb) -> {
                        List<Predicate> predicates = new ArrayList<>();

                        if (id != null) {
                            predicates.add(cb.equal(root.get("id"), id));
                        }
                        if (retailerId != null && !retailerId.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("retailerId")), "%" + retailerId.toLowerCase() + "%"));
                        }
                        if (bankName != null && !bankName.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("bankName")), "%" + bankName.toLowerCase() + "%"));
                        }
                        if (bankHolderName != null && !bankHolderName.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("bankHolderName")), "%" + bankHolderName.toLowerCase() + "%"));
                        }
                        if (accountNumber != null && !accountNumber.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("accountNumber")), "%" + accountNumber.toLowerCase() + "%"));
                        }
                        if (ifscCode != null && !ifscCode.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("ifscCode")), "%" + ifscCode.toLowerCase() + "%"));
                        }
                        if (paymentMode != null && !paymentMode.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("paymentMode")), "%" + paymentMode.toLowerCase() + "%"));
                        }
                        if (isVerified != null) {
                            predicates.add(cb.equal(root.get("isVerified"), isVerified));
                        }
                        if (createdOn != null) {
                            predicates.add(cb.equal(cb.function("DATE", LocalDate.class, root.get("createdOn")), createdOn));
                        }
                        if (createdBy != null && !createdBy.isBlank()) {
                            predicates.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
                        }

                        return cb.and(predicates.toArray(new Predicate[0]));
                    }, pageable);

            return CommonResponse.success(results, "Filtered results");
    }


}
