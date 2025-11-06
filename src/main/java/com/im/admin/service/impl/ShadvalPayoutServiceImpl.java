package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.ShadvalPayResponse;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class ShadvalPayoutServiceImpl implements PayoutVendorService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public String getVendorCode() {
        return "SHADVALPAY";
    }

    @Override
    public PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType) {
        try {
            log.info("[SHADVAL] Initiating payout for orderId: {}", orderId);

            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), req.getRetailerId());
            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            commonMethods.updateWallet(walletAmountTable, req.getRetailerId(), walletType, req.getAmount(), "Debit", baseTxnId,
                    "PENDING", "DEBIT", subProducts.getSubProductTitle(),
                    req.getBeneficiaryName() + " #" + req.getAccountNumber(), "Fund Transferred " + "#" + req.getAccountNumber());

            //Build Request Body
            Map<String, Object> body = Map.of(
                    "amount", req.getAmount(),
                    "mode", req.getPaymentMode(),
                    "fund_account", Map.of(
                            "name", req.getBeneficiaryName(),
                            "ifsc", req.getIfscCode(),
                            "account_number", req.getAccountNumber()
                    ),
                    "contact_details", Map.of(
                            "name", req.getBeneficiaryName(),
                            "email", "test@gmail.com",
                            "mobile", "9999999999"
                    ),
                    "reference_id", orderId,
                    "latitude", "77.3066",
                    "longitude", "28.4912",
                    "narration", "test"
            );

            //Convert Body to JSON String
            ObjectMapper mapper = new ObjectMapper();
            String payloadJson = mapper.writeValueAsString(body);

            // Format: ShadvalKey + " payload " + ShadvalKey + ref_no + amount
            String shadvalKey = creds.getClientSecret(); // assuming VendorCredentials has this shadvalKey = ClientKey
            String plainText = shadvalKey + " payload " + shadvalKey + orderId + req.getAmount();

            //Generate HMAC SHA-256 Signature
            String signature = generateHmacSHA256Signature(plainText, shadvalKey);

            //Prepare Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", shadvalKey);
            headers.set("Signature", signature);
            headers.set("Payload", payloadJson);

            HttpEntity<String> entity = new HttpEntity<>(payloadJson, headers);

            String url = creds.getBaseUrlProd() + "/initiate_bank_transfer";
            log.info("[SHADVAL] Sending payout request to: {}", url);

            // Send API Request
            ResponseEntity<ShadvalPayResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    ShadvalPayResponse.class
            );

            ShadvalPayResponse vendorResp = response.getBody();

            // Map Vendor Response to Common DTO
            log.info("[SHADVAL] Vendor response: {}", vendorResp);
            return new PayoutResponseDto(vendorResp.getStatus(), vendorResp.getMessage(), vendorResp.getCode());

        } catch (Exception e) {
            log.error("[SHADVAL] Error during payout execution: {}", e.getMessage(), e);
            return new PayoutResponseDto(orderId, "FAILED", "Error during payout execution: " + e.getMessage());
        }
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
