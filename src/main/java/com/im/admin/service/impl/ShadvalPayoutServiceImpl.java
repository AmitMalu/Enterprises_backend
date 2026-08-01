package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.LoggableVendor;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.repositories.SchemeRateRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@LoggableVendor
public class ShadvalPayoutServiceImpl implements PayoutVendorService {

    private static final List<String> VENDOR_NAME = List.of("Shadval Payout", "Shadval MTB");

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<String> getVendorName() {
        return VENDOR_NAME;
    }

    @Override
    public PayoutResponseDto sendPayout(SendPayoutRequest req,
                                        VendorCredentials creds,
                                        String orderId,
                                        SubProducts subProducts,
                                        WalletType walletType,
                                        RequestHeaderMetaDto requestHeaderMetaDto) {
        try {
            log.info("[SHADVAL] Initiating payout orderId={} retailer={} amount={}",
                    orderId, req.getRetailerId(), req.getAmount());

            Retailer retailer = retailerRepository.findByRetailerId(req.getRetailerId())
                    .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + req.getRetailerId()));

            TransactionSchemeDetailsRequestDto dto = null;

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
                            "name", retailer.getName(),
                            "email", retailer.getEmailId(),
                            "mobile", retailer.getMobileNumber()
                    ),
                    "reference_id", orderId,
                    "latitude", retailer.getLatitude(),
                    "longitude", retailer.getLongitude(),
                    "narration", "test"
            );

            // convert to JSON for signing header (if required)
            String payloadJson = objectMapper.writeValueAsString(payload);

            // Signature: plaintext = key + " payload " + key + ref_no + amount
            String shadvalKey = Objects.requireNonNull(creds.getSaltKey(), "Shadval key missing in credentials");
            String plainText = shadvalKey + " payload " + shadvalKey + orderId + req.getAmount();

            String signature = generateHmacSHA256Signature(plainText, shadvalKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", shadvalKey); // according to vendor doc
            headers.set("Signature", signature);
            headers.set("Payload", payloadJson); // vendor requires payload header

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
            String url = creds.getBaseUrlProd() + "initiate_bank_transfer";

            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

            Map<String, Object> vendorResp = resp.getBody();
            log.info("[SHADVAL] Raw response for orderId={}: {}", orderId, vendorResp);

            // defensive checks
            if (vendorResp == null) {
                return failureDto(orderId, "FAILED", "Empty response from Shadval", req.getAmount() ,creds.getVendor().getVendorName());
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
                        .vendorName(creds.getVendor().getVendorName())
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

            // Prepare DTO (common for all statuses)
            log.info("[5] Constructing TransactionSchemeDetailsRequestDto...");
            dto = new TransactionSchemeDetailsRequestDto();

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            subProducts.getProduct().getTransactionType(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() -> new RuntimeException("No matching scheme rate found for amount: " + req.getAmount()));

            log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                    selectedScheme.getGross(), selectedScheme.getRetailer());

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(req.getAmount().doubleValue());
            dto.setConfirmAmount(req.getAmount().doubleValue());
            dto.setOperatorName(selectedScheme.getOperatorName());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(subProducts.getProduct().getTransactionType());
            dto.setWalletType(subProducts.getWalletType());
            dto.setGstValue(String.valueOf(subProducts.getProduct().getSgst()));
            dto.setTdsRate(String.valueOf(subProducts.getProduct().getTds()));
            dto.setProductCode(subProducts.getProduct().getProductCode());
            dto.setSubProductCode(subProducts.getSubProductCode());
            dto.setIsCommission(subProducts.getProduct().getIsCommissions());
            dto.setHasCharge(subProducts.getProduct().getHasCharges());
            dto.setHierarchyId(req.getRetailerId());
            dto.setLatitude(requestHeaderMetaDto.getLatitude());
            dto.setLongitude(requestHeaderMetaDto.getLongitude());

            log.info("[6] DTO ready for transaction processing: {}", dto);

            // Process transaction for any status (SUCCESS, FAILED, PENDING, etc.)
            transactionEngineServiceImpl.processTransaction(dto, String.valueOf(vendorResp.get("status")));
            log.info("[7] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                    orderId, vendorResp.get("status"));

            return PayoutResponseDto.builder()
                    .orderId(referenceId)
                    .vendorName(creds.getVendor().getVendorName())
                    .status(mappedStatus)
                    .message(message)
                    .amount(amount)
                    .beneficiaryName(beneficiaryName)
                    .accountNumber(accountNumber)
                    .ifscCode(ifsc)
                    .build();

        } catch (RestClientException rce) {
            log.error("[SHADVAL] Rest error for orderId {}: {}", orderId, rce.getMessage(), rce);
            return failureDto(orderId, "FAILED", "Vendor communication error: " + rce.getMessage(), req.getAmount(), creds.getVendor().getVendorName());
        } catch (Exception e) {
            log.error("[SHADVAL] Unexpected error for orderId {}: {}", orderId, e.getMessage(), e);
            return failureDto(orderId, "FAILED", "Internal error: " + e.getMessage(), req.getAmount(), creds.getVendor().getVendorName());
        }
    }

    // helper to create failure DTO
    private PayoutResponseDto failureDto(String orderId, String status, String message, BigDecimal amount, String vendorName) {
        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(vendorName)
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
