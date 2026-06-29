package com.im.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.PayoutPendingTransactions;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.PayoutPendingTransactionRepository;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.PayoutStatusResponse;
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
import java.util.Map;

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
                                        Double latitude,
                                        Double longitude,
                                        Object ipAddress) {
        log.info("[IM PAYOUT] Starting payout for orderId={} | amount={}",
                orderId, req.getAmount());

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            // Handle AES Token Flow (Admin Panel)
            if ("AES".equalsIgnoreCase(creds.getTokenType())) {
                return processAesPayout(req, creds, orderId, latitude, longitude);
            }

            // Direct Vendor Payout Flow
            return processDirectVendorPayout(req, creds, orderId, latitude, longitude, ipAddress);

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
       AES PAYOUT FLOW (Matching NixaSoft Pattern)
       ===================================================== */

    private PayoutResponseDto processAesPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String baseTxnId,
            Double latitude,
            Double longitude) throws Exception {

        String token = commonMethods.generateTokenForAES(creds);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("client_id", creds.getClientId());
        headers.set("token", token);
        headers.setBearerAuth(token);
        headers.set("latitude", String.valueOf(latitude));
        headers.set("longitude", String.valueOf(longitude));

        // Handle TXN prefix like NixaSoft
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
        savePendingTransaction(req, orderId, latitude, longitude);

        return PayoutResponseDto.builder()
                .orderId("TXN" + baseTxnId)
                .vendorName(getVendorName())
                .status(responseBody.getData().getStatus())
                .amount(req.getAmount())
                .message(responseBody.getMessage())
                .build();
    }

     /* =====================================================
       DIRECT RUNPAISA PAYOUT FLOW
       ===================================================== */

    private PayoutResponseDto processDirectVendorPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            Double latitude,
            Double longitude,
            Object ipAddress) throws Exception {

        // Build headers
        HttpHeaders headers = buildPayoutHeaders(creds);

        // Build request body
        MultiValueMap<String, Object> body = buildPayoutRequestBody(req, orderId, creds, ipAddress);

        String url = creds.getBaseUrlProd() + "/transfer";
        log.info("[IM PAYOUT] Sending payout request to: {}", url);

        // Call vendor API
        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Map.class
        );

        // Handle vendor response
        return handleVendorResponse(response.getBody(), req, orderId, latitude, longitude);
    }

    /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPayoutHeaders(VendorCredentials creds) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        //headers.setContentType(MediaType.MULTIPART_FORM_DATA);
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
            Double longitude) {

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

        String vendorCodeResp = (String) vendorResp.get("code");
        String vendorStatus = (String) vendorResp.get("status");
        String vendorMessage = (String) vendorResp.get("message");

        boolean accepted = "RP000".equals(vendorCodeResp)
                && "SUCCESS".equalsIgnoreCase(vendorStatus);

        if (!accepted) {
            return failed(orderId, vendorMessage);
        }

        // Save pending transaction
        savePendingTransaction(req, orderId, latitude, longitude);

        log.info("[IM PAYOUT] Transaction saved | TxnID={} | Status={}", orderId, vendorStatus);

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
            String orderId,
            VendorCredentials creds,
            Object ipAddress) {

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("transaction_amount", req.getAmount());
        body.add("tx_mode", req.getPaymentMode().toUpperCase());
        body.add("to_bank", req.getAccountNumber());
        body.add("geolocation", req.getLatitude()+","+req.getLongitude());
        body.add("userid", creds.getUsername());
        body.add("clientip", String.valueOf(ipAddress));
        body.add("medium", "rweb");
        body.add("site", "mapp");
        body.add("host", "www.nfttech.in");
        body.add("timestamp", LocalDateTime.now());
        return body;
    }

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        return "";
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
