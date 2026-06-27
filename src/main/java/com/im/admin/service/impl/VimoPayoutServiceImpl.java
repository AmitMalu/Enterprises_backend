package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.controller.AESController;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
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

        log.info("URL {} ", url);

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
                        latitude,
                        longitude
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

        Map<String, Object> rawResponse =
                response.getBody();

        log.info(
                "Vendor Raw Response={}",
                rawResponse
        );

        if (rawResponse == null) {
            return failed(
                    orderId,
                    "Empty response from vendor"
            );
        }

        /*
         * Validate top-level response
         */
        String responseCode =
                String.valueOf(
                        rawResponse.getOrDefault(
                                "responseCode",
                                ""
                        )
                );

        Boolean success =
                Boolean.parseBoolean(
                        String.valueOf(
                                rawResponse.getOrDefault(
                                        "successStatus",
                                        false
                                )
                        )
                );

        if (!success || !"000".equals(responseCode)) {

            return failed(
                    orderId,
                    String.valueOf(
                            rawResponse.getOrDefault(
                                    "message",
                                    "Vendor request failed"
                            )
                    )
            );
        }

        /*
         * Extract encrypted data
         */
        String encryptedData =
                String.valueOf(
                        rawResponse.get("data")
                );

        log.info(
                "Encrypted Response Data={}",
                encryptedData
        );

        /*
         * Decrypt
         */
        String decryptedJson =
                vimoPayCryptoUtil.decryptFromVendor(
                        creds,
                        encryptedData
                );

        log.info(
                "Decrypted Vendor Response={}",
                decryptedJson
        );

        /*
         * Convert JSON → Map
         */
        Map<String, Object> decryptedResponse =
                objectMapper.readValue(
                        decryptedJson,
                        new TypeReference<Map<String, Object>>() {}
                );

        /*
         * Send decrypted response
         */
        return handleVendorResponse(
                decryptedResponse,
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
            Double latitude,
            Double longitude) {

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
                latitude.toString()
        );

        body.put(
                "long",
                longitude.toString()
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
        log.info("Lat={}", latitude);
        log.info("Long={}", longitude);

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
         * Accepted states:
         * 000 = Success
         * 004 = Queued
         */
        boolean accepted =
                "000".equals(txnStatusCode)
                        || "004".equals(txnStatusCode);

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
                        "000".equals(txnStatusCode)
                                ? "SUCCESS"
                                : "PENDING"
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
        log.info("NixaSoft Callback Received: {}", payload);

        // ===== 1. Extract Fields =====
        String status = payload.get("txnStatus");
        String txnStatusCode = payload.get("txnStatusCode");
        String orderId = payload.get("merchantRefId");
        String vendorTxnId = payload.get("txnId");
        String amount = payload.get("amount");
        String rrn = payload.get("rrn");
        String responseMessage = payload.get("responseMessage");

        log.info("[1] Parsed Callback: orderId={}, requestId={}, status={}, amount={}, utr={}",
                vendorTxnId, orderId, status, amount, rrn);

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
        dto.setVendorName("VimoPay Payout");
        dto.setVendorTxnId(vendorTxnId);

        log.info("[6] DTO ready for transaction processing: {}", dto);

        // ===== 7. Map Vendor Status =====
        String finalStatus = switch (txnStatusCode) {

            case "000" -> "SUCCESS"; // Success

            case "001" -> "FAILED"; // Failed

            case "002" -> "PENDING"; // Pending

            case "003" -> "FAILED"; // Validation Failed

            case "004" -> "PENDING"; // Queued

            default -> throw new RuntimeException(
                    "Unknown vendor status code: " + txnStatusCode
            );
        };

        log.info(
                "[7] Status mapped | code={} description={} finalStatus={}",
                txnStatusCode,
                status,
                finalStatus
        );

        log.info("[7] Status mapped from vendor: {} -> {}", status, finalStatus);

        // ===== 8. Process Transaction =====
        transactionEngineServiceImpl.processTransaction(dto, finalStatus);

        log.info("[8] Transaction processed successfully for ORDER_ID: {}, STATUS={}",
                orderId, finalStatus);

        // ===== 9. Update UTR =====
        pendingTxn.setStatus(finalStatus);
        payoutPendingTransactionRepository.save(pendingTxn);

        log.info("[9] Pending transaction updated with RRN={}, STATUS={}", rrn, finalStatus);

        return "SUCCESS";
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
