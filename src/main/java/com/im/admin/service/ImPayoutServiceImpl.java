package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import com.im.admin.util.CommonMethods;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@Slf4j
public class ImPayoutServiceImpl implements PayoutVendorService, PayoutCallbackRegistrar, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;


    @Override
    public List<String> getVendorName() {
        return List.of("IM Payout", "IM MTB");
    }

    @Override
    @Transactional
    public PayoutResponseDto sendPayout(SendPayoutRequest req,
                                        VendorCredentials creds,
                                        String orderId,
                                        SubProducts subProduct,
                                        WalletType walletType,
                                        RequestHeaderMetaDto requestHeaderMetaDtoMeta) {
        log.info("[IM PAYOUT] Starting payout for orderId={} | amount={}",
                orderId, req.getAmount());

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            // Handle AES Token Flow (Admin Panel)
            if ("AES".equalsIgnoreCase(creds.getTokenType())) {
                return processAesPayout(req, creds, orderId, requestHeaderMetaDtoMeta);
            }

            // Direct Vendor Payout Flow
            return processDirectVendorPayout(req, creds, orderId, subProduct, requestHeaderMetaDtoMeta);

        } catch (HttpStatusCodeException ex) {
            String vendorResponse = ex.getResponseBodyAsString();

            log.error("[IM PAYOUT] Vendor API Error | orderId={} | response={}",
                    orderId, vendorResponse);

            return failed(orderId, vendorResponse, creds.getVendor().getVendorName());

        } catch (Exception e) {
            Throwable root = getRootCause(e);

            log.error("[IM PAYOUT] Internal payout error | orderId={} | error={}",
                    orderId, root.getMessage(), e);

            return failed(orderId, root.getMessage(), creds.getVendor().getVendorName());

        } finally {
            String vendorId = creds.getVendor().getId();

            commonFunctions.saveVendorLogs(
                    requestTime,
                    requestJson,
                    vendorId,
                    LocalDateTime.now(),
                    responseJson,
                    null
            );
        }
    }

     /* =====================================================
       AES PAYOUT FLOW (Matching INSTANT MUDRA Pattern)
       ===================================================== */

    private PayoutResponseDto processAesPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String baseTxnId,
            RequestHeaderMetaDto requestHeaderMetaDtoMeta) throws Exception {

        String token = commonMethods.generateTokenForAES(creds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", creds.getClientId());
        headers.set("token", token);
        headers.setBearerAuth(token);
        headers.set("latitude", String.valueOf(requestHeaderMetaDtoMeta.getLatitude()));
        headers.set("longitude", String.valueOf(requestHeaderMetaDtoMeta.getLongitude()));

        // Handle TXN prefix like Instant Mudra
        String orderId = baseTxnId.startsWith("TXN") ? baseTxnId.substring(3) : baseTxnId;

        SendPayoutRequest payoutRequest = new SendPayoutRequest();
        payoutRequest.setAmount(req.getAmount());
        payoutRequest.setOrderId(orderId);
        payoutRequest.setIfscCode(req.getIfscCode());
        payoutRequest.setAccountNumber(req.getAccountNumber());
        payoutRequest.setBankName(req.getBankName());
        payoutRequest.setPaymentMode(req.getPaymentMode());
        payoutRequest.setBeneficiaryName(req.getBeneficiaryName());
        payoutRequest.setPartnerId(creds.getUsername());

        HttpEntity<SendPayoutRequest> requestEntity = new HttpEntity<>(payoutRequest, headers);

        String prodUrl = creds.getBaseUrlProd();
        log.info("[IM PAYOUT][AES] Calling AES payout API | url={}", prodUrl);

        ResponseEntity<CommonResponse<PayoutResponseDto>> response = restTemplate.exchange(
                prodUrl + "/api/send-payout",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<CommonResponse<PayoutResponseDto>>() {}
        );

        CommonResponse<PayoutResponseDto> responseBody = response.getBody();
        log.info("[IM PAYOUT][AES] AES payout response => {}", responseBody);

        if (responseBody == null || responseBody.getData() == null) {
            return failed("TXN" + baseTxnId, "Empty response from AES vendor", creds.getVendor().getVendorName());
        }

        log.info("[IM PAYOUT][AES] AES payout completed | orderId={} | status={}",
                "TXN" + baseTxnId,
                responseBody.getData().getStatus());

        // Save pending transaction (like NixaSoft)
        savePendingTransaction(req, orderId, requestHeaderMetaDtoMeta.getLongitude(), requestHeaderMetaDtoMeta.getLatitude(), null, "PENDING");

        return PayoutResponseDto.builder()
                .orderId("TXN" + baseTxnId)
                .vendorName(creds.getVendor().getVendorName())
                .status(responseBody.getData().getStatus())
                .amount(req.getAmount())
                .message(responseBody.getMessage())
                .build();
    }

     /* =====================================================
       DIRECT INSTANT MUDRA PAYOUT FLOW
       ===================================================== */

    private PayoutResponseDto processDirectVendorPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            SubProducts subProduct,
            RequestHeaderMetaDto requestHeaderMetaDto) throws Exception {

        // Build headers
        HttpHeaders headers = buildPayoutHeaders(creds);

        // Build request body
        Map<String, Object> body = buildPayoutRequestBody(req, creds, subProduct, requestHeaderMetaDto);

        String url = creds.getBaseUrlProd() + "/payoutAPI/transfer";
        if (creds.getVendor().getVendorName().equalsIgnoreCase("IM MTB")) {
            url = creds.getBaseUrlProd() + "/move_to_bankAPI/pobr";
        }
        log.info("[IM PAYOUT] Sending payout request to: {}", url);

        // Call vendor API
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        // Handle vendor response
        return handleVendorResponse(response.getBody(), req, orderId, requestHeaderMetaDto.getLatitude(), requestHeaderMetaDto.getLongitude(), creds.getVendor().getVendorName());
    }

    /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPayoutHeaders(VendorCredentials creds) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+creds.getToken());
        log.info("<----------- Headers{} ------------>", headers);
        return headers;
    }


    /* =====================================================
       COMMON FAILED RESPONSE
       ===================================================== */

    private PayoutResponseDto failed(String orderId, String message, String vendorName) {
        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(vendorName)
                .status("FAILED")
                .message(message != null ? message : "Something went wrong")
                .build();
    }

    /* =====================================================
       ROOT CAUSE EXTRACTOR
       ===================================================== */

    private Throwable getRootCause(Throwable throwable) {
        Throwable root = throwable;

        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }

        return root;
    }

    /* =====================================================
       SAVE PENDING TRANSACTION
       ===================================================== */

    private void savePendingTransaction(
            SendPayoutRequest req,
            String orderId,
            Double latitude,
            Double longitude,
            String vendorTxRef,
            String status) {

        PayoutPendingTransactions pending = new PayoutPendingTransactions();
        pending.setTransactionId(orderId);
        pending.setSchemeCode(req.getSchemeCode());
        pending.setRetailerId(req.getRetailerId());
        pending.setAmount(req.getAmount());
        pending.setTransactionType(req.getPaymentMode());
        pending.setStatus(status);
        pending.setCreatedAt(LocalDateTime.now());
        pending.setIfscCode(req.getIfscCode());
        pending.setAccountNumber(req.getAccountNumber());
        pending.setBankName(req.getBankName());
        pending.setBeneficiaryName(req.getBeneficiaryName());
        pending.setLatitude(latitude);
        pending.setLongitude(longitude);
        pending.setTempTxnId(vendorTxRef);

        payoutPendingTransactionRepository.save(pending);
    }

     /* =====================================================
       HANDLE VENDOR RESPONSE
       ===================================================== */

    private PayoutResponseDto handleVendorResponse(
            Map<String, Object> vendorResp,
            SendPayoutRequest req,
            String orderId,
            Double latitude,
            Double longitude,
            String vendorName) {

        log.info("[IM PAYOUT] Raw response: {}", vendorResp);

        if (vendorResp == null || vendorResp.isEmpty()) {
            log.warn("[IM PAYOUT] Empty or invalid response from vendor");
            return failed(orderId, "Empty or invalid response from vendor", vendorName);
        }

        String vendorStatus = String.valueOf(vendorResp.get("status"));
        String vendorMessage = String.valueOf(vendorResp.get("msg"));
        String vendorTxnId = String.valueOf(vendorResp.get("txid"));
        String vendorData = String.valueOf(vendorResp.get("data"));
        String vendorTxRef = String.valueOf(vendorResp.get("tx_ref"));

        // ===== HANDLE BASED ON STATUS =====
        switch (vendorStatus.toLowerCase()) {
            case "success":
                return handleSuccessResponse(req, orderId, vendorTxnId, vendorTxRef,
                        vendorMessage, latitude, longitude, vendorName);

            case "waiting":
                return handleWaitingResponse(req, orderId, vendorTxnId, vendorTxRef,
                        vendorMessage, latitude, longitude, vendorName);

            case "error":
            case "failed":
                return handleErrorResponse(req, orderId, vendorTxnId, vendorTxRef,
                        vendorStatus, vendorMessage, vendorData, latitude, longitude, vendorName);

            default:
                // Unknown status - treat as PENDING
                log.warn("[IM PAYOUT] Unknown status from vendor: {}, treating as PENDING", vendorStatus);
                savePendingTransaction(req, orderId, latitude, longitude, vendorTxRef, "PENDING");
                return PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorName(vendorName)
                        .status("PENDING")
                        .message("Transaction is being processed")
                        .amount(req.getAmount())
                        .beneficiaryName(req.getBeneficiaryName())
                        .accountNumber(req.getAccountNumber())
                        .bankName(req.getBankName())
                        .ifscCode(req.getIfscCode())
                        .build();
        }
    }

    // ===== HANDLE SUCCESS RESPONSE =====
    private PayoutResponseDto handleSuccessResponse(SendPayoutRequest req, String orderId,
                                                    String vendorTxnId, String vendorTxRef,
                                                    String vendorMessage, Double latitude,
                                                    Double longitude, String vendorName) {
        log.info("[IM PAYOUT] Transaction SUCCESS | OrderID={} | VendorTxnID={}", orderId, vendorTxnId);

        savePendingTransaction(req, orderId, latitude, longitude, vendorTxRef, "PENDING");

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(vendorName)
                .status("SUCCESS")
                .message(vendorMessage != null ? vendorMessage : "Transaction completed successfully")
                .amount(req.getAmount())
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .bankName(req.getBankName())
                .ifscCode(req.getIfscCode())
                .build();
    }

    // ===== HANDLE WAITING RESPONSE =====
    private PayoutResponseDto handleWaitingResponse(SendPayoutRequest req, String orderId,
                                                    String vendorTxnId, String vendorTxRef,
                                                    String vendorMessage, Double latitude,
                                                    Double longitude, String vendorName) {
        log.info("[IM PAYOUT] Transaction PENDING | OrderID={} | VendorTxnID={}", orderId, vendorTxnId);

        savePendingTransaction(req, orderId, latitude, longitude, vendorTxRef, "PENDING");

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(vendorName)
                .status("PENDING")
                .message(vendorMessage != null ? vendorMessage : "Transaction is being processed")
                .amount(req.getAmount())
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .bankName(req.getBankName())
                .ifscCode(req.getIfscCode())
                .build();
    }

    // ===== HANDLE ERROR RESPONSE =====
    private PayoutResponseDto handleErrorResponse(SendPayoutRequest req, String orderId,
                                                  String vendorTxnId, String vendorTxRef,
                                                  String vendorStatus, String vendorMessage,
                                                  String vendorData, Double latitude,
                                                  Double longitude, String vendorName) {
        // Build error message
        String errorMessage = vendorMessage != null ? vendorMessage : "Transaction failed";

        if (vendorData != null && !"null".equalsIgnoreCase(vendorData) && !vendorData.isBlank()) {
            errorMessage = errorMessage + " - " + vendorData;
        }

        log.warn("[IM PAYOUT] Transaction FAILED | OrderID={} | Status={} | Message={}",
                orderId, vendorStatus, errorMessage);

        savePendingTransaction(req, orderId, latitude, longitude, vendorTxRef, "FAILED");

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(vendorName)
                .status("FAILED")
                .message(errorMessage)
                .amount(req.getAmount())
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .bankName(req.getBankName())
                .ifscCode(req.getIfscCode())
                .build();
    }

      /* =====================================================
       BUILD PAYOUT REQUEST BODY
       ===================================================== */

    private Map<String, Object> buildPayoutRequestBody(
            SendPayoutRequest req,
            VendorCredentials creds,
            SubProducts subProduct,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("geolocation",
                requestHeaderMetaDto.getLatitude() + "," + requestHeaderMetaDto.getLongitude());

        body.put("userid", creds.getUsername());
        body.put("clientip", requestHeaderMetaDto.getIpAddress());
        body.put("medium", requestHeaderMetaDto.getMedium() != null
                ? requestHeaderMetaDto.getMedium()
                : "rweb");
        body.put("site", "mapp");
        body.put("host", requestHeaderMetaDto.getRequestDomainName());

        body.put("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        body.put("transaction_amount", String.valueOf(req.getAmount()));
        body.put("tx_mode", req.getPaymentMode().toLowerCase());

        if (creds.getVendor().getVendorName().equalsIgnoreCase("IM MTB")) {
            body.put("productCode", "POBR");
        } else {
            body.put("subproduct", req.getImSubProductName());
        }

        Map<String, Object> bankData = new LinkedHashMap<>();
        bankData.put("bank_name", req.getBankName());
        bankData.put("ifsc_code", req.getIfscCode());
        bankData.put("account_name", req.getBeneficiaryName());
        bankData.put("account_no", req.getAccountNumber());

        body.put("bankData", bankData);

        log.info("<------------- Request Body {} --------->", body);

        return body;
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("IM Callback Received: {}", payload);

        // ===== 1. Extract Fields =====
        String status = payload.get("status");
        String vendorTxnId = payload.get("tx_ref");
        String utrNo = payload.get("utr");

        log.info("[1] Parsed Callback: orderId={}, requestId={}, status={}", vendorTxnId, vendorTxnId, status);

        // ===== 2. Fetch Pending Transaction =====
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                .findByTempTxnId(vendorTxnId)
                .orElseThrow(() ->
                        new RuntimeException("Pending transaction not found for order ID: " + vendorTxnId));

        BigDecimal amount = pendingTxn.getAmount();
        String orderId = pendingTxn.getTransactionId();

        if (amount == null) {
            throw new RuntimeException("Pending transaction amount is null for order ID: " + vendorTxnId);
        }

        // ===== Already SUCCESS check =====
        if ("SUCCESS".equalsIgnoreCase(pendingTxn.getStatus())) {
            log.warn("Transaction already marked SUCCESS for orderId={}", vendorTxnId);
            return "Transaction already processed successfully";
        }

        log.info("[2] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                pendingTxn.getAmount(), pendingTxn.getStatus());

        // Sanitize amount from callback payload (remove commas)
        Double txnAmt = amount.doubleValue();

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
        dto.setVendorName("IM Payout");
        dto.setVendorTxnId(vendorTxnId);
        dto.setUtrNo(utrNo);

        log.info("[6] DTO ready for transaction processing: {}", dto);

        // ===== 7. Map Vendor Status =====
        String finalStatus = switch (status) {

            case "completed", "SUCCESS" -> "SUCCESS";

            case "FAILED" -> "FAILED";

            case "PENDING", "waiting" -> "PENDING";

            default -> "UNKNOWN";
        };

        log.info("[7] Status mapped from vendor: {} -> {}", status, finalStatus);

        // ===== 8. Process Transaction =====
        transactionEngineServiceImpl.processTransaction(dto, finalStatus);

        log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                orderId, finalStatus);

        // ===== 9. Update UTR =====
        pendingTxn.setStatus(finalStatus);
        pendingTxn.setUtr(utrNo);
        payoutPendingTransactionRepository.save(pendingTxn);

        log.info("[9] Pending transaction updated with UTR={}, STATUS={}", vendorTxnId, finalStatus);

        return "SUCCESS";
    }


    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {
        return null;
    }

    @Override
    public PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials creds, String callbackUrl) {
        return null;
    }

}
