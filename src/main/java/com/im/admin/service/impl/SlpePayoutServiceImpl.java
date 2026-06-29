package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@Slf4j
public class SlpePayoutServiceImpl implements PayoutVendorService, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public String getVendorName() {
        return "SLPE Payout";
    }

    @Override
    public PayoutResponseDto sendPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            Double latitude,
            Double longitude,
            Object ipAddress) {

        log.info("Starting SLPE payout | orderId={}, amount={}", orderId, req.getAmount());

        try {

            // ===== 1. Prepare Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("access-token", creds.getToken());
            headers.set("api-key", creds.getSaltKey());
            headers.set("api-secret", creds.getClientSecret());
            headers.set("api-mode", "test");

            // ===== 2. Prepare Request Body =====
            Map<String, Object> body = new HashMap<>();

            body.put("amount", req.getAmount());
            body.put("mode", req.getPaymentMode()); // IMPS / NEFT
            body.put("call_back_url", creds.getCallbackUrl());
            body.put("gateway_id", "1"); // make dynamic if needed

            // ===== Bank Details =====
            Map<String, Object> bank = new HashMap<>();
            bank.put("name", req.getBeneficiaryName());
            bank.put("ifsc", req.getIfscCode());
            bank.put("bank_name", req.getBankName());
            bank.put("account_number", req.getAccountNumber());

            body.put("bank_account", bank);

            log.info("SLPE payout request body: {}", body);

            // ===== 3. API Call =====
            RestTemplate restTemplate = new RestTemplate();

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(body, headers);

            String url = creds.getBaseUrlUat() + "/create-payout";

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();

            log.info("SLPE payout response: {}", responseBody);

            // ===== 4. Handle Response =====
            if (responseBody != null && Boolean.TRUE.equals(responseBody.get("result"))) {

                String payoutId = responseBody.get("payout_id").toString();
                String status = responseBody.get("status").toString(); // processing

                log.info("SLPE payout success | payoutId={}, status={}", payoutId, status);

                PayoutPendingTransactions pending = new PayoutPendingTransactions();
                pending.setTransactionId(orderId);
                pending.setSchemeCode(req.getSchemeCode());
                pending.setRetailerId(req.getRetailerId());
                pending.setAmount(req.getAmount());
                pending.setTransactionType(req.getPaymentMode());
                pending.setCreatedAt(LocalDateTime.now());
                pending.setIfscCode(req.getIfscCode());
                pending.setAccountNumber(req.getAccountNumber());
                pending.setBeneficiaryName(req.getBeneficiaryName());
                pending.setLatitude(latitude);
                pending.setLongitude(longitude);

                // ===== Map status =====
                String mappedStatus;
                if ("processing".equalsIgnoreCase(status)) {
                    mappedStatus = "PENDING";
                    pending.setStatus("PENDING");
                } else if ("success".equalsIgnoreCase(status)) {
                    mappedStatus = "SUCCESS";
                    pending.setStatus("SUCCESS");
                } else {
                    mappedStatus = "FAILED";
                    pending.setStatus("FAILED");
                }

                payoutPendingTransactionRepository.save(pending);

                // ===== 5. Build Response DTO =====
                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorName(getVendorName())
                        .status(mappedStatus)
                        .message("Payout initiated successfully")
                        .amount(req.getAmount())
                        .beneficiaryName(req.getBeneficiaryName())
                        .accountNumber(req.getAccountNumber())
                        .ifscCode(req.getIfscCode())
                        .build();

            } else {
                log.error("SLPE payout failed: {}", responseBody);

                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorName(getVendorName())
                        .status("FAILED")
                        .message("Payout failed")
                        .amount(req.getAmount())
                        .build();
            }

        } catch (Exception e) {
            log.error("Error while processing SLPE payout", e);

            return PayoutResponseDto.builder()
                    .orderId(orderId)
                    .vendorName(getVendorName())
                    .status("FAILED")
                    .message("Exception: " + e.getMessage())
                    .amount(req.getAmount())
                    .build();
        }
    }

    @Override
    @Transactional
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("SLPE Callback Received: {}", payload);

        try {
            // ===== 1. Extract Fields =====
            String orderId = payload.get("order_id");
            String slpeStatus = payload.get("status");   // paid / failed / pending
            String txnId = payload.get("txn_id");

            Double amount = payload.get("amount") != null
                    ? Double.parseDouble(payload.get("amount"))
                    : 0.0;
//
//            String txnMode = payload.get("mode");        // card
//            String category = payload.get("network");    // VISA
//            String pgPartner = payload.get("pg_partner");// Razorpay

            if (orderId == null) {
                log.error("OrderId missing in callback");
                return "FAILED";
            }

            // ===== 2. Fetch Pending Transaction =====
            PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                    .findByTransactionId(orderId)
                    .orElseThrow(() ->
                            new RuntimeException("Pending transaction not found for order ID: " + orderId));

            log.info("[2] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                    pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                    pendingTxn.getAmount(), pendingTxn.getStatus());

            // ===== 3. Fetch Scheme Rate =====
            log.info("[3] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                    pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), amount);

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            pendingTxn.getSchemeCode(),
                            pendingTxn.getTransactionType(),
                            amount)
                    .orElseThrow(() ->
                            new RuntimeException("No matching scheme rate found for amount: " + amount));

            log.info("[3.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(), selectedScheme.getRateType(), selectedScheme.getSlabType(),
                    selectedScheme.getGross(), selectedScheme.getRetailer());

            // ===== 4. Fetch Scheme =====
            Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                    .orElseThrow(() ->
                            new RuntimeException("Scheme Not Found with code: " + selectedScheme.getSchemeCode()));

            log.info("[4] Scheme fetched successfully: HashCode={}", scheme.getSchemeCombinationHashCode());

            // ===== 5. Extract Product & SubProduct =====
            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

            if (hashParts.length < 2) {
                throw new RuntimeException("Invalid schemeCombinationHashCode format: "
                        + scheme.getSchemeCombinationHashCode());
            }

            short productId = Short.parseShort(hashParts[0]);
            String subProductIdStr = hashParts[1];

            Products product = productsRepository.findById(productId)
                    .orElseThrow(() ->
                            new RuntimeException("Product not found with id: " + productId));

            SubProducts subProduct = subProductsRepository.findById(subProductIdStr)
                    .orElseThrow(() ->
                            new RuntimeException("SubProduct not found with id: " + subProductIdStr));

            log.info("[5] Product/SubProduct fetched successfully: Product={}, SubProduct={}",
                    product.getName(), subProduct.getSubProductTitle());

            // ===== 6. Prepare DTO =====
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            dto.setBaseTranxId(baseTxnId);
            dto.setAmount(amount);
            dto.setConfirmAmount(amount);
            dto.setOperatorName(pendingTxn.getTransactionType());
            dto.setOperatorCode(selectedScheme.getOperatorCode());
            dto.setSchemeRate(selectedScheme);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setHierarchyId(pendingTxn.getRetailerId());
            dto.setVendorName("SLPE Payout");
            dto.setVendorTxnId(txnId);

            log.info("[6] DTO ready for transaction processing: {}", dto);

            // ===== 7. Map Vendor Status =====
            String finalStatus = switch (slpeStatus) {
                case "SUCCESS" -> "SUCCESS";
                case "FAILED" -> "FAILED";
                case "PENDING" -> "PENDING";
                default -> "UNKNOWN";
            };

            log.info("[7] Status mapped from vendor: {} -> {}", slpeStatus, finalStatus);

            // ===== 8. Process Transaction =====
            transactionEngineServiceImpl.processTransaction(dto, finalStatus);

            log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                    orderId, finalStatus);

            // ===== 9. Update UTR =====
            pendingTxn.setStatus(finalStatus);
            payoutPendingTransactionRepository.save(pendingTxn);

            log.info("[9] Pending transaction updated with txnId={}, STATUS={}", txnId, finalStatus);

            return "SUCCESS";

        } catch (Exception e) {
            log.error("Error in SLPE callback", e);
            return "FAILED";
        }
    }


    @Override
    public PayoutStatusResponse checkPayoutStatus(String payoutId, VendorCredentials credentials) {

        log.info("Checking SLPE payout status for payoutId={}", payoutId);

        try {

            // ===== 2. Prepare Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("access-token", credentials.getToken());
            headers.set("api-key", credentials.getSaltKey());
            headers.set("api-secret", credentials.getClientSecret());
            headers.set("api-mode", "test");

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // ===== 3. API Call =====
            String url = credentials.getBaseUrlUat() + "/payout-status/" + payoutId;

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> body = response.getBody();

            log.info("SLPE payout status response: {}", body);

            // ===== 4. Parse Response =====
            Map data = (Map) body.get("data");
            Map payoutData = (Map) data.get("payout_data");

            String slpeStatus = (String) payoutData.get("status"); // processing / success / failed
            String amountStr = (String) payoutData.get("amount");
            String utr = payoutData.get("utr") != null
                    ? payoutData.get("utr").toString()
                    : null;

            Double amount = amountStr != null
                    ? Double.valueOf(amountStr)
                    : 0.0;

            // ===== 5. Map Status =====
            String mappedStatus;
            if ("processing".equalsIgnoreCase(slpeStatus)) {
                mappedStatus = "PENDING";
            } else if ("success".equalsIgnoreCase(slpeStatus)) {
                mappedStatus = "SUCCESS";
            } else if ("failed".equalsIgnoreCase(slpeStatus)) {
                mappedStatus = "FAILED";
            } else {
                mappedStatus = "PENDING";
            }

            log.info("Mapped payout status: {} -> {}", slpeStatus, mappedStatus);

            // ===== 6. Return Common DTO =====
            return PayoutStatusResponse.builder()
                    .orderId(payoutId)
                    .status(mappedStatus)
                    .message("Payout status fetched successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error while checking payout status", e);

            return PayoutStatusResponse.builder()
                    .orderId(payoutId)
                    .status("FAILED")
                    .message("Error: " + e.getMessage())
                    .build();
        }
    }
}
