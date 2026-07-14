package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.RateType;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.WalletToWalletTransferService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.WalletTransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Slf4j
public class WalletToWalletTransferServiceImpl implements WalletToWalletTransferService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private TransactionEngineServiceImpl transactionEngineServiceImpl;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private WalletTransactionService walletTransactionService;

    @Transactional
    @Override
    public void transferAmountWalletToWallet(
            WalletToWalletTransferDto request,
            Double latitude,
            Double longitude
    ) throws JsonProcessingException {

        log.info("=== START Wallet-to-Wallet Transfer ===");

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {

            // =========================================================
            // SERIALIZE AND LOG REQUEST
            // =========================================================
            requestJson = objectMapper.writeValueAsString(request);

            log.info("Initiating wallet-to-wallet transfer: {}", requestJson);

            log.info(
                    "Transfer Details - HierarchyId: {}, FromWallet: {}, ToWallet: {}, " +
                            "Amount: {}, ConfirmAmount: {}, SubProductId: {}, SchemeCode: {}",
                    request.getHierarchyId(),
                    request.getWalletTypeFrom(),
                    request.getWalletTypeTo(),
                    request.getTransferAmount(),
                    request.getConfirmAmount(),
                    request.getSubProductId(),
                    request.getSchemeCode()
            );

            log.info(
                    "Location Details - Latitude: {}, Longitude: {}",
                    latitude,
                    longitude
            );

            String hierarchyId = request.getHierarchyId();
            WalletType fromWallet = request.getWalletTypeFrom();
            WalletType toWallet = request.getWalletTypeTo();
            BigDecimal amount = request.getTransferAmount();
            BigDecimal confirmAmount = request.getConfirmAmount();

            // =========================================================
            // BASIC VALIDATIONS
            // =========================================================
            if (hierarchyId == null || hierarchyId.isBlank()) {
                log.warn("Hierarchy ID is null or blank");
                throw new IllegalArgumentException("Hierarchy ID is required.");
            }

            if (fromWallet == null) {
                log.warn("Source wallet type is null");
                throw new IllegalArgumentException("Source wallet type is required.");
            }

            if (toWallet == null) {
                log.warn("Destination wallet type is null");
                throw new IllegalArgumentException("Destination wallet type is required.");
            }

            if (amount == null) {
                log.warn("Transfer amount is null");
                throw new IllegalArgumentException("Transfer amount is required.");
            }

            if (confirmAmount == null) {
                log.warn("Confirmation amount is null");
                throw new IllegalArgumentException("Confirmation amount is required.");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("Invalid transfer amount: {}", amount);
                throw new IllegalArgumentException(
                        "Transfer amount must be greater than zero."
                );
            }

            // Using compareTo so 100 and 100.00 are treated as equal.
            log.debug("Validating amount and confirmation amount");

            if (amount.compareTo(confirmAmount) != 0) {

                log.warn(
                        "Amount mismatch. Amount: {}, Confirmation amount: {}",
                        amount,
                        confirmAmount
                );

                throw new IllegalArgumentException(
                        "Amount and confirmation amount do not match."
                );
            }

            log.debug("Amount validation passed");

            // Validate wallet types
            log.debug(
                    "Validating wallet types - From: {}, To: {}",
                    fromWallet,
                    toWallet
            );

            if (fromWallet == toWallet) {

                log.warn(
                        "Same wallet type transfer attempted: {}",
                        fromWallet
                );

                throw new IllegalArgumentException(
                        "Cannot transfer between the same wallet types."
                );
            }

            log.debug("Wallet type validation passed");

            // =========================================================
            // FETCH AVAILABLE BALANCE
            // =========================================================
            log.info(
                    "Fetching available balance for hierarchyId: {}, walletType: {}",
                    hierarchyId,
                    fromWallet
            );

            BigDecimal availableBalance =
                    commonMethods.getCurrentBalance(
                            hierarchyId,
                            fromWallet.name()
                    );

            if (availableBalance == null) {

                log.error(
                        "Available balance returned null for hierarchyId: {}, walletType: {}",
                        hierarchyId,
                        fromWallet
                );

                throw new IllegalStateException(
                        "Unable to fetch available balance for " +
                                fromWallet + " wallet."
                );
            }

            log.info(
                    "Available balance in {} wallet: {}",
                    fromWallet,
                    availableBalance
            );

            // =========================================================
            // NEW CHANGE 1:
            // EARLY BALANCE CHECK BEFORE FETCHING SCHEME/SLAB
            //
            // At this point charge is not known, so only the requested
            // transfer amount can be checked.
            // =========================================================
            log.debug(
                    "Performing early balance validation. Requested amount: {}, Available balance: {}",
                    amount,
                    availableBalance
            );

            if (availableBalance.compareTo(amount) < 0) {

                log.warn(
                        "Insufficient balance before slab lookup. " +
                                "HierarchyId: {}, WalletType: {}, " +
                                "RequestedAmount: {}, AvailableBalance: {}",
                        hierarchyId,
                        fromWallet,
                        amount,
                        availableBalance
                );

                throw new IllegalArgumentException(
                        "Insufficient balance in " + fromWallet +
                                " wallet. Requested amount: " + amount +
                                ", Available balance: " + availableBalance
                );
            }

            log.info(
                    "Early balance validation passed. Requested amount: {}, Available balance: {}",
                    amount,
                    availableBalance
            );

            // =========================================================
            // FETCH SUB-PRODUCT
            // =========================================================
//            log.info(
//                    "Fetching SubProduct for subProductId: {}",
//                    request.getSubProductId()
//            );
//
//            SubProducts subProduct =
//                    subProductsRepository
//                            .findById(request.getSubProductId())
//                            .orElseThrow(() -> {
//
//                                log.error(
//                                        "[WTW] SubProduct NOT FOUND for subProductId: {}",
//                                        request.getSubProductId()
//                                );
//
//                                return new RuntimeException(
//                                        "SubProduct not found for subProductId: " +
//                                                request.getSubProductId()
//                                );
//                            });
//
//            log.info(
//                    "[WTW] SubProduct fetched successfully | " +
//                            "subProductId={}, subProductName={}",
//                    request.getSubProductId(),
//                    subProduct.getSubProductTitle()
//            );
//
//            // =========================================================
//            // FETCH PRODUCT FROM SUB-PRODUCT
//            // =========================================================
//            Products product = subProduct.getProduct();
//
//            if (product == null) {
//
//                log.error(
//                        "[WTW] Product not mapped with subProductId: {}",
//                        request.getSubProductId()
//                );
//
//                throw new RuntimeException(
//                        "Product is not mapped with sub-product: " +
//                                request.getSubProductId()
//                );
//            }
//
//            log.info(
//                    "[WTW] Product fetched successfully | " +
//                            "productId={}, productCode={}, transactionType={}",
//                    product.getId(),
//                    product.getProductCode(),
//                    product.getTransactionType()
//            );

            // =========================================================
            // RESOLVE SCHEME CODE
            // =========================================================
//            log.info(
//                    "Extracting hierarchy type for hierarchyId: {}",
//                    hierarchyId
//            );
//
//            String hierarchyType =
//                    walletTransactionService.extractHierarchyType(
//                            hierarchyId
//                    );
//
//            log.info("Hierarchy Type: {}", hierarchyType);
//
//            String schemeCode;
//
//            if ("RU".equalsIgnoreCase(hierarchyType)
//                    || "RT".equalsIgnoreCase(hierarchyType)) {
//
//                log.debug(
//                        "Retailer/RU hierarchy detected, using scheme from request"
//                );
//
//                schemeCode = request.getSchemeCode();
//
//                if (schemeCode == null || schemeCode.isBlank()) {
//
//                    log.error(
//                            "Scheme code is required for Retailer/RU but was null or blank"
//                    );
//
//                    throw new RuntimeException(
//                            "Scheme code is required for Retailer/RU"
//                    );
//                }
//
//                schemeCode = schemeCode.trim();
//
//                log.info(
//                        "Scheme code received from request: {}",
//                        schemeCode
//                );
//
//            } else {
//
//                log.debug(
//                        "Non-retailer hierarchy detected, resolving scheme from hierarchy"
//                );
//
//                String schemeName =
//                        getSchemeNameByHierarchy(hierarchyId);
//
//                log.info(
//                        "Scheme name resolved from hierarchy: {}",
//                        schemeName
//                );
//
//                Optional<Scheme> schemeOptional =
//                        schemeRepository.findBySchemeName(schemeName);
//
//                if (schemeOptional.isEmpty()) {
//
//                    log.error(
//                            "Scheme not found for name: {}",
//                            schemeName
//                    );
//
//                    throw new RuntimeException(
//                            "Scheme not found for name: " + schemeName
//                    );
//                }
//
//                schemeCode =
//                        schemeOptional.get().getSchemeCode();
//
//                log.info(
//                        "Scheme resolved from hierarchy | " +
//                                "hierarchyType={}, schemeCode={}",
//                        hierarchyType,
//                        schemeCode
//                );
//            }

            // =========================================================
            // RESOLVE SCHEME CODE AND SUB-PRODUCT ID
            // =========================================================
            log.info(
                    "Extracting hierarchy type for hierarchyId: {}",
                    hierarchyId
            );

            String hierarchyType =
                    walletTransactionService.extractHierarchyType(
                            hierarchyId
                    );

            log.info("Hierarchy Type: {}", hierarchyType);

            String schemeCode;
            String resolvedSubProductId;

            if ("RU".equalsIgnoreCase(hierarchyType)
                    || "RT".equalsIgnoreCase(hierarchyType)) {

                // Existing production workflow remains unchanged.
                log.debug(
                        "Retailer/RU hierarchy detected, using scheme and sub-product from request"
                );

                schemeCode = request.getSchemeCode();
                resolvedSubProductId = request.getSubProductId();

                if (schemeCode == null || schemeCode.isBlank()) {

                    log.error(
                            "Scheme code is required for Retailer/RU but was null or blank"
                    );

                    throw new RuntimeException(
                            "Scheme code is required for Retailer/RU"
                    );
                }

                if (resolvedSubProductId == null) {

                    log.error(
                            "SubProduct ID is required for Retailer/RU but was null"
                    );

                    throw new RuntimeException(
                            "SubProduct ID is required for Retailer/RU"
                    );
                }

                schemeCode = schemeCode.trim();

                log.info(
                        "Retailer/RU values received from request | " +
                                "schemeCode={}, subProductId={}",
                        schemeCode,
                        resolvedSubProductId
                );

            } else {

                // Existing scheme-resolution workflow remains unchanged.
                log.debug(
                        "Non-retailer hierarchy detected, resolving scheme from hierarchy"
                );

                String schemeName =
                        getSchemeNameByHierarchy(hierarchyId);

                log.info(
                        "Scheme name resolved from hierarchy: {}",
                        schemeName
                );

                Optional<Scheme> schemeOptional =
                        schemeRepository.findBySchemeName(schemeName);

                if (schemeOptional.isEmpty()) {

                    log.error(
                            "Scheme not found for name: {}",
                            schemeName
                    );

                    throw new RuntimeException(
                            "Scheme not found for name: " + schemeName
                    );
                }

                Scheme scheme = schemeOptional.get();

                schemeCode = scheme.getSchemeCode();

                // Only new behavior:
                // extract SubProduct ID from existing scheme hash.
                resolvedSubProductId =
                        extractSubProductIdFromSchemeHash(
                                scheme.getSchemeCombinationHashCode()
                        );

                log.info(
                        "Scheme resolved from hierarchy | " +
                                "hierarchyType={}, schemeCode={}, subProductId={}",
                        hierarchyType,
                        schemeCode,
                        resolvedSubProductId
                );
            }

            // =========================================================
            // FETCH SUB-PRODUCT
            // =========================================================
            log.info(
                    "Fetching SubProduct for resolvedSubProductId: {}",
                    resolvedSubProductId
            );

            SubProducts subProduct =
                    subProductsRepository
                            .findById(resolvedSubProductId)
                            .orElseThrow(() -> {

                                log.error(
                                        "[WTW] SubProduct NOT FOUND for subProductId: {}",
                                        resolvedSubProductId
                                );

                                return new RuntimeException(
                                        "SubProduct not found for subProductId: " +
                                                resolvedSubProductId
                                );
                            });

            log.info(
                    "[WTW] SubProduct fetched successfully | " +
                            "subProductId={}, subProductName={}",
                    resolvedSubProductId,
                    subProduct.getSubProductTitle()
            );

            // =========================================================
            // FETCH PRODUCT FROM SUB-PRODUCT
            // =========================================================
            Products product = subProduct.getProduct();

            if (product == null) {

                log.error(
                        "[WTW] Product not mapped with subProductId: {}",
                        resolvedSubProductId
                );

                throw new RuntimeException(
                        "Product is not mapped with sub-product: " +
                                resolvedSubProductId
                );
            }

            log.info(
                    "[WTW] Product fetched successfully | " +
                            "productId={}, productCode={}, transactionType={}",
                    product.getId(),
                    product.getProductCode(),
                    product.getTransactionType()
            );

            // =========================================================
            // FETCH EXACT SCHEME SLAB/RATE
            // =========================================================
            log.info(
                    "Fetching exact scheme rate for schemeCode: {}, amount: {}",
                    schemeCode,
                    confirmAmount
            );

            SchemeRate schemeRate =
                    getExactSchemeRate(
                            schemeCode,
                            confirmAmount.doubleValue()
                    );

            if (schemeRate == null) {

                log.error(
                        "Scheme rate returned null for schemeCode: {}, amount: {}",
                        schemeCode,
                        confirmAmount
                );

                throw new RuntimeException(
                        "Scheme slab not found for scheme code: " +
                                schemeCode + " and amount: " + confirmAmount
                );
            }

            log.info(
                    "Scheme Rate fetched - SchemeCode: {}, OperatorCode: {}, " +
                            "RateType: {}, Gross: {}, IM: {}",
                    schemeRate.getSchemeCode(),
                    schemeRate.getOperatorCode(),
                    schemeRate.getRateType(),
                    schemeRate.getGross(),
                    schemeRate.getIm()
            );

            // =========================================================
            // FETCH OPERATOR
            // =========================================================
            log.info(
                    "Fetching Operator for operatorCode: {}",
                    schemeRate.getOperatorCode()
            );

            Operators operator =
                    operatorsRepository
                            .findByOperatorCode(
                                    schemeRate.getOperatorCode()
                            )
                            .orElseThrow(() -> {

                                log.error(
                                        "Operator not found for operatorCode: {}",
                                        schemeRate.getOperatorCode()
                                );

                                return new RuntimeException(
                                        "Operator not found for code: " +
                                                schemeRate.getOperatorCode()
                                );
                            });

            log.info(
                    "Operator fetched successfully | " +
                            "operatorCode={}, operatorName={}",
                    operator.getOperatorCode(),
                    operator.getOperatorName()
            );

            // =========================================================
            // CALCULATE GROSS CHARGE
            // =========================================================
            log.debug(
                    "Calculating gross charge. RateType: {}, GrossRate: {}, Amount: {}",
                    schemeRate.getRateType(),
                    schemeRate.getGross(),
                    amount
            );

            if (schemeRate.getRateType() == null) {

                log.error(
                        "Rate type is null for schemeCode: {}",
                        schemeCode
                );

                throw new RuntimeException(
                        "Rate type is not configured for scheme: " +
                                schemeCode
                );
            }

            if (schemeRate.getGross() == null) {

                log.error(
                        "Gross rate is null for schemeCode: {}",
                        schemeCode
                );

                throw new RuntimeException(
                        "Gross rate is not configured for scheme: " +
                                schemeCode
                );
            }

            BigDecimal grossRate =
                    BigDecimal.valueOf(schemeRate.getGross());

            BigDecimal grossCharge;

            if (schemeRate.getRateType() == RateType.PERCENTAGE) {

                grossCharge = amount
                        .multiply(grossRate)
                        .divide(
                                BigDecimal.valueOf(100),
                                2,
                                RoundingMode.HALF_UP
                        );

                log.debug(
                        "Percentage gross charge calculated. " +
                                "Rate: {}%, Amount: {}, GrossCharge: {}",
                        grossRate,
                        amount,
                        grossCharge
                );

            } else {

                grossCharge = grossRate.setScale(
                        2,
                        RoundingMode.HALF_UP
                );

                log.debug(
                        "Fixed gross charge calculated: {}",
                        grossCharge
                );
            }

            if (grossCharge.compareTo(BigDecimal.ZERO) < 0) {

                log.error(
                        "Calculated gross charge is negative. " +
                                "SchemeCode: {}, GrossCharge: {}",
                        schemeCode,
                        grossCharge
                );

                throw new IllegalStateException(
                        "Invalid negative charge configured for scheme: " +
                                schemeCode
                );
            }

            BigDecimal totalRequiredAmount =
                    amount.add(grossCharge);

            log.info(
                    "Total required amount calculated. " +
                            "TransferAmount: {}, GrossCharge: {}, TotalRequiredAmount: {}",
                    amount,
                    grossCharge,
                    totalRequiredAmount
            );

            // =========================================================
            // NEW CHANGE 2:
            // FINAL BALANCE CHECK AFTER SLAB AND CHARGE CALCULATION
            //
            // This check is mandatory because the final debit includes
            // the transfer amount plus the calculated charge.
            // =========================================================
            log.debug(
                    "Performing final balance validation. " +
                            "AvailableBalance: {}, TotalRequiredAmount: {}",
                    availableBalance,
                    totalRequiredAmount
            );

            if (availableBalance.compareTo(totalRequiredAmount) < 0) {

                log.warn(
                        "Insufficient balance after charge calculation. " +
                                "HierarchyId: {}, WalletType: {}, " +
                                "TransferAmount: {}, GrossCharge: {}, " +
                                "TotalRequiredAmount: {}, AvailableBalance: {}",
                        hierarchyId,
                        fromWallet,
                        amount,
                        grossCharge,
                        totalRequiredAmount,
                        availableBalance
                );

                throw new IllegalArgumentException(
                        "Insufficient balance in " + fromWallet +
                                " wallet. Transfer amount: " + amount +
                                ", Charge: " + grossCharge +
                                ", Total required: " + totalRequiredAmount +
                                ", Available balance: " + availableBalance
                );
            }

            log.info(
                    "Final balance validation passed. " +
                            "TotalRequiredAmount: {}, AvailableBalance: {}",
                    totalRequiredAmount,
                    availableBalance
            );

            // =========================================================
            // GENERATE BASE TRANSACTION ID
            // =========================================================
            String baseTransactionId =
                    commonMethods.generateBaseTransactionId();

            log.info(
                    "Generated base transaction ID: {}",
                    baseTransactionId
            );

            // =========================================================
            // DEBIT TRANSFER AMOUNT FROM SOURCE WALLET
            //
            // Only the transfer amount is debited here.
            // Charges must be debited separately by processCharges().
            // =========================================================
            log.info(
                    "Debiting transfer amount from {} wallet for hierarchyId: {}",
                    fromWallet,
                    hierarchyId
            );

            AbstractWalletAmountTable fromWalletTable =
                    commonMethods.getWalletByType(
                            fromWallet.name(),
                            hierarchyId
                    );

            if (fromWalletTable == null) {

                log.error(
                        "Source wallet not found. HierarchyId: {}, WalletType: {}",
                        hierarchyId,
                        fromWallet
                );

                throw new RuntimeException(
                        "Source wallet not found for hierarchyId: " +
                                hierarchyId
                );
            }

            log.debug(
                    "Source wallet table fetched: {}",
                    fromWalletTable.getClass().getSimpleName()
            );

            updateWallet(
                    fromWalletTable,
                    hierarchyId,
                    fromWallet,
                    amount,
                    "Debit",
                    baseTransactionId,
                    "SUCCESS",
                    "DEBIT",
                    latitude,
                    longitude
            );

            log.info(
                    "Debited transfer amount {} from {} wallet. Transaction ID: {}",
                    amount,
                    fromWallet,
                    baseTransactionId
            );

            // =========================================================
            // HIERARCHY CHAIN PROCESSING
            // =========================================================
            log.info(
                    "Processing hierarchy chain starting from: {}",
                    hierarchyId
            );

            String currentId = hierarchyId;
            int hierarchyLevel = 0;

            while (true) {

                hierarchyLevel++;

                log.debug(
                        "Hierarchy level {} - Processing hierarchyId: {}",
                        hierarchyLevel,
                        currentId
                );

                Optional<String> parentOpt =
                        commonMethods.findParentHierarchy(currentId);

                if (parentOpt.isEmpty()) {

                    log.error(
                            "No approver found for hierarchyId: {}",
                            hierarchyId
                    );

                    throw new RuntimeException(
                            "No approver found for hierarchy: " +
                                    hierarchyId
                    );
                }

                String parentId = parentOpt.get();

                String parentType =
                        commonMethods.extractHierarchyType(parentId);

                log.info(
                        "Found parent hierarchy: {} with type: {}",
                        parentId,
                        parentType
                );

                if ("WL".equalsIgnoreCase(parentType)) {

                    log.info("=== Processing WL level transfer ===");

                    WhiteLabels whiteLabel =
                            whiteLabelRepository
                                    .findById(parentId)
                                    .orElseThrow(() -> {

                                        log.error(
                                                "WhiteLabel not found for parentId: {}",
                                                parentId
                                        );

                                        return new RuntimeException(
                                                "WhiteLabel not found for hierarchyId: " +
                                                        parentId
                                        );
                                    });

                    log.info(
                            "Processing WL level transfer for WL ID: {}",
                            whiteLabel.getWhiteLabelId()
                    );

                    AbstractWalletAmountTable wlFromWallet =
                            commonMethods.getWalletByType(
                                    fromWallet.name(),
                                    whiteLabel.getWhiteLabelId()
                            );

                    if (wlFromWallet == null) {

                        throw new RuntimeException(
                                fromWallet +
                                        " wallet not found for WhiteLabel: " +
                                        whiteLabel.getWhiteLabelId()
                        );
                    }

                    updateWallet(
                            wlFromWallet,
                            parentId,
                            fromWallet,
                            amount,
                            "Credit",
                            baseTransactionId,
                            "SUCCESS",
                            "CREDIT",
                            latitude,
                            longitude
                    );

                    log.debug(
                            "Credited {} to {} wallet at WL level",
                            amount,
                            fromWallet
                    );

                    AbstractWalletAmountTable wlToWallet =
                            commonMethods.getWalletByType(
                                    toWallet.name(),
                                    whiteLabel.getWhiteLabelId()
                            );

                    if (wlToWallet == null) {

                        throw new RuntimeException(
                                toWallet +
                                        " wallet not found for WhiteLabel: " +
                                        whiteLabel.getWhiteLabelId()
                        );
                    }

                    updateWallet(
                            wlToWallet,
                            parentId,
                            toWallet,
                            amount,
                            "Debit",
                            baseTransactionId,
                            "SUCCESS",
                            "DEBIT",
                            latitude,
                            longitude
                    );

                    log.debug(
                            "Debited {} from {} wallet at WL level",
                            amount,
                            toWallet
                    );

                    log.info(
                            "WL level transfer completed for WL ID: {}",
                            whiteLabel.getWhiteLabelId()
                    );

                    log.info("=== WL level processing complete ===");

                    break;
                }

                if ("IM".equalsIgnoreCase(parentType)
                        || "AD".equalsIgnoreCase(parentType)) {

                    log.info(
                            "=== Processing {} level transfer ===",
                            parentType
                    );

                    Admin admin =
                            adminRepository
                                    .findById(parentId)
                                    .orElseThrow(() -> {

                                        log.error(
                                                "Admin not found for parentId: {}",
                                                parentId
                                        );

                                        return new RuntimeException(
                                                "Admin not found for hierarchyId: " +
                                                        parentId
                                        );
                                    });

                    log.info(
                            "Processing {} level transfer for Admin ID: {}",
                            parentType,
                            admin.getId()
                    );

                    AbstractWalletAmountTable adminFromWallet =
                            commonMethods.getWalletByType(
                                    fromWallet.name(),
                                    admin.getId()
                            );

                    if (adminFromWallet == null) {

                        throw new RuntimeException(
                                fromWallet +
                                        " wallet not found for Admin: " +
                                        admin.getId()
                        );
                    }

                    updateWallet(
                            adminFromWallet,
                            parentId,
                            fromWallet,
                            amount,
                            "Credit",
                            baseTransactionId,
                            "SUCCESS",
                            "CREDIT",
                            latitude,
                            longitude
                    );

                    log.debug(
                            "Credited {} to {} wallet at {} level",
                            amount,
                            fromWallet,
                            parentType
                    );

                    AbstractWalletAmountTable adminToWallet =
                            commonMethods.getWalletByType(
                                    toWallet.name(),
                                    admin.getId()
                            );

                    if (adminToWallet == null) {

                        throw new RuntimeException(
                                toWallet +
                                        " wallet not found for Admin: " +
                                        admin.getId()
                        );
                    }

                    updateWallet(
                            adminToWallet,
                            parentId,
                            toWallet,
                            amount,
                            "Debit",
                            baseTransactionId,
                            "SUCCESS",
                            "DEBIT",
                            latitude,
                            longitude
                    );

                    log.debug(
                            "Debited {} from {} wallet at {} level",
                            amount,
                            toWallet,
                            parentType
                    );

                    log.info(
                            "{} level transfer completed for Admin ID: {}",
                            parentType,
                            admin.getId()
                    );

                    log.info(
                            "=== {} level processing complete ===",
                            parentType
                    );

                    break;
                }

                currentId = parentId;

                log.debug(
                        "Moving to next parent hierarchy: {}",
                        currentId
                );
            }

            // =========================================================
            // CREDIT TRANSFER AMOUNT TO DESTINATION WALLET
            // =========================================================
            log.info(
                    "Crediting transfer amount to {} wallet for hierarchyId: {}",
                    toWallet,
                    hierarchyId
            );

            AbstractWalletAmountTable toWalletTable =
                    commonMethods.getWalletByType(
                            toWallet.name(),
                            hierarchyId
                    );

            if (toWalletTable == null) {

                log.error(
                        "Destination wallet not found. HierarchyId: {}, WalletType: {}",
                        hierarchyId,
                        toWallet
                );

                throw new RuntimeException(
                        "Destination wallet not found for hierarchyId: " +
                                hierarchyId
                );
            }

            log.debug(
                    "Destination wallet table fetched: {}",
                    toWalletTable.getClass().getSimpleName()
            );

            updateWallet(
                    toWalletTable,
                    hierarchyId,
                    toWallet,
                    amount,
                    "Credit",
                    baseTransactionId,
                    "SUCCESS",
                    "CREDIT",
                    latitude,
                    longitude
            );

            log.info(
                    "Credited {} to {} wallet. Transaction ID: {}",
                    amount,
                    toWallet,
                    baseTransactionId
            );

            // =========================================================
            // PREPARE AND PROCESS CHARGES
            // =========================================================
            log.info(
                    "Preparing transaction details for processing charges"
            );

            TransactionSchemeDetailsRequestDto dto =
                    new TransactionSchemeDetailsRequestDto();

            dto.setBaseTranxId(baseTransactionId);
            dto.setAmount(confirmAmount.doubleValue());
            dto.setConfirmAmount(confirmAmount.doubleValue());
            dto.setOperatorName(schemeRate.getOperatorName());
            dto.setOperatorCode(schemeRate.getSchemeCode());
            dto.setSchemeRate(schemeRate);
            dto.setTransactionType(product.getTransactionType());
            dto.setWalletType(subProduct.getWalletType());
            dto.setGstValue(String.valueOf(product.getSgst()));
            dto.setTdsRate(String.valueOf(product.getTds()));
            dto.setHierarchyId(hierarchyId);
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setLatitude(latitude);
            dto.setLongitude(longitude);

            log.debug(
                    "[WTW] Transaction charge DTO prepared: {}",
                    dto
            );

            log.info(
                    "Processing charges - IM Rate: {}, SGST: {}",
                    schemeRate.getIm(),
                    product.getSgst()
            );

            transactionEngineServiceImpl.processChargesAndCommission(
                    dto,
                    fromWalletTable,
                    BigDecimal.valueOf(schemeRate.getIm()),
                    product.getSgst().toString(),
                    baseTransactionId,
                    "SUCCESS"
            );

            log.info(
                    "Charges processed successfully for transaction ID: {}",
                    baseTransactionId
            );

            // =========================================================
            // BUILD SUCCESS RESPONSE LOG
            // =========================================================
            CommonResponse<String> response =
                    new CommonResponse<>(
                            200,
                            "Transfer successful",
                            baseTransactionId
                    );

            responseJson =
                    objectMapper.writeValueAsString(response);

            log.info("=== Transfer completed successfully ===");

            log.info(
                    "Transaction ID: {}, From: {} -> To: {}, " +
                            "TransferAmount: {}, GrossCharge: {}, TotalRequiredAmount: {}",
                    baseTransactionId,
                    fromWallet,
                    toWallet,
                    amount,
                    grossCharge,
                    totalRequiredAmount
            );

            log.info(
                    "Hierarchy: {}, Scheme: {}, Operator: {}",
                    hierarchyId,
                    schemeCode,
                    operator.getOperatorName()
            );

        } catch (Exception ex) {

            log.error("=== Wallet-to-wallet transfer failed ===");

            log.error(
                    "Wallet-to-wallet transfer error: {}",
                    ex.getMessage(),
                    ex
            );

            try {

                Map<String, Object> errorResponse = new HashMap<>();

                errorResponse.put("status", 500);
                errorResponse.put("message", ex.getMessage());

                responseJson =
                        objectMapper.writeValueAsString(errorResponse);

            } catch (Exception serializationException) {

                log.error(
                        "Failed to serialize wallet transfer error response",
                        serializationException
                );

                responseJson =
                        "{\"status\":500,\"message\":\"Wallet transfer failed\"}";
            }

            throw ex;

        } finally {

            LocalDateTime responseDateTime =
                    LocalDateTime.now();

            log.info(
                    "Saving IM logs - Request: {}, Response: {}",
                    requestJson != null ? "Present" : "Null",
                    responseJson != null ? "Present" : "Null"
            );

            try {

                commonFunctions.saveImLogs(
                        requestJson,
                        requestDateTime,
                        null,
                        responseJson,
                        responseDateTime
                );

                log.info("IM logs saved successfully");

            } catch (Exception logException) {

                // Do not overwrite the original transaction exception
                // if only the audit-log save operation fails.
                log.error(
                        "Failed to save IM logs for wallet-to-wallet transfer",
                        logException
                );
            }

            log.info("=== END Wallet-to-Wallet Transfer ===");
        }
    }

    private String extractSubProductIdFromSchemeHash(
            String schemeCombinationHashCode
    ) {

        if (schemeCombinationHashCode == null
                || schemeCombinationHashCode.isBlank()) {

            throw new RuntimeException(
                    "Scheme combination hash code is missing."
            );
        }

        String[] parts =
                schemeCombinationHashCode.trim().split("-");

        /*
         * Example:
         * 10-01-02-001-00001
         *
         * index 0 = productId
         * index 1 = subProductId
         */
        if (parts.length < 2
                || parts[1] == null
                || parts[1].isBlank()) {

            throw new RuntimeException(
                    "Invalid scheme combination hash code: " +
                            schemeCombinationHashCode
            );
        }

        try {

            String subProductId = parts[1].trim();

            log.info(
                    "[WTW] SubProduct ID extracted from scheme hash | " +
                            "schemeHash={}, subProductId={}",
                    schemeCombinationHashCode,
                    subProductId
            );

            return subProductId;

        } catch (NumberFormatException ex) {

            throw new RuntimeException(
                    "Invalid SubProduct ID in scheme combination hash: " +
                            schemeCombinationHashCode,
                    ex
            );
        }
    }


    private <T extends AbstractWalletAmountTable> void updateWallet(T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service, Double latitude, Double longitude) {

        log.debug("Updating wallet: {} | Type: {} | Amount: {} | TranxType: {}", hierarchyId, walletType, amount, tranxType);

        String actionOnBalance;
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            actionOnBalance = "Add Funds - " + walletType.name();
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
            actionOnBalance = "Deduct Funds - " + walletType.name();
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        log.debug("Wallet updated. Before: {} | After: {}", balanceBefore, wallet.getAvailableBalance());

        insertTransaction(walletType.name(), hierarchyId, tranStatus, amount, balanceBefore,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service, latitude, longitude);
    }


    private void insertTransaction(String walletType, String hierarchyId, String tranStatus, BigDecimal amount,
                                   BigDecimal beforeBalance, BigDecimal afterBalance, String actionOnBalanceAmount,
                                   String tranxId, String service, Double latitude, Double longitude) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String transactionTableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        log.info("Inserting into {} | Type: {} | Amount: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTableName, service, amount, hierarchyId, beforeBalance, afterBalance);

        String sql = "INSERT INTO " + transactionTableName +
                " (" + columnName + ", service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, latitude, longitude) " +
                "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, :latitude, :longitude)";

        try {
            entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("dateOfTransaction", new Date())
                    .setParameter("timeOfTransaction", new Time(System.currentTimeMillis()))
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("latitude", latitude)
                    .setParameter("longitude", longitude)
                    .executeUpdate();

            log.info("Transaction inserted successfully | TranxID={}", tranxId);

        } catch (PersistenceException ex) {

            // Handle duplicate safely
            if (ex.getMessage() != null &&
                    (ex.getMessage().contains("constraint") || ex.getMessage().contains("Duplicate"))) {

                log.warn("Duplicate transaction skipped | TranxID={}", tranxId);
                return; // skip insert, continue flow
            }

            log.error("Failed to insert transaction | TranxID={}", tranxId, ex);
            throw ex; // real error → propagate
        }
    }

    public SchemeRate getExactSchemeRate(String schemeCode, Double txnAmount) {

        log.info("Getting exact scheme rate for schemeCode: {}, txnAmount: {}", schemeCode, txnAmount);

        List<SchemeRate> schemeRates = schemeRateRepository.findBySchemeCode(schemeCode);
        log.debug("Found {} scheme rate records for schemeCode: {}", schemeRates.size(), schemeCode);

        if (schemeRates.isEmpty()) {
            log.error("No scheme found for code: {}", schemeCode);
            throw new RuntimeException("Scheme not found for code: " + schemeCode);
        }

        log.debug("Processing scheme rates for schemeCode: {}", schemeCode);

        for (SchemeRate rate : schemeRates) {
            log.debug("Checking rate - SlabType: {}, SlabStart: {}, SlabEnd: {}, Amount: {}",
                    rate.getSlabType(), rate.getSlabStart(), rate.getSlabEnd(), txnAmount);

            // Handle VARIABLE slab - check amount range
            if (rate.getSlabType() == SlabType.VARIABLE) {
                log.debug("Processing VARIABLE slab - Checking if {} is between {} and {}",
                        txnAmount, rate.getSlabStart(), rate.getSlabEnd());

                if (txnAmount >= rate.getSlabStart() && txnAmount <= rate.getSlabEnd()) {
                    log.info("Found matching VARIABLE slab for amount: {} - SlabStart: {}, SlabEnd: {}",
                            txnAmount, rate.getSlabStart(), rate.getSlabEnd());
                    return rate;
                } else {
                    log.debug("Amount {} does NOT fall within VARIABLE slab range: {} - {}",
                            txnAmount, rate.getSlabStart(), rate.getSlabEnd());
                }
            }

            // Handle FIXED slab - applies to all amounts
            if (rate.getSlabType() == SlabType.FIXED) {
                log.debug("Processing FIXED slab - Checking if {} is between {} and {}",
                        txnAmount, rate.getSlabStart(), rate.getSlabEnd());

                if (txnAmount >= rate.getSlabStart() && txnAmount <= rate.getSlabEnd()) {
                    log.info("Found matching FIXED slab for amount: {} - SlabStart: {}, SlabEnd: {}",
                            txnAmount, rate.getSlabStart(), rate.getSlabEnd());
                    return rate;
                } else {
                    log.debug("Amount {} does NOT fall within FIXED slab range: {} - {}",
                            txnAmount, rate.getSlabStart(), rate.getSlabEnd());
                }
            }
        }

        log.error("No matching slab found for schemeCode: {}, amount: {}. Total records checked: {}",
                schemeCode, txnAmount, schemeRates.size());
        throw new RuntimeException("No slab found for amount: " + txnAmount);
    }

    public String getSchemeNameByHierarchy(String hierarchyId) {

        String type = walletTransactionService.extractHierarchyType(hierarchyId);

        return switch (type.toUpperCase()) {
            case "SD" -> "wallet_to_wallet_SD_Scheme";
            case "PT" -> "wallet_to_wallet_PT_Scheme";
            case "DT" -> "wallet_to_wallet_DT_Scheme";
            default -> throw new RuntimeException("No scheme defined for hierarchy: " + type);
        };
    }
}
