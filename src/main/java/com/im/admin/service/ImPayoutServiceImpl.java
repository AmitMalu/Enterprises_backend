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

import java.time.LocalDateTime;
import java.util.HashMap;
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
    public String getVendorName() {
        return "IM Payout";
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

            return failed(orderId, vendorResponse);

        } catch (Exception e) {
            Throwable root = getRootCause(e);

            log.error("[IM PAYOUT] Internal payout error | orderId={} | error={}",
                    orderId, root.getMessage(), e);

            return failed(orderId, root.getMessage());

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
            return failed("TXN" + baseTxnId, "Empty response from AES vendor");
        }

        log.info("[IM PAYOUT][AES] AES payout completed | orderId={} | status={}",
                "TXN" + baseTxnId,
                responseBody.getData().getStatus());

        // Save pending transaction (like NixaSoft)
        savePendingTransaction(req, orderId, requestHeaderMetaDtoMeta.getLongitude(), requestHeaderMetaDtoMeta.getLatitude(), null);

        return PayoutResponseDto.builder()
                .orderId("TXN" + baseTxnId)
                .vendorName(getVendorName())
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
        MultiValueMap<String, Object> body = buildPayoutRequestBody(req, creds, subProduct, requestHeaderMetaDto);

        String url = creds.getBaseUrlProd() + "/payoutAPI/transfer";
        log.info("[IM PAYOUT] Sending payout request to: {}", url);

        // Call vendor API
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        // Handle vendor response
        return handleVendorResponse(response.getBody(), req, orderId, requestHeaderMetaDto.getLatitude(), requestHeaderMetaDto.getLongitude());
    }

    /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPayoutHeaders(VendorCredentials creds) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer "+creds.getToken());
        return headers;
    }


    /* =====================================================
       COMMON FAILED RESPONSE
       ===================================================== */

    private PayoutResponseDto failed(String orderId, String message) {
        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(getVendorName())
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
            String vendorTxRef ) {

        PayoutPendingTransactions pending = new PayoutPendingTransactions();
        pending.setTransactionId(orderId);
        pending.setSchemeCode(req.getSchemeCode());
        pending.setRetailerId(req.getRetailerId());
        pending.setAmount(req.getAmount());
        pending.setTransactionType(req.getPaymentMode());
        pending.setStatus("PENDING");
        pending.setCreatedAt(LocalDateTime.now());
        pending.setIfscCode(req.getIfscCode());
        pending.setAccountNumber(req.getAccountNumber());
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
            Double longitude) {

        log.info("[IM PAYOUT] Raw response: {}", vendorResp);

        if (vendorResp == null || vendorResp.isEmpty()) {
            log.warn("[IM PAYOUT] Empty or invalid response from vendor");
            return failed(orderId, "Empty or invalid response from vendor");
        }

        String vendorStatus = String.valueOf(vendorResp.get("status"));
        String vendorMessage = String.valueOf(vendorResp.get("msg"));
        String vendorTxnId = String.valueOf(vendorResp.get("txid"));
        String vendorData = String.valueOf(vendorResp.get("data"));
        String vendorTxRef = String.valueOf(vendorResp.get("tx_ref"));

        if (vendorTxRef != null && !"null".equalsIgnoreCase(vendorTxRef)) {
            orderId = vendorTxRef;
        }

        boolean accepted = "waiting".equalsIgnoreCase(vendorStatus);

        if (!accepted) {
            String errorMessage = vendorMessage;

            if (vendorData != null && !"null".equalsIgnoreCase(vendorData) && !vendorData.isBlank()) {
                errorMessage = vendorMessage + " - " + vendorData;
            }

            log.warn("[IM PAYOUT] Vendor rejected transaction | TxnID={} | Status={} | Message={}",
                    orderId, vendorStatus, errorMessage);

            return failed(orderId, errorMessage);
        }

        savePendingTransaction(req, orderId, latitude, longitude, vendorTxRef);

        log.info("[IM PAYOUT] Transaction saved | TxnID={} | VendorTxnID={} | Status={}",
                orderId, vendorTxnId, vendorStatus);

        return PayoutResponseDto.builder()
                .orderId(orderId)
                .vendorName(getVendorName())
                .status("PENDING")
                .message(vendorMessage)
                .amount(req.getAmount())
                .beneficiaryName(req.getBeneficiaryName())
                .accountNumber(req.getAccountNumber())
                .ifscCode(req.getIfscCode())
                .build();
    }

      /* =====================================================
       BUILD PAYOUT REQUEST BODY
       ===================================================== */

    private MultiValueMap<String, Object> buildPayoutRequestBody(
            SendPayoutRequest req,
            VendorCredentials creds,
            SubProducts subProduct,
            RequestHeaderMetaDto requestHeaderMetaDto) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("geolocation",
                requestHeaderMetaDto.getLatitude() + "," + requestHeaderMetaDto.getLongitude());

        body.add("userid", creds.getUsername());
        body.add("clientip", requestHeaderMetaDto.getIpAddress());
        body.add("medium", requestHeaderMetaDto.getMedium());
        body.add("site", "mapp");
        body.add("host", requestHeaderMetaDto.getRequestDomainName());

        body.add("timestamp",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        body.add("transaction_amount", req.getAmount());
        body.add("tx_mode", req.getPaymentMode().toLowerCase());

        body.add("subproduct", subProduct.getSubProductTitle());

        Map<String, Object> bankData = new HashMap<>();
        bankData.put("bank_name", req.getBankName());
        bankData.put("ifsc_code", req.getIfscCode());
        bankData.put("account_name", req.getBeneficiaryName());
        bankData.put("account_no", req.getAccountNumber());

        body.add("bankData", bankData);

        log.info("Request Body" + body.toString());

        return body;
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("NixaSoft Callback Received: {}", payload);

        // ===== 1. Extract Fields =====
        String status = payload.get("status");
        String type = payload.get("type");
        String orderId = payload.get("requestId");
        String vendorTxnId = payload.get("orderId");
        String amount = payload.get("amount");
        String utr = payload.get("utr");
        String description = payload.get("description");

        log.info("[1] Parsed Callback: orderId={}, requestId={}, status={}, amount={}, utr={}",
                vendorTxnId, orderId, status, amount, utr);

        // ===== 2. Fetch Pending Transaction =====
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository
                .findByTransactionId(orderId)
                .orElseThrow(() ->
                        new RuntimeException("Pending transaction not found for order ID: " + orderId));

        // ===== Already SUCCESS check =====
        if ("SUCCESS".equalsIgnoreCase(pendingTxn.getStatus())) {
            log.warn("Transaction already marked SUCCESS for orderId={}", orderId);
            return "Transaction already processed successfully";
        }

        log.info("[2] PendingTxn fetched successfully: ID={}, SchemeCode={}, TxnType={}, Amount={}, Status={}",
                pendingTxn.getId(), pendingTxn.getSchemeCode(), pendingTxn.getTransactionType(),
                pendingTxn.getAmount(), pendingTxn.getStatus());

        // Sanitize amount from callback payload (remove commas)
        String cleanCallbackAmount = amount != null ? amount.replace(",", "") : "0";
        Double txnAmt = Double.valueOf(cleanCallbackAmount);

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
        dto.setVendorName("Nixasoft Payout");
        dto.setVendorTxnId(utr);

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
        return null;
    }

    @Override
    public PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials creds, String callbackUrl) {
        return null;
    }

}
