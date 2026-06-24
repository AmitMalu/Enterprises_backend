package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.controller.AESController;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.PayoutCallbackHandler;
import com.im.admin.service.PayoutCallbackRegistrar;
import com.im.admin.service.PayoutVendorService;
import com.im.admin.service.VendorStatusChecker;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.VimoPayCryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@Slf4j
public class VimoPayoutServiceImpl implements PayoutVendorService, PayoutCallbackRegistrar, PayoutCallbackHandler, VendorStatusChecker {

    @Autowired
    private NetworkBankMasterRepository networkBankMasterRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AESController aesController;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorDataSyncServiceImpl vendorDataSyncServiceImpl;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private VimoPayStateListRepository vimoPayStateListRepository;

    @Autowired
    private VimoPayCryptoUtil vimoPayCryptoUtil;

    @Override
    public String getVendorName() {
        return "VidualPay Payout";
    }


    @Override
    public PayoutResponseDto sendPayout(SendPayoutRequest req, VendorCredentials creds, String orderId, SubProducts subProducts, WalletType walletType, Double latitude, Double longitude) {
        log.info("[VIMOPAY] Starting payout for orderId={} | amount={}",
                orderId, req.getAmount());

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            // Handle AES Token Flow (Admin Panel)
            if ("AES".equalsIgnoreCase(creds.getTokenType())) {
                //return processAesPayout(req, creds, orderId, latitude, longitude);
            }

            // Direct Vendor Payout Flow
            return processDirectVendorPayout(req, creds, orderId, latitude, longitude);

        } catch (HttpStatusCodeException ex) {
            String vendorResponse = ex.getResponseBodyAsString();

            log.error("[VIMOPAY] Vendor API Error | orderId={} | response={}",
                    orderId, vendorResponse);

            return failed(orderId, vendorResponse);

        } catch (Exception e) {
            Throwable root = getRootCause(e);

            log.error("[VIMOPAY] Internal payout error | orderId={} | error={}",
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

    private PayoutResponseDto processDirectVendorPayout(
            SendPayoutRequest req,
            VendorCredentials creds,
            String orderId,
            Double latitude,
            Double longitude
    ) throws Exception {

        log.info("===== STARTING VIMOPAY PAYOUT =====");

        String url =
                creds.getBaseUrlUat()
                        + "/payoutapi/api/payment/payoutsuat";

        log.info("URL {} ---> ", url);

        /*
         * Build Headers
         */
        HttpHeaders headers =
                buildPayoutHeaders(creds);

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        /*
         * Build Plain Payload
         */
        Map<String, Object> plainPayload =
                buildPayoutRequestBody(
                        req,
                        orderId,
                        creds
                );

        String plainJson =
                objectMapper.writeValueAsString(
                        plainPayload
                );

        log.info(
                "Plain Payout Payload={}",
                plainJson
        );

        /*
         * Encrypt payload
         */
        String encrypted =
                vimoPayCryptoUtil.encryptForVendor(
                        creds,
                        plainJson
                );

        log.info("Encrypted payload generated {}", encrypted);

        /*
         * Final request
         * {
         *   requestBody: encryptedString
         * }
         */
        Map<String, Object> finalBody =
                new HashMap<>();

        finalBody.put(
                "requestBody",
                encrypted
        );

        log.info(
                "Final Request={}",
                objectMapper.writeValueAsString(
                        finalBody
                )
        );

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(
                                finalBody,
                                headers
                        ),
                        Map.class
                );

        log.info(
                "Vendor Raw Response={}",
                response.getBody()
        );

        return handleVendorResponse(
                response.getBody(),
                req,
                orderId,
                latitude,
                longitude
        );
    }

     /* =====================================================
       BUILD PAYOUT HEADERS
       ===================================================== */

    private HttpHeaders buildPayoutHeaders(
            VendorCredentials creds
    ) {

        log.info("===== Building Payout Headers =====");

        String token =
                vendorDataSyncServiceImpl.callTokenApi(creds);

        HttpHeaders headers = new HttpHeaders();

        // DOC: Authorization: Bearer <token>
        headers.setBearerAuth(token);

        headers.set(
                "userId",
                creds.getUsername()
        );

        log.info("Headers{} ----> ", headers);
        log.info("Payout Headers Prepared");
        log.info("userId={}", creds.getUsername());
        log.info("Authorization=Bearer [TOKEN]");

        return headers;
    }


    private Map<String, Object> buildPayoutRequestBody(
            SendPayoutRequest req,
            String orderId,
            VendorCredentials creds) {

        log.info("===== Building Payout Request =====");

        Retailer retailer =
                retailerRepository
                        .findByRetailerId(req.getRetailerId())
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Retailer not found : "
                                                + req.getRetailerId()
                                ));

        /*
         * Resolve State Code
         */
        String beneficiaryLocation = "MH";

        try {

            String retailerState =
                    retailer.getAddress().getState();

            log.info(
                    "Retailer State={}",
                    retailerState
            );

            if (retailerState != null
                    && !retailerState.isBlank()) {

                beneficiaryLocation =
                        vimoPayStateListRepository
                                .findByStateNameIgnoreCase(
                                        retailerState
                                )
                                .map(
                                        VimoPayStateList::getStateCode
                                )
                                .orElse("MH");
            }

        } catch (Exception ex) {

            log.warn(
                    "Unable to fetch state code. Using default MH"
            );
        }


