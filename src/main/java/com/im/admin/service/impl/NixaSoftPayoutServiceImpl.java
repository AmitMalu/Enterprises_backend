package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.NixaSoftPayoutStatusResponse;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.NixaSoftPayoutData;
import com.im.admin.response.NixaSoftPayoutResponse;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @author SHUBHAM KHOPADE
 */                 @Service
@Slf4j
public class NixaSoftPayoutServiceImpl implements PayoutVendorService, PayoutCallbackHandler, VendorStatusChecker {

     @Autowired
     private BeneficiaryBankAccountRepository beneficiaryBankAccountRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;


    @Override
    public String getVendorCode() {
        return "NixaSoft Payout";
    }

    @Override
    public PayoutResponseDto sendPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            SubProducts subProducts,
            WalletType walletType,
            Double latitude,
            Double longitude) {

        log.info("Starting NixaSoft payout for orderId={}, amount={}", orderId, req.getAmount());

        try {

            // ===== 1. Build Request =====

            BeneficiaryBankAccount beneficiaryBankAccount = beneficiaryBankAccountRepository.findByIfscCode(req.getIfscCode());

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("amount", req.getAmount());
            requestBody.put("mobileNumber", beneficiaryBankAccount.getMobileNo());
            requestBody.put("requestId", orderId);
            requestBody.put("accountNumber", req.getAccountNumber());
            requestBody.put("ifscCode", req.getIfscCode());
            requestBody.put("beneficiaryName", req.getBeneficiaryName());
            requestBody.put("bankName", beneficiaryBankAccount.getBankName());
            requestBody.put("transferMode", req.getPaymentMode());
            requestBody.put("emailId", req.getEmailId());
            requestBody.put("latitude", req.getLatitude());
            requestBody.put("longitude", req.getLongitude());

            // ===== 2. Headers =====

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", creds.getToken());

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            log.info("Calling NixaSoft payout API");

            ResponseEntity<NixaSoftPayoutResponse> response =
                    restTemplate.exchange(
                            creds.getBaseUrlUat() + "/payout",
                            HttpMethod.POST,
                            entity,
                            NixaSoftPayoutResponse.class
                    );

            NixaSoftPayoutResponse body = response.getBody();

            assert body != null;
            log.info("NixaSoft payout response status={}, message={}",
                    body.getStatuscode(),
                    body.getMessage());

            NixaSoftPayoutData data = body.getData();

            // ===== 3. Map Status =====

            String status = switch (body.getStatuscode()) {
                case "TXN" -> "SUCCESS";
                case "TXP" -> "PENDING";
                default -> "FAILED";
            };

            // ===== 4. Build Response DTO =====

            PayoutResponseDto payoutResponse = new PayoutResponseDto();

            payoutResponse.setOrderId(orderId);
            payoutResponse.setStatus(status);
            payoutResponse.setAmount(new BigDecimal(data.getAmount()));
            payoutResponse.setMessage(body.getMessage());

            log.info("Payout completed orderId={}, status={}", orderId, status);

            return payoutResponse;

        } catch (Exception e) {

            log.error("Error while calling NixaSoft payout API for orderId={}", orderId, e);

            PayoutResponseDto response = new PayoutResponseDto();
            response.setOrderId(orderId);
            response.setStatus("FAILED");
            response.setMessage("Vendor API error");

            return response;
        }
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("NixaSoft Callback Received: {}", payload);

        // ===== 1. Extract Fields =====
        String status = payload.get("status");
        String type = payload.get("type");
        String requestId = payload.get("requestId");
        String orderId = payload.get("orderIdTransaction");
        String amount = payload.get("amount");
        String utr = payload.get("utr");
        String description = payload.get("description");

        log.info("[1] Parsed Callback: orderId={}, requestId={}, status={}, amount={}, utr={}",
                orderId, requestId, status, amount, utr);

        // ===== 2. Fetch Pending Transaction =====
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                .findByTransactionId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Pending transaction not found for order ID: " + orderId));

        log.info("[2] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                pendingTxn.getAmount(), pendingTxn.getStatus());

        Double txnAmt = Double.valueOf(amount);

        // ===== 3. Fetch Scheme Rate =====
        log.info("[3] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(), txnAmt);

        SchemeRate selectedScheme = schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        pendingTxn.getSchemeCode(),
                        pendingTxn.getTransactionType(),
                        txnAmt)
                .orElseThrow(() ->
                        new RuntimeException("No matching scheme rate found for amount: " + txnAmt));

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
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);
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

        log.info("[6] DTO ready for transaction processing: {}", dto);

        // ===== 7. Map Vendor Status =====
        String finalStatus = switch (status) {

            case "SUCCESS" -> "SUCCESS";

            case "FAILED" -> "FAILED";

            case "PENDING" -> "PENDING";

            default -> "UNKNOWN";
        };

        log.info("[7] Status mapped from vendor: {} -> {}", status, finalStatus);

        // ===== 8. Process Transaction =====
        transactionEngineServiceImpl.processTransaction(dto, finalStatus);

        log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                orderId, finalStatus);

        // ===== 9. Update UTR =====
        pendingTxn.setStatus(finalStatus);
        payoutPendingTransactionRepository.save(pendingTxn);

        log.info("[9] Pending transaction updated with UTR={}, STATUS={}", utr, finalStatus);

        return "SUCCESS";
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {

        log.info("Checking NixaSoft payout status for orderId={}", orderId);

        try {

            // ===== Request Body =====
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("requestId", orderId);

            // ===== Headers =====
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apiToken", credentials.getToken());

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            RestTemplate restTemplate = new RestTemplate();

            // ===== Call API =====
            ResponseEntity<NixaSoftPayoutStatusResponse> response =
                    restTemplate.exchange(
                            credentials.getBaseUrlUat() + "/check-status",
                            HttpMethod.POST,
                            entity,
                            NixaSoftPayoutStatusResponse.class
                    );

            NixaSoftPayoutStatusResponse body = response.getBody();

            log.info("NixaSoft Status Response: {}", body);

            // ===== Map Status =====
            assert body != null;
            String mappedStatus = switch (body.getStatus()) {
                case "SUCCESS" -> "SUCCESS";
                case "PENDING" -> "PENDING";
                case "FAILED" -> "FAILED";
                default -> "UNKNOWN";
            };

            // ===== Build Response =====
            PayoutStatusResponse payoutStatusResponse = new PayoutStatusResponse();

            payoutStatusResponse.setOrderId(body.getOrderId());
            payoutStatusResponse.setStatus(mappedStatus);
            payoutStatusResponse.setMessage(body.getDescription());

            return payoutStatusResponse;

        } catch (Exception e) {

            log.error("Error while checking NixaSoft payout status for orderId={}", orderId, e);

            PayoutStatusResponse response = new PayoutStatusResponse();
            response.setOrderId(orderId);
            response.setStatus("FAILED");
            response.setMessage("Vendor API Error");

            return response;
        }
    }
}
