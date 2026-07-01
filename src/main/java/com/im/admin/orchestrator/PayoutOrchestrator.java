package com.im.admin.orchestrator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.response.PayoutStatusResponse;
import com.im.admin.service.*;
import com.im.admin.service.impl.TransactionEngineServiceImpl;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.PayoutVendorFactory;
import com.im.admin.util.VimoPayCryptoUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutOrchestrator {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private PayoutVendorFactory vendorFactory;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private VimoPayCryptoUtil vimoPayCryptoUtil;

    @Autowired
    private ObjectMapper objectMapper;


    @Transactional
    public CommonResponse<PayoutResponseDto> processPayout(SendPayoutRequest req, Double latitude, Double longitude, Object ipaddress) {
        String orderId = "TXN" + commonMethods.generateBaseTransactionId();
        log.info("[PROCESS PAYOUT ORCH] Initiating payout process for orderId={}", orderId);

        try {

            //Validate Scheme Active or Not
            Scheme scheme = schemeRepository.findBySchemeCode(req.getSchemeCode())
                    .orElseThrow(() -> new RuntimeException("Scheme is not activated. Please contact admin"));

            if (!Boolean.TRUE.equals(scheme.getIsActive())) {
                log.warn("Scheme is inactive for schemeCode={}", req.getSchemeCode());
                throw new RuntimeException("Scheme is inactive. Please contact admin");
            }

            // Validate SubProduct
            SubProducts subProducts = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new RuntimeException("SubProduct not found for ID: " + req.getSubProductId()));

            WalletType walletType = WalletType.valueOf(subProducts.getWalletType());
            log.debug("[PROCESS PAYOUT ORCH] Resolved walletType={}", walletType);

            // 2. Check Retailer Balance
            BigDecimal availableBalance = commonMethods.getCurrentBalance(req.getRetailerId(), walletType.name());

            // Fetch matching SchemeRate
            log.info("[2] Searching SchemeRate for schemeCode={}, operatorName={}, amount={}",
                    req.getSchemeCode(), req.getPaymentMode(), req.getAmount());

            SchemeRate selectedScheme = schemeRateRepository
                    .findSchemeRateByCodeAndOperatorAndAmount(
                            req.getSchemeCode(),
                            req.getPaymentMode(),
                            req.getAmount().doubleValue())
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "No matching scheme rate found for amount: " + req.getAmount()));

            log.info("[2.1] Found SchemeRate: ID={}, RateType={}, SlabType={}, Gross={}, RetailerRate={}",
                    selectedScheme.getId(),
                    selectedScheme.getRateType(),
                    selectedScheme.getSlabType(),
                    selectedScheme.getGross(),
                    selectedScheme.getRetailer());

            BigDecimal leanAmount =
                    commonMethods.getLeanAmount(req.getRetailerId(), walletType.name());

            BigDecimal txnAmount = req.getAmount();

            BigDecimal grossAmount =
                    BigDecimal.valueOf(selectedScheme.getGross());

            BigDecimal finalDeductAmt =
                    txnAmount.add(grossAmount);

            // -------------------------------------
            // 3. INSUFFICIENT WALLET BALANCE
            // -------------------------------------
            if (availableBalance.compareTo(finalDeductAmt) < 0) {

                log.warn(
                        "[PROCESS PAYOUT ORCH] Insufficient wallet balance | retailer={} | required={} | available={}",
                        req.getRetailerId(),
                        finalDeductAmt,
                        availableBalance
                );

                return new CommonResponse<>(
                        400,
                        "Insufficient Balance In " + subProducts.getWalletType(),
                        null
                );
            }


            // -------------------------------------
            // 1. ONLY LEAN AMOUNT VALIDATION
            // -------------------------------------
            BigDecimal balanceAfterTxn =
                    availableBalance.subtract(txnAmount);

            if (balanceAfterTxn.compareTo(leanAmount) < 0) {

                log.warn(
                        "[PROCESS PAYOUT ORCH] Lean amount failed | retailer={} | available={} | txn={} | lean={}",
                        req.getRetailerId(),
                        availableBalance,
                        txnAmount,
                        leanAmount
                );

                return new CommonResponse<>(
                        400,
                        String.format(
                                "Transaction not allowed. ₹%s must remain as lean balance.",
                                leanAmount
                        ),
                        null
                );
            }


            // -------------------------------------
            // 2. LEAN + GROSS VALIDATION
            // -------------------------------------
            BigDecimal balanceAfterGross =
                    availableBalance.subtract(finalDeductAmt);

            if (balanceAfterGross.compareTo(leanAmount) < 0) {

                log.warn(
                        "[PROCESS PAYOUT ORCH] Lean + Gross validation failed | retailer={} | available={} | finalDeduct={} | lean={}",
                        req.getRetailerId(),
                        availableBalance,
                        finalDeductAmt,
                        leanAmount
                );

                return new CommonResponse<>(
                        400,
                        String.format(
                                "Insufficient usable balance after charges. ₹%s must remain locked as lean amount.",
                                leanAmount
                        ),
                        null
                );
            }

            // 3. Resolve Vendor Routing & Credentials
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                log.error("[PROCESS PAYOUT ORCH] Vendor routing not found for subProductId={}", req.getSubProductId());
                return new CommonResponse<>(404, "Vendor routing not found", null);
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials credentials = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            // 4. Get vendor bean
            PayoutVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
            if (vendorService == null) {
                log.error("[PROCESS PAYOUT ORCH] No vendor implementation for code={}", vendor.getVendorCode());
                return new CommonResponse<>(500, "Vendor implementation not found", null);
            }

            // 5. CENTRALIZED WALLET DEBIT (transaction engine)
            AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), req.getRetailerId());
            String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

            commonMethods.updateWallet(
                    walletAmountTable,
                    req.getRetailerId(),
                    walletType,
                    req.getAmount(),
                    "Debit",
                    baseTxnId,
                    "PENDING",
                    "DEBIT",
                    subProducts.getSubProductCode(),
                    req.getBeneficiaryName() + " #" + req.getAccountNumber(),
                    "Fund Transferred #" + req.getAccountNumber(),
                    "",
                    latitude,
                    longitude
            );

            log.debug("[PROCESS PAYOUT ORCH] Wallet debited (PENDING) for retailer={}, amount={}", req.getRetailerId(), req.getAmount());

            // 6. Call vendor-specific implementation (must return PayoutResponseDto)
            PayoutResponseDto vendorResp = vendorService.sendPayout(req, credentials, orderId, subProducts, walletType, latitude, longitude, ipaddress);

            if (vendorResp == null) {
                // defensive fallback
                vendorResp = PayoutResponseDto.builder()
                        .orderId(orderId)
                        .vendorName(vendor.getVendorName())
                        .status("FAILED")
                        .message("Empty response from vendor")
                        .amount(req.getAmount())
                        .build();
            }

            log.info("[PROCESS PAYOUT ORCH] Saved pending txn: {}, vendorStatus={}", orderId, vendorResp.getStatus());

            // 8. Map vendor status to common HTTP-like status code
            int statusCode = mapVendorStatusToHttpCode(vendorResp.getStatus());

            // 9. Build and return unified CommonResponse
            return CommonResponse.<PayoutResponseDto>builder()
                    .statusCode(statusCode)
                    .message(vendorResp.getMessage() != null ? vendorResp.getMessage() : "Processed")
                    .data(vendorResp)
                    .build();

        } catch (Exception e) {

            Throwable root = e;

            while (root.getCause() != null) {
                root = root.getCause();
            }

            log.error("ROOT CAUSE => ", root);
            // optionally reverse wallet debit if you want — implement compensation logic here
            return CommonResponse.<PayoutResponseDto>builder()
                    .statusCode(500)
                    .message("Internal Server Error: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

    @Transactional
    public CommonResponse<PayoutMerchantCallbackResponse> registerCallbackUrl(SendPayoutRequest req) {
        try {
            log.info("[ORCH] Starting callback registration for subProductId={}, retailerId={}",
                    req.getSubProductId(), req.getRetailerId());

            // Validate SubProduct
            SubProducts subProduct = subProductsRepository.findById(req.getSubProductId())
                    .orElseThrow(() -> new IllegalArgumentException("SubProduct not found for ID: " + req.getSubProductId()));

            WalletType walletType = WalletType.valueOf(subProduct.getWalletType());
            log.debug("[ORCH] WalletType resolved: {}", walletType);

            // Resolve Vendor Routing
            VendorRouting routing = vendorRoutingRepository.findBySubProductId(req.getSubProductId());
            if (routing == null) {
                return new CommonResponse<>(404, "Vendor routing not found", null);
            }

            Vendors vendor = CommonMethods.getVendors(routing);
            VendorCredentials creds = vendorCredentialsRepository
                    .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Vendor credentials not found"));

            // Get vendor implementation
            PayoutVendorService vendorService = vendorFactory.getVendor(vendor.getVendorName());
            if (vendorService == null) {
                return new CommonResponse<>(500, "Vendor implementation not found", null);
            }

            // Check if vendor supports callback registration
            if (vendorService instanceof PayoutCallbackRegistrar registrar) {
                PayoutMerchantCallbackResponse callbackResponse =
                        registrar.registerCallbackUrl(creds, creds.getCallbackUrl());

                log.info("[ORCH] Callback registered for vendor={} | callbackUrl={}",
                        vendor.getVendorName(), creds.getCallbackUrl());

                return new CommonResponse<>(200, "Callback registered successfully", callbackResponse);
            } else {
                log.info("[ORCH] Vendor={} does not require callback registration.", vendor.getVendorName());
                return new CommonResponse<>(200, "Vendor does not require callback registration", null);
            }

        } catch (IllegalArgumentException e) {
            log.warn("[ORCH] Validation error: {}", e.getMessage());
            return new CommonResponse<>(400, e.getMessage(), null);
        } catch (Exception e) {
            log.error("[ORCH] Unexpected error during callback registration: {}", e.getMessage(), e);
            return new CommonResponse<>(500, "Internal Server Error: " + e.getMessage(), null);
        }
    }

    // helper
    private int mapVendorStatusToHttpCode(String vendorStatus) {
        if (vendorStatus == null) return 500;
        return switch (vendorStatus.toUpperCase()) {
            case "SUCCESS" -> 200;
            case "PENDING", "ACCEPTED" -> 202;
            case "FAILED" -> 400;
            default -> 500;
        };
    }


    @Transactional
    public String processPayoutCallback(String vendorCode, Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();
        log.info("Runpaisa Old Payout [ORCH_CALLBACK] Received callback from vendor={} IP={} Payload={}", vendorCode, clientIp, payload);

        try {
            Object vendorService = vendorFactory.getVendor(vendorCode);
            if (!(vendorService instanceof PayoutCallbackHandler handler)) {
                log.error("-Runpaisa Old Payout [ORCH_CALLBACK] No callback handler implemented for vendor={}", vendorCode);
                return "Vendor callback handler not found";
            }

            String vendorResponse = handler.handleCallback(payload, request);

            // Log callback event for auditing
            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Callback processed for vendor = " + vendorCode,
                    LocalDateTime.now()
            );

            log.info("Runpaisa Old Payout [ORCH_CALLBACK] Successfully processed callback for vendor={}", vendorCode);
            return vendorResponse;

        } catch (Exception e) {
            log.error("Runpaisa Old Payout [ORCH_CALLBACK] Error processing callback for vendor={} | Error={}", vendorCode, e.getMessage(), e);

            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Error: " + e.getMessage(),
                    LocalDateTime.now()
            );

            return "Callback processing failed: " + e.getMessage();
        }
    }

    @Transactional
    public String unisusPayoutCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        String clientRefNo = payload.get("ClientRefNo");
        String status = payload.get("Status");
        String statusMsg = payload.get("StatusMsg");
        String trnId = payload.get("TrnID");
        String bankRefNo = payload.get("BankRefNo");
        String amount = payload.get("TTC");

        log.info("[1] Fetching pending transaction for ORDER_ID: {}", trnId);
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(trnId)
                .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + trnId));

        Optional<Scheme> scheme = schemeRepository.findBySchemeCode(pendingTxn.getSchemeCode());

        // Parse scheme combination hash code
        String[] hashParts = scheme.get().getSchemeCombinationHashCode().split("-");
        if (hashParts.length < 2) {
            throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.get().getSchemeCombinationHashCode());
        }

            String vendorId = hashParts[2];

        Optional<Vendors> vendor = vendorRepository.findById(vendorId);
        String vendorCode = vendor.get().getVendorCode();

        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();
        log.info("[ORCH_CALLBACK] Received callback from vendor={} IP={} Payload={}", vendorCode, clientIp, payload);

        try {
            Object vendorService = vendorFactory.getVendor(vendorCode);
            if (!(vendorService instanceof PayoutCallbackHandler handler)) {
                log.error("[ORCH_CALLBACK] No callback handler implemented for vendor={}", vendorCode);
                return "Vendor callback handler not found";
            }

            String vendorResponse = handler.handleCallback(payload, request);

            // Log callback event for auditing
            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Callback processed for vendor = " + vendorCode,
                    LocalDateTime.now()
            );

            log.info("[ORCH_CALLBACK] Successfully processed callback for vendor={}", vendorCode);
            return vendorResponse;

        } catch (Exception e) {
            log.error("[ORCH_CALLBACK] Error processing callback for vendor={} | Error={}", vendorCode, e.getMessage(), e);

            commonFunctions.saveCallbackEventLog(
                   payload.toString(),
                    receivedAt,
                    clientIp,
                    "Error: " + e.getMessage(),
                    LocalDateTime.now()
            );

            return "Callback processing failed: " + e.getMessage();
        }
    }


    @Transactional
    public String nixasoftPayoutCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        log.info("============== NIXASOFT PAYOUT CALLBACK START ==============");

        // ===== Extract Payload Fields =====
        String status = payload.get("status");
        String type = payload.get("type");
        String trnId = payload.get("requestId");
        String orderId = payload.get("orderId");
        String amount = payload.get("amount");
        String utr = payload.get("utr");
        String description = payload.get("description");

        // ===== Fetch Pending Transaction =====
        log.info("[2] Fetching pending transaction using transactionId={}", trnId);

        PayoutPendingTransactions pendingTxn =
                payoutPendingTransactionRepository.findByTransactionId(trnId)
                        .orElseThrow(() -> {
                            log.error("[2.1] Pending transaction NOT FOUND for transactionId={}", trnId);
                            return new RuntimeException(
                                    "Pending transaction not found for order ID: " + trnId
                            );
                        });

        log.info("[2.2] Pending transaction found successfully");
        log.info("    Scheme Code      : {}", pendingTxn.getSchemeCode());
        log.info("    Retailer Id      : {}", pendingTxn.getRetailerId());
        log.info("    Amount           : {}", pendingTxn.getAmount());

        // ===== Already SUCCESS Check =====
        if ("SUCCESS".equalsIgnoreCase(pendingTxn.getStatus())) {

            log.warn("[3] Transaction already processed successfully");
            log.warn("    transactionId : {}", trnId);

            return "Transaction already processed successfully";
        }

        // ===== Fetch Scheme =====
        log.info("[4] Fetching scheme using schemeCode={}", pendingTxn.getSchemeCode());

        Scheme scheme = schemeRepository.findBySchemeCode(pendingTxn.getSchemeCode())
                .orElseThrow(() -> {
                    log.error("[4.1] Scheme NOT FOUND for schemeCode={}",
                            pendingTxn.getSchemeCode());

                    return new RuntimeException(
                            "Scheme not found for code: " + pendingTxn.getSchemeCode()
                    );
                });

        log.info("[4.2] Scheme fetched successfully");
        log.info("    Scheme HashCode : {}", scheme.getSchemeCombinationHashCode());

        // ===== Parse Hash Code =====
        log.info("[5] Parsing schemeCombinationHashCode");

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

        if (hashParts.length < 5) {

            log.error("[5.1] Invalid schemeCombinationHashCode format");
            log.error("    Actual Hash : {}", scheme.getSchemeCombinationHashCode());

            throw new RuntimeException(
                    "Invalid schemeCombinationHashCode format: "
                            + scheme.getSchemeCombinationHashCode()
            );
        }

        String vendorId = hashParts[2];

        log.info("[5.2] Hash parsed successfully");
        log.info("    Vendor Id                 : {}", vendorId);

        // ===== Fetch Vendor =====
        log.info("[6] Fetching vendor using vendorId={}", vendorId);

        Vendors vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> {

                    log.error("[6.1] Vendor NOT FOUND for vendorId={}", vendorId);

                    return new RuntimeException(
                            "Vendor not found for ID: " + vendorId
                    );
                });

        String vendorName = vendor.getVendorName();

        log.info("[6.2] Vendor fetched successfully");
        log.info("    Vendor Name : {}", vendorName);
        log.info("    Vendor Code : {}", vendor.getVendorCode());

        // ===== Request Metadata =====
        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();

        try {

            // ===== Resolve Vendor Service =====
            log.info("[8] Resolving vendor implementation for vendorName={}",
                    vendorName);

            Object vendorService = vendorFactory.getVendor(vendorName);

            if (!(vendorService instanceof PayoutCallbackHandler handler)) {

                log.error("[8.1] Vendor callback handler NOT implemented");
                log.error("Vendor Name : {}", vendorName);

                return "Vendor callback handler not found";
            }

            log.info("[8.2] Vendor callback handler resolved successfully");

            // ===== Execute Vendor Callback =====
            log.info("[9] Executing vendor callback handler");

            String vendorResponse = handler.handleCallback(payload, request);

            log.info("[9.1] Vendor callback executed successfully");
            log.info("    Vendor Response : {}", vendorResponse);

            // ===== Save Callback Logs =====
            log.info("[10] Saving callback audit logs");

            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Callback processed for vendor = " + vendorName,
                    LocalDateTime.now()
            );

            log.info("[10.1] Callback audit log saved successfully");

            log.info("============== NIXASOFT PAYOUT CALLBACK END SUCCESS ==============");

            return vendorResponse;

        } catch (Exception e) {

            log.error("[ERROR] Error while processing callback");
            log.error("    Vendor Name : {}", vendorName);
            log.error("    Error Msg   : {}", e.getMessage(), e);

            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Error: " + e.getMessage(),
                    LocalDateTime.now()
            );

            log.info("[ERROR] Callback error log saved");

            log.info("============== NIXASOFT PAYOUT CALLBACK END FAILED ==============");

            return "Callback processing failed: " + e.getMessage();
        }
    }

    @Transactional
    public void checkStatusForPendingTransactions() {

        List<PayoutPendingTransactions> pendingList = payoutPendingTransactionRepository.findByStatus("PENDING");
        log.info("[ORCH_SCHEDULER] Found {} pending transactions", pendingList.size());

        for (PayoutPendingTransactions txn : pendingList) {
            String orderId = txn.getTransactionId();
            log.info("[1] Checking status for pending ORDER_ID={}", orderId);

            try {
                // Fetch pending transaction
                PayoutPendingTransactions pendingTxn = getPendingTransaction(orderId);

                // Fetch scheme, product, sub-product, and rate
                SchemeRate schemeRate = getSchemeRate(pendingTxn);
                Scheme scheme = getSchemeEntity(schemeRate.getSchemeCode());
                String[] hashParts = validateAndSplitSchemeHash(scheme.getSchemeCombinationHashCode());
                short productId = Short.parseShort(hashParts[0]);
                String subProductIdStr = hashParts[1];

                Products product = getProduct(productId);
                SubProducts subProduct = getSubProduct(subProductIdStr);

                // Resolve vendor and credentials
                VendorCredentials credentials = getVendorCredentials(subProductIdStr);
                VendorStatusChecker statusChecker = getVendorStatusChecker(credentials);

                if (statusChecker == null) {
                    log.warn("[ORCH_SCHEDULER] Vendor does not support status check for ORDER_ID={}", orderId);
                    continue;
                }

                // Call vendor check-status API dynamically
                PayoutStatusResponse vendorResponse = statusChecker.checkPayoutStatus(orderId, credentials);
                log.info("[ORCH_SCHEDULER] Vendor={} Order={} Status={}",
                        credentials.getVendor().getVendorName(), orderId, vendorResponse.getStatus());

                // Only process if status finalized
                if (!"PENDING".equalsIgnoreCase(vendorResponse.getStatus())) {
                    txn.setStatus(vendorResponse.getStatus());
                    payoutPendingTransactionRepository.save(txn);

                    // Build transaction DTO
                    TransactionSchemeDetailsRequestDto dto = buildTransactionDTO(
                            txn, schemeRate, scheme, product, subProduct);

                    // Process via transaction engine
                    transactionEngineServiceImpl.processTransaction(dto, vendorResponse.getStatus());
                    log.info("[ORCH_SCHEDULER] Processed ORDER_ID={} with STATUS={}",
                            orderId, vendorResponse.getStatus());
                }

            } catch (Exception e) {
                log.error("[ORCH_SCHEDULER] Error while checking status for ORDER_ID={} | {}",
                        orderId, e.getMessage(), e);
            }
        }
    }


    private PayoutPendingTransactions getPendingTransaction(String orderId) {
        log.debug("Fetching pending transaction for order ID: {}", orderId);
        return payoutPendingTransactionRepository.findByTransactionId(orderId)
                .orElseThrow(() -> {
                    log.error("Pending transaction not found for order ID: {}", orderId);
                    return new RuntimeException("Pending transaction not found for order ID: " + orderId);
                });
    }

    private SchemeRate getSchemeRate(PayoutPendingTransactions txn) {
        log.debug("Fetching scheme rate for SchemeCode={}, Operator={}, Amount={}",
                txn.getSchemeCode(), txn.getTransactionType(), txn.getAmount());
        return schemeRateRepository
                .findSchemeRateByCodeAndOperatorAndAmount(
                        txn.getSchemeCode(), txn.getTransactionType(), txn.getAmount().doubleValue())
                .orElseThrow(() -> {
                    log.error("No matching scheme rate found for amount: {}", txn.getAmount());
                    return new RuntimeException("No matching scheme rate found for amount: " + txn.getAmount());
                });
    }

    private Scheme getSchemeEntity(String schemeCode) {
        log.debug("Fetching scheme entity for SchemeCode={}", schemeCode);
        return schemeRepository.findBySchemeCode(schemeCode)
                .orElseThrow(() -> {
                    log.error("Scheme not found with code: {}", schemeCode);
                    return new RuntimeException("Scheme not found with code: " + schemeCode);
                });
    }

    private String[] validateAndSplitSchemeHash(String hash) {
        log.debug("Validating and splitting scheme hash: {}", hash);
        String[] parts = hash.split("-");
        if (parts.length < 2) {
            log.error("Invalid schemeCombinationHashCode: {}", hash);
            throw new RuntimeException("Invalid schemeCombinationHashCode: " + hash);
        }
        return parts;
    }

    private Products getProduct(short productId) {
        log.debug("Fetching product for ProductID={}", productId);
        return productsRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productId);
                    return new RuntimeException("Product not found with ID: " + productId);
                });
    }

    private SubProducts getSubProduct(String subProductId) {
        log.debug("Fetching sub-product for SubProductID={}", subProductId);
        return subProductsRepository.findById(subProductId)
                .orElseThrow(() -> {
                    log.error("SubProduct not found with ID: {}", subProductId);
                    return new RuntimeException("SubProduct not found with ID: " + subProductId);
                });
    }

    private VendorCredentials getVendorCredentials(String subProductId) {
        log.debug("Fetching vendor credentials for SubProductID={}", subProductId);
        VendorRouting routing = vendorRoutingRepository.findBySubProductId(subProductId);
        Vendors vendor = CommonMethods.getVendors(routing);
        log.debug("Resolved VendorID={} for SubProductID={}", vendor.getId(), subProductId);

        return vendorCredentialsRepository
                .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                .orElseThrow(() -> {
                    log.error("No credentials found for vendor ID={}", vendor.getId());
                    return new RuntimeException("No credentials found for vendor ID=" + vendor.getId());
                });
    }

    private VendorStatusChecker getVendorStatusChecker(VendorCredentials credentials) {
        log.debug("Fetching vendor status checker for VendorName={}", credentials.getVendor().getVendorName());
        Object vendorService = vendorFactory.getVendor(credentials.getVendor().getVendorName());
        if (vendorService instanceof VendorStatusChecker checker) {
            log.debug("VendorStatusChecker instance found for VendorName={}", credentials.getVendor().getVendorName());
            return checker;
        }
        log.warn("VendorStatusChecker not available for VendorName={}", credentials.getVendor().getVendorName());
        return null;
    }

    private TransactionSchemeDetailsRequestDto buildTransactionDTO(
            PayoutPendingTransactions txn,
            SchemeRate schemeRate,
            Scheme scheme,
            Products product,
            SubProducts subProduct) {

        String orderId = txn.getTransactionId();
        String baseTxnId = orderId.startsWith("TXN") ? orderId.substring(3) : orderId;

        log.debug("Building TransactionSchemeDetailsRequestDto for ORDER_ID={}", orderId);

        TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txn.getAmount().doubleValue());
        dto.setConfirmAmount(txn.getAmount().doubleValue());
        dto.setOperatorName(txn.getTransactionType());
        dto.setOperatorCode(schemeRate.getOperatorCode());
        dto.setSchemeRate(schemeRate);
        dto.setTransactionType(product.getTransactionType());
        dto.setWalletType(subProduct.getWalletType());
        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));
        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(subProduct.getSubProductCode());
        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());
        dto.setHierarchyId(txn.getRetailerId());

        log.debug("[DTO BUILDER] Created DTO for ORDER_ID={} -> {}", orderId, dto);
        return dto;
    }

    @Transactional
    public CommonResponse<BeneficiaryVerifyResponseDto> beneficiaryVerify(
            BeneficiaryVerifyRequestDto req) {

        try {

//            SubProducts subProduct = subProductsRepository.findById(req.getSubProductId())
//                    .orElseThrow(() -> new RuntimeException("SubProduct not found"));

            VendorRouting routing =
                    vendorRoutingRepository.findBySubProductId(req.getSubProductId());

            if (routing == null) {
                return new CommonResponse<>(404, "Vendor routing not found", null);
            }

            Vendors vendor = CommonMethods.getVendors(routing);

            VendorCredentials credentials =
                    vendorCredentialsRepository
                            .findByVendorIdAndProductId(vendor.getId(), vendor.getProduct().getId())
                            .orElseThrow(() -> new RuntimeException("Vendor credentials not found"));

            PayoutVendorService vendorService =
                    vendorFactory.getVendor(vendor.getVendorName());

            if (vendorService == null) {
                return new CommonResponse<>(500, "Vendor implementation not found", null);
            }

            if (!(vendorService instanceof PayoutBeneficiaryVerifier verifier)) {
                return new CommonResponse<>(400, "Vendor does not support beneficiary verification", null);
            }

            BeneficiaryVerifyResponseDto resp =
                    verifier.verifyBeneficiary(req, credentials);

            return new CommonResponse<>(200, resp.getMessage(), resp);

        } catch (Exception e) {

            log.error("Beneficiary verification error", e);

            return new CommonResponse<>(500, "Internal Server Error", null);
        }
    }

    @Transactional
    public String slpePayoutCallback(Map<String, String> payload, HttpServletRequest request)
            throws JsonProcessingException {

        String orderId = payload.get("order_id");
        String slpeStatus = payload.get("status");   // paid / failed / pending
        String txnId = payload.get("txn_id");

        log.info("[1] Fetching pending transaction for ORDER_ID: {}", txnId);
        PayoutPendingTransactions pendingTxn = payoutPendingTransactionRepository.findByTransactionId(txnId)
                .orElseThrow(() -> new RuntimeException("Pending transaction not found for order ID: " + txnId));

        Optional<Scheme> scheme = schemeRepository.findBySchemeCode(pendingTxn.getSchemeCode());

        // Parse scheme combination hash code
        String[] hashParts = scheme.get().getSchemeCombinationHashCode().split("-");
        if (hashParts.length < 2) {
            throw new RuntimeException("Invalid schemeCombinationHashCode format: " + scheme.get().getSchemeCombinationHashCode());
        }

        String vendorId = hashParts[2];

        Optional<Vendors> vendor = vendorRepository.findById(vendorId);
        String vendorCode = vendor.get().getVendorCode();

        LocalDateTime receivedAt = LocalDateTime.now();
        String clientIp = request.getRemoteAddr();
        log.info("[ORCH_CALLBACK] Received callback from vendor={} IP={} Payload={}", vendorCode, clientIp, payload);

        try {
            Object vendorService = vendorFactory.getVendor(vendorCode);
            if (!(vendorService instanceof PayoutCallbackHandler handler)) {
                log.error("[ORCH_CALLBACK] No callback handler implemented for vendor={}", vendorCode);
                return "Vendor callback handler not found";
            }

            String vendorResponse = handler.handleCallback(payload, request);

            // Log callback event for auditing
            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Callback processed for vendor = " + vendorCode,
                    LocalDateTime.now()
            );

            log.info("[ORCH_CALLBACK] Successfully processed callback for vendor={}", vendorCode);
            return vendorResponse;

        } catch (Exception e) {
            log.error("[ORCH_CALLBACK] Error processing callback for vendor={} | Error={}", vendorCode, e.getMessage(), e);

            commonFunctions.saveCallbackEventLog(
                    payload.toString(),
                    receivedAt,
                    clientIp,
                    "Error: " + e.getMessage(),
                    LocalDateTime.now()
            );

            return "Callback processing failed: " + e.getMessage();
        }
    }

    @Async
    @Transactional
    public void handleEncryptedCallbackAsync(Map<String, Object> body) {
        try {
            handleEncryptedCallback(body);
        } catch (Exception ex) {
            log.error("Callback processing failed: {}", ex.getMessage(), ex);
            // optionally save internal log to DB
        }
    }

    @Transactional
    public void handleEncryptedCallback(Map<String, Object> encryptedBody) {

        log.debug("Encrypted callback keys: {}", encryptedBody.keySet());

        Map<String, String> payload;
        try {
            log.info("➡ Decrypting & parsing vendor callback");

            // This method already logs encrypted/plain payload internally
            payload  = doHandleEncryptedCallback(encryptedBody);

            log.info(" Vendor callback decrypted & parsed | callbackId={}", payload.get("merchantRefId") != null ? payload.get("merchantRefId") : null);

        } catch (Exception e) {

            // Critical: decryption/parsing failed
            log.error("Failed to parse/decrypt vendor callback ", e);

            log.warn("Raw encrypted callback persisted | vendorId={} | reason=CALLBACK_DECRYPT_ERROR", "VIMOPAY");

            return; // swallow exception → vendor still receives 200
        }

        try {
            log.info("➡ Processing parsed vendor callback | vendorId={} | callbackId={}",
                    "VIMOPAY",
                    payload.get("merchantRefId"));

            vimopayHandleCallback(payload);

            log.info(" Vendor callback processed successfully | vendorId={} | callbackId={}",
                    "VIMOPAY",
                    payload.get("merchantRefId"));

        } catch (Exception e) {
            // Optional: depends on your retry/alert strategy
            log.error(" Error while processing parsed callback | vendorId={} | callbackId={}",
                    "VIMOPAY",
                    payload.get("merchantRefId"),
                    e
            );
        }
    }

    public Map<String, String> doHandleEncryptedCallback(
            Map<String, Object> body
    ) {

        String rawJson = null;
        String encryptedBlock = null;
        String decryptedJson = null;

        LocalDateTime requestTime =
                LocalDateTime.now();

        try {

            /*
             * Encrypted callback
             */
            if (body.containsKey("data") && body.get("data") instanceof String) {

                rawJson = objectMapper.writeValueAsString(body);

                encryptedBlock = String.valueOf(body.get("data"));

                decryptedJson = vimoPayCryptoUtil.decryptFromVendor(null, encryptedBlock);

                /*
                 * Persist callback log
                 */
                commonFunctions.saveVendorLogs(
                        requestTime,
                        rawJson,
                        "VIMOPAY",
                        LocalDateTime.now(),
                        decryptedJson,
                        null
                );

                log.info("Encrypted callback logged successfully");

                return objectMapper.readValue(
                        decryptedJson,
                        new TypeReference<Map<String, String>>() {}
                );
            }

            /*
             * Plain callback
             */
            rawJson = objectMapper.writeValueAsString(body);

            commonFunctions.saveVendorLogs(
                    requestTime,
                    rawJson,
                    "VIMOPAY",
                    LocalDateTime.now(),
                    rawJson,
                    null
            );

            log.info("Plain callback logged successfully");

            return objectMapper.readValue(
                    rawJson,
                    new TypeReference<Map<String, String>>() {}
            );

        } catch (Exception ex) {

            try {

                commonFunctions.saveVendorLogs(
                        requestTime,
                        rawJson == null
                                ? objectMapper.writeValueAsString(body)
                                : rawJson,
                        "VIMOPAY",
                        LocalDateTime.now(),
                        ex.getMessage(),
                        null
                );

            } catch (Exception ignore) {
                log.error(
                        "Failed saving vendor callback error log",
                        ignore
                );
            }

            throw new RuntimeException("Invalid callback payload",ex
            );
        }
    }

    /**
     * Handle callback from vendor with transaction status update
     */
    @Transactional
    public void vimopayHandleCallback(Map<String, String> payload) {
        log.info(
                "Received payout callback: merchantRef={} status={} txnId={}",
                payload.get("merchantRefId"),
                payload.get("txnStatus"),
                payload.get("txnId")
        );

        String trnId =
                payload.get("merchantRefId");

        try {
            // ===== Fetch Pending Transaction =====
            log.info("[2] VIMOPAY Fetching pending transaction using transactionId={}", trnId);

            PayoutPendingTransactions pendingTxn =
                    payoutPendingTransactionRepository.findByTransactionId(trnId)
                            .orElseThrow(() -> {
                                log.error("[2.1] VIMOPAY Pending transaction NOT FOUND for transactionId={}", trnId);
                                return new RuntimeException(
                                        "Pending transaction not found for order ID: " + trnId
                                );
                            });

            log.info("[2.2] VIMOPAY Pending transaction found successfully");
            log.info("    Scheme Code : {}", pendingTxn.getSchemeCode());
            log.info("    Retailer Id : {}", pendingTxn.getRetailerId());
            log.info("    Amount      : {}", pendingTxn.getAmount());

            // ===== Already SUCCESS Check =====
            if ("SUCCESS".equalsIgnoreCase(pendingTxn.getStatus())) {

                log.warn("[3] VIMOPAY Transaction already processed successfully");
                log.warn("TransactionId : {}", trnId);

                return;
            }

            // ===== Fetch Scheme =====
            log.info("[4] VIMOPAY Fetching scheme using schemeCode={}", pendingTxn.getSchemeCode());

            Scheme scheme = schemeRepository.findBySchemeCode(pendingTxn.getSchemeCode())
                    .orElseThrow(() -> {
                        log.error("[4.1]VIMOPAY Scheme NOT FOUND for schemeCode={}",
                                pendingTxn.getSchemeCode());

                        return new RuntimeException(
                                "Scheme not found for code: " + pendingTxn.getSchemeCode()
                        );
                    });

            log.info("[4.2] VIMOPAY Scheme fetched successfully");
            log.info("Scheme HashCode : {}", scheme.getSchemeCombinationHashCode());

            // ===== Parse Hash Code =====
            log.info("[5]VIMOPAY Parsing schemeCombinationHashCode");

            String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

            if (hashParts.length < 5) {

                log.error("[5.1]VIMOPAY Invalid schemeCombinationHashCode format");
                log.error("Actual Hash : {}", scheme.getSchemeCombinationHashCode());

                throw new RuntimeException(
                        "Invalid schemeCombinationHashCode format: "
                                + scheme.getSchemeCombinationHashCode()
                );
            }

            String vendorId = hashParts[2];

            log.info("[5.2] VIMOPAY Hash parsed successfully");
            log.info("    Vendor Id   : {}", vendorId);

            // ===== Fetch Vendor =====
            log.info("[6] VIMOPAY Fetching vendor using vendorId={}", vendorId);

            Vendors vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> {

                        log.error("[6.1] VIMOPAY Vendor NOT FOUND for vendorId={}", vendorId);

                        return new RuntimeException(
                                "Vendor not found for ID: " + vendorId
                        );
                    });

            String vendorName = vendor.getVendorName();

            log.info("[6.2] VIMOPAY Vendor fetched successfully");
            log.info("Vendor Name : {}", vendorName);
            log.info("Vendor Code : {}", vendor.getVendorCode());

            // ===== Request Metadata =====
            LocalDateTime receivedAt = LocalDateTime.now();
            //String clientIp = request.getRemoteAddr();

            try {

                // ===== Resolve Vendor Service =====
                log.info("[8]VIMOPAY Resolving vendor implementation for vendorName={}",
                        vendorName);

                Object vendorService = vendorFactory.getVendor(vendorName);

                if (!(vendorService instanceof PayoutCallbackHandler handler)) {

                    log.error("[8.1]VIMOPAY Vendor callback handler NOT implemented");
                    log.error("Vendor Name:{}", vendorName);

                    return;
                }

                log.info("[8.2] VIMOPAY Vendor callback handler resolved successfully");

                // ===== Execute Vendor Callback =====
                log.info("[9] VIMOPAY Executing vendor callback handler");

                String vendorResponse = handler.handleCallback(payload, null);

                log.info("[9.1] VIMOPAY Vendor callback executed successfully");
                log.info("Vendor Response : {}", vendorResponse);

                // ===== Save Callback Logs =====
                log.info("[10] VIMOPAY Saving callback audit logs");

                commonFunctions.saveCallbackEventLog(
                        payload.toString(),
                        receivedAt,
                        null,
                        "Callback processed for vendor = " + vendorName,
                        LocalDateTime.now()
                );

                log.info("[10.1] VIMOPAY Callback audit log saved successfully");

                log.info("============== VIMOPAY PAYOUT CALLBACK END SUCCESS ==============");

                return;

            } catch (Exception e) {

                log.error("[ERROR] VIMOPAY Error while processing callback");
                log.error("VIMOPAY Vendor Name : {}", vendorName);
                log.error("Error Msg   : {}", e.getMessage(), e);

                commonFunctions.saveCallbackEventLog(
                        payload.toString(),
                        receivedAt,
                        null,
                        "Error: " + e.getMessage(),
                        LocalDateTime.now()
                );

                log.info("[ERROR] VIMOPAY Callback error log saved");

                log.info("============== VIMOPAY PAYOUT CALLBACK END FAILED ==============");
            }
        } catch (Exception e) {
            log.error("Failed to process callback for ref={}: {}",
                    trnId, e.getMessage(), e);
            throw new RuntimeException("Callback processing failed", e);
        }
    }

}