        /*
         * Resolve Bank Code from IFSC
         */
        String beneficiaryBank = "001"; // default

        try {

            String ifsc =
                    req.getIfscCode();

            log.info(
                    "Searching bank code for IFSC={}",
                    ifsc
            );

            beneficiaryBank =
                    networkBankMasterRepository
                            .findByIfscCodeIgnoreCase(ifsc)
                            .map(NetworkBankMaster::getVidualBankCode)
                            .map(code -> String.format("%03d", code))
                            .orElse("001");

            log.info(
                    "Resolved beneficiaryBank={}",
                    beneficiaryBank
            );

        } catch (Exception ex) {

            log.warn(
                    "Unable to fetch bank code. Using default 001"
            );
        }

        Map<String, Object> body =
                new LinkedHashMap<>();

        body.put(
                "amount",
                req.getAmount()
        );

        body.put(
                "merchantRefId",
                orderId
        );

        body.put(
                "beneficiaryBank",
                beneficiaryBank
        );

        body.put(
                "paymentPurpose",
                "004"
        );

        body.put(
                "paymentMode",
                req.getPaymentMode()
                        .toLowerCase()
        );

        body.put(
                "beneficiaryAccountNumber",
                req.getAccountNumber()
        );

        body.put(
                "beneficiaryIFSC",
                req.getIfscCode()
        );

        body.put(
                "beneficiaryMobileNumber",
                retailer.getMobileNumber()
        );

        body.put(
                "beneficiaryLocation",
                beneficiaryLocation
        );

        body.put(
                "beneficiaryName",
                req.getBeneficiaryName()
        );

        body.put(
                "lat",
                retailer.getLatitude()
        );

        body.put(
                "long",
                retailer.getLongitude()
        );

        body.put("udf1", "");

        body.put("udf2", "");

        body.put("udf3", "");

        log.info("===== Final Payout Request =====");
        log.info(body.toString());
        log.info("amount={}", req.getAmount());
        log.info("merchantRefId={}", orderId);
        log.info("beneficiaryBank={}", beneficiaryBank);
        log.info("paymentMode={}", req.getPaymentMode());
        log.info("beneficiaryLocation={}", beneficiaryLocation);
        log.info("beneficiaryName={}", req.getBeneficiaryName());
        log.info("beneficiaryMobile={}", retailer.getMobileNumber());
        log.info("Lat={}", retailer.getLatitude());
        log.info("Long={}", retailer.getLongitude());

        return body;
    }

    private PayoutResponseDto handleVendorResponse(
            Map<String, Object> vendorResp,
            SendPayoutRequest req,
            String orderId,
            Double latitude,
            Double longitude
    ) {

        log.info("===== HANDLE VIMOPAY RESPONSE =====");
        log.info("Vendor Response={}", vendorResp);

        if (vendorResp == null || vendorResp.isEmpty()) {

            log.warn("Empty vendor response");

            return failed(
                    orderId,
                    "Empty response from vendor"
            );
        }

        String txnStatus =
                String.valueOf(
                        vendorResp.getOrDefault(
                                "txnStatus",
                                ""
                        )
                );

        String txnStatusCode =
                String.valueOf(
                        vendorResp.getOrDefault(
                                "txnStatusCode",
                                ""
                        )
                );

        String responseMessage =
                String.valueOf(
                        vendorResp.getOrDefault(
                                "responseMessage",
                                "No response"
                        )
                );

        String vendorTxnId =
                String.valueOf(
                        vendorResp.getOrDefault(
                                "txnId",
                                ""
                        )
                );

        log.info("txnStatus={}", txnStatus);
        log.info("txnStatusCode={}", txnStatusCode);
        log.info("txnId={}", vendorTxnId);
        log.info("responseMessage={}", responseMessage);

        /*
         * Vimo:
         * 004 = Queued
         */

        boolean accepted =
                "004".equals(txnStatusCode)
                        || "QUEUED".equalsIgnoreCase(txnStatus);

        if (!accepted) {

            log.warn(
                    "Payout rejected. Status={} Code={}",
                    txnStatus,
                    txnStatusCode
            );

            return failed(
                    orderId,
                    responseMessage
            );
        }

        /*
         * Save pending txn
         */
        savePendingTransaction(
                req,
                orderId,
                latitude,
                longitude
        );

        log.info(
                "Transaction saved successfully | orderId={} vendorTxnId={}",
                orderId,
                vendorTxnId
        );

        return PayoutResponseDto
                .builder()
                .orderId(orderId)

                .vendorName(
                        getVendorName()
                )

                .status(
                        "PENDING"
                )

                .message(
                        responseMessage
                )

                .amount(
                        req.getAmount()
                )

                .beneficiaryName(
                        req.getBeneficiaryName()
                )

                .accountNumber(
                        req.getAccountNumber()
                )

                .ifscCode(
                        req.getIfscCode()
                )

                // optional if DTO has field
//                .vendorTransactionId(
//                        vendorTxnId
//                )

                .build();
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

    @Override
    public String handleCallback(Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        return "";
    }

    @Override
    public PayoutMerchantCallbackResponse registerCallbackUrl(VendorCredentials creds, String callbackUrl) {
        return null;
    }

    @Override
    public PayoutStatusResponse checkPayoutStatus(String orderId, VendorCredentials credentials) {
        return null;
    }
}
