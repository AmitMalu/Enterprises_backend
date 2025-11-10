package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
public class ShadvalPayoutServiceImpl implements PayoutVendorService {

    private static final String VENDOR_CODE = "SHADVALPAY";

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private CommonMethods commonMethods;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getVendorCode() {
        return VENDOR_CODE;
    }

    @Override
    public PayoutResponseDto sendPayout(SendPayoutRequest req,
                                        VendorCredentials creds,
                                        String orderId,
                                        SubProducts subProducts,
                                        WalletType walletType) {
        try {
            log.info("[SHADVAL] Initiating payout orderId={} retailer={} amount={}",
                    orderId, req.getRetailerId(), req.getAmount());

            // Build request body (use Map so Jackson will convert automatically)
            Map<String, Object> payload = Map.of(
                    "amount", req.getAmount(),
                    "mode", req.getPaymentMode(),
                    "fund_account", Map.of(
                            "name", req.getBeneficiaryName(),
                            "ifsc", req.getIfscCode(),
                            "account_number", req.getAccountNumber()
                    ),
                    "contact_details", Map.of(
                            "name", req.getBeneficiaryName(),
                            "email", "testing@gmail.com",
                            "mobile", "9632587410"
                            //"email", req.getCallbackEmail() == null ? "" : req.getCallbackEmail(),
                            //"mobile", req.getCallbackMobile() == null ? "" : req.getCallbackMobile()
                    ),
                    "reference_id", orderId,
                    "latitude", "77.3066",
                    "longitude", "28.4912",
                    "narration", "test"
            );

            // convert to JSON for signing header (if required)
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Signature: plaintext = key + " payload " + key + ref_no + amount
            String shadvalKey = Objects.requireNonNull(creds.getClientSecret(), "Shadval key missing in credentials");
            String plainText = shadvalKey + " payload " + shadvalKey + orderId + req.getAmount();

            String signature = generateHmacSHA256Signature(plainText, shadvalKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", shadvalKey); // according to vendor doc
            headers.set("Signature", signature);
            headers.set("Payload", payloadJson); // vendor requires payload header

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = creds.getBaseUrlProd().endsWith("/") ?
                    creds.getBaseUrlProd() + "initiate_bank_transfer" :
                    creds.getBaseUrlProd() + "/initiate_bank_transfer";

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> vendorResp = resp.getBody();
            log.info("[SHADVAL] Raw response for orderId={}: {}", orderId, vendorResp);

            // defensive checks
            if (vendorResp == null) {
                return failureDto(orderId, "FAILED", "Empty response from Shadval", req.getAmount());
            }

            // vendorResp expected: { status, code, message, data: {...} }
            String rawStatus = String.valueOf(vendorResp.getOrDefault("status", "FAILED"));
            String mappedStatus = "SUCCESS".equalsIgnoreCase(rawStatus) ? "SUCCESS" : "FAILED";
            String message = String.valueOf(vendorResp.getOrDefault("message", ""));

            Map<String, Object> data = (Map<String, Object>) vendorResp.get("data");
            if (data == null) {
                // vendor returned top-level status + message but no data
                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorCode(VENDOR_CODE)
                        .status(mappedStatus)
                        .message(message)
                        .amount(req.getAmount())
                        .build();
            }

            // extract fund_account and trans_details safely
            Map<String, Object> fundAccount = (Map<String, Object>) data.get("fund_account");
            Map<String, Object> transDetails = (Map<String, Object>) data.get("trans_details");

            BigDecimal amount = req.getAmount();
            if (transDetails != null && transDetails.get("amount") != null) {
                try {
                    amount = new BigDecimal(String.valueOf(transDetails.get("amount")));
                } catch (NumberFormatException ignore) { /* keep req.getAmount() */ }
            }

            String referenceId = String.valueOf(data.getOrDefault("reference_id", orderId));
            String beneficiaryName = fundAccount != null ? String.valueOf(fundAccount.getOrDefault("name", req.getBeneficiaryName())) : req.getBeneficiaryName();
            String accountNumber = fundAccount != null ? String.valueOf(fundAccount.getOrDefault("account_number", req.getAccountNumber())) : req.getAccountNumber();
            String ifsc = fundAccount != null ? String.valueOf(fundAccount.getOrDefault("ifsc", req.getIfscCode())) : req.getIfscCode();

            return PayoutResponseDto.builder()
                    .orderId(referenceId)
                    .vendorCode(VENDOR_CODE)
                    .status(mappedStatus)
                    .message(message)
                    .amount(amount)
                    .beneficiaryName(beneficiaryName)
                    .accountNumber(accountNumber)
                    .ifscCode(ifsc)
                    .build();

        } catch (RestClientException rce) {
            log.error("[SHADVAL] Rest error for orderId {}: {}", orderId, rce.getMessage(), rce);
            return failureDto(orderId, "FAILED", "Vendor communication error: " + rce.getMessage(), req.getAmount());
        } catch (Exception e) {
            log.error("[SHADVAL] Unexpected error for orderId {}: {}", orderId, e.getMessage(), e);
            return failureDto(orderId, "FAILED", "Internal error: " + e.getMessage(), req.getAmount());
        }
    }

    // helper to create failure DTO
    private PayoutResponseDto failureDto(String orderId, String status, String message, BigDecimal amount) {
        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorCode(VENDOR_CODE)
                .status(status)
                .message(message)
                .amount(amount)
                .build();
    }

    private String generateHmacSHA256Signature(String plainText, String key) throws Exception {
        Mac sha256Hmac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256Hmac.init(secretKey);
        byte[] hash = sha256Hmac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Convert to Base64 (most fintech APIs use Base64)
        return Base64.getEncoder().encodeToString(hash);
    }

}
