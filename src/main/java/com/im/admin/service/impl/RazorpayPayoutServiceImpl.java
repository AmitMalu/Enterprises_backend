package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.BeneficiaryBankAccountRepository;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.service.PayoutVendorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class RazorpayPayoutServiceImpl  implements PayoutVendorService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Override
    public String getVendorName() {
        return "Razorpay Payout";
    }

    @Override
    public PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType, RequestHeaderMetaDto requestHeaderMetaDto) {

        log.info("[RAZORPAY] Starting payout for orderId={}", orderId);

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;
            try {

                // 1. Fetch beneficiary
                BeneficiaryBankAccount beneficiary =
                        beneficiaryBankAccountRepository
                                .findTopByRetailerIdAndAccountNumberOrderByIdDesc(
                                        req.getRetailerId(),
                                        req.getAccountNumber()
                                )
                                .orElse(null);

                // 2. Check if Razorpay mapping exists
                assert beneficiary != null;
                if (beneficiary.getRazorpayFundAccountId() == null || beneficiary.getRazorpayContactId() == null) {

                    // Create Contact
                    String contactId = createContactAPI(beneficiary, creds);

                    // Create Fund Account
                    String fundAccountId = createFundAccountAPI(contactId, beneficiary, creds);

                    // Save in DB
                    beneficiary.setRazorpayContactId(contactId);
                    beneficiary.setRazorpayFundAccountId(fundAccountId);
                    beneficiaryBankAccountRepository.save(beneficiary);
                }

                // 3. Call Payout API
                PayoutResponseDto payoutResponse = createPayoutAPI(
                        beneficiary.getRazorpayFundAccountId(),
                        req,
                        creds,
                        orderId
                );


            } catch (Exception e) {
                log.error("[RAZORPAY] Exception during payout: {}", e.getMessage(), e);
                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorName(getVendorName())
                        .status("FAILED")
                        .message("Razorpay error: " + e.getMessage())
                        .amount(req.getAmount())
                        .build();
            } finally {
                commonFunctions.saveVendorLogs(
                        requestTime,
                        requestJson,
                        creds.getVendor().getId(),
                        LocalDateTime.now(),
                        responseJson,
                        null
                );
            }

        return null;
    }

    private HttpHeaders getHeaders(VendorCredentials creds) {
        String auth = creds.getClientId() + ":" + creds.getClientSecret();
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    private String createContactAPI(BeneficiaryBankAccount beneficiary, VendorCredentials creds) {

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("name", beneficiary.getBankHolderName());
            request.put("email", beneficiary.getEmailId());
            request.put("contact", beneficiary.getMobileNo());
            request.put("type", "vendor");
            request.put("reference_id", "ben_" + beneficiary.getId());

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(request, getHeaders(creds));

            ResponseEntity<Map> response =
                    restTemplate.exchange(creds.getBaseUrlUat() + "/contacts", HttpMethod.POST, entity, Map.class);

            Map body = response.getBody();

            String contactId = (String) body.get("id");

            log.info("[RAZORPAY] Contact Created: {}", contactId);

            return contactId;

        } catch (Exception e) {
            log.error("[RAZORPAY] Contact creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create contact");
        }
    }

    private String createFundAccountAPI(String contactId, BeneficiaryBankAccount beneficiary, VendorCredentials creds) {

        try {
            Map<String, Object> bankAccount = new HashMap<>();
            bankAccount.put("name", beneficiary.getBankHolderName());
            bankAccount.put("ifsc", beneficiary.getIfscCode());
            bankAccount.put("account_number", beneficiary.getAccountNumber());

            Map<String, Object> request = new HashMap<>();
            request.put("contact_id", contactId);
            request.put("account_type", "bank_account");
            request.put("bank_account", bankAccount);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(request, getHeaders(creds));

            ResponseEntity<Map> response =
                    restTemplate.exchange(creds.getBaseUrlUat() + "/fund_accounts", HttpMethod.POST, entity, Map.class);

            Map body = response.getBody();

            String fundAccountId = (String) body.get("id");

            log.info("[RAZORPAY] Fund Account Created: {}", fundAccountId);

            return fundAccountId;

        } catch (Exception e) {
            log.error("[RAZORPAY] Fund account creation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create fund account");
        }
    }

    private PayoutResponseDto createPayoutAPI(
            String fundAccountId,
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId
    ) {

        try {

            // Convert amount to paise
            long amountInPaise = req.getAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            if (amountInPaise < 100) {
                throw new RuntimeException("Minimum payout amount is ₹1");
            }

            Map<String, Object> request = new HashMap<>();
            //request.put("account_number", "041905005096");
            request.put("account_number", req.getAccountNumber());
            request.put("fund_account_id", fundAccountId);
            request.put("amount", amountInPaise);
            request.put("currency", "INR");
            request.put("mode", req.getPaymentMode().toUpperCase());
            request.put("purpose", "payout");
            request.put("queue_if_low_balance", true);
            request.put("reference_id", orderId);
            request.put("narration", "Payout " + orderId);

            // Headers
            HttpHeaders headers = getHeaders(creds);

            // VERY IMPORTANT (Idempotency)
            headers.add("X-Payout-Idempotency", orderId);

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(request, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response =
                    restTemplate.exchange(creds.getBaseUrlUat() + "/payouts", HttpMethod.POST, entity, Map.class);

            Map body = response.getBody();

            String payoutId = (String) body.get("id");
            String status = (String) body.get("status");
            String utr = body.get("utr") != null ? body.get("utr").toString() : null;

            log.info("[RAZORPAY] Payout Created | payoutId={} status={}", payoutId, status);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .vendorName("Razorpay Payout")
                    .status(status)
                    .message("Payout " + status)
                    .amount(req.getAmount())
                    .beneficiaryName(req.getBeneficiaryName())
                    .accountNumber(req.getAccountNumber())
                    .ifscCode(req.getIfscCode())
                    .build();

        } catch (HttpClientErrorException e) {

            log.error("[RAZORPAY] API Error: {}", e.getResponseBodyAsString());

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .vendorName(getVendorName())
                    .status("FAILED")
                    .message("Razorpay error: " + e.getResponseBodyAsString())
                    .amount(req.getAmount())
                    .build();

        } catch (Exception e) {

            log.error("[RAZORPAY] Exception: {}", e.getMessage(), e);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .vendorName(getVendorName())
                    .status("FAILED")
                    .message("Razorpay error: " + e.getMessage())
                    .amount(req.getAmount())
                    .build();
        }
    }
}
