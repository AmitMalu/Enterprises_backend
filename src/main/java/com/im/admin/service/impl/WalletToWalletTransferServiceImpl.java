package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.entities.Operators;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.SubProducts;
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
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    public void transferAmountWalletToWallet(WalletToWalletTransferDto request, Double latitude, Double longitude) throws JsonProcessingException {

        log.info("=== START Wallet-to-Wallet Transfer ===");
        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            // Serialize request
            requestJson = objectMapper.writeValueAsString(request);
            log.info("Initiating wallet-to-wallet transfer: {}", requestJson);

            // Log request parameters
            log.info("Transfer Details - HierarchyId: {}, FromWallet: {}, ToWallet: {}, Amount: {}, ConfirmAmount: {}, SubProductId: {}, SchemeCode: {}",
                    request.getHierarchyId(),
                    request.getWalletTypeFrom(),
                    request.getWalletTypeTo(),
                    request.getTransferAmount(),
                    request.getConfirmAmount(),
                    request.getSubProductId(),
                    request.getSchemeCode()
            );

            log.info("Location Details - Latitude: {}, Longitude: {}", latitude, longitude);

            String hierarchyId = request.getHierarchyId();
            WalletType fromWallet = request.getWalletTypeFrom();
            WalletType toWallet = request.getWalletTypeTo();
            BigDecimal amount = request.getTransferAmount();
            BigDecimal confirmAmount = request.getConfirmAmount();

            // Validate amount match
            log.debug("Validating amount and confirmation amount");
            if (!Objects.equals(amount, confirmAmount)) {
                log.warn("Amount mismatch: {} != {}", amount, confirmAmount);
                throw new IllegalArgumentException("Amount and confirmation amount do not match.");
            }
            log.debug("Amount validation passed");

            // Validate wallet types
            log.debug("Validating wallet types - From: {}, To: {}", fromWallet, toWallet);
            if (fromWallet == toWallet) {
                log.warn("Same wallet type transfer attempted: {}", fromWallet);
                throw new IllegalArgumentException("Cannot transfer between the same wallet types.");
            }
            log.debug("Wallet type validation passed");

            // Check available balance
            log.info("Fetching available balance for hierarchyId: {}, walletType: {}", hierarchyId, fromWallet);
            BigDecimal availableBalance = commonMethods.getCurrentBalance(hierarchyId, String.valueOf(fromWallet));
            log.info("Available balance in {} wallet: {}", fromWallet, availableBalance);

            // Fetch SubProduct
            log.info("Fetching SubProduct for subProductId: {}", request.getSubProductId());
            SubProducts subProduct =
                    subProductsRepository.findById(request.getSubProductId())
                            .orElseThrow(() -> {
                                log.error("---- [Callback] SubProduct NOT FOUND for subProductId: {}", request.getSubProductId());
                                return new RuntimeException("SubProduct not found for subProductId: " + request.getSubProductId());
                            });

            log.info("---- [Callback] SubProduct fetched successfully | subProductId={}, subProductName={}",
                    request.getSubProductId(), subProduct.getSubProductTitle());

            // Get Product from SubProduct
            Products product = subProduct.getProduct();
            log.info("---- [Callback] Product fetched successfully | productId={}, productCode={}, transactionType={}",
                    product.getId(), product.getProductCode(), product.getTransactionType());

            // Fetch Scheme
            log.info("Extracting hierarchy type for hierarchyId: {}", hierarchyId);
            String hierarchyType = walletTransactionService.extractHierarchyType(hierarchyId);
            log.info("Hierarchy Type: {}", hierarchyType);

            String schemeCode;

            if ("RU".equalsIgnoreCase(hierarchyType) || "RT".equalsIgnoreCase(hierarchyType)) {

                // Retailer → use request scheme
                log.debug("Retailer/RU hierarchy detected, using scheme from request");
                schemeCode = request.getSchemeCode();

                if (schemeCode == null || schemeCode.isBlank()) {
                    log.error("Scheme code is required for Retailer/RU but was null or blank");
                    throw new RuntimeException("Scheme code is required for Retailer/RU");
                }
                log.info("Scheme code from request: {}", schemeCode);

            } else {

                // Other hierarchy → use mapped scheme
                log.debug("Non-retailer hierarchy, resolving scheme from hierarchy");
                String schemeName = getSchemeNameByHierarchy(hierarchyId);
                log.info("Scheme name resolved from hierarchy: {}", schemeName);

                Optional<Scheme> scheme = schemeRepository.findBySchemeName(schemeName);

                if (scheme.isEmpty()) {
                    log.error("Scheme not found for name: {}", schemeName);
                    throw new RuntimeException("Scheme not found for name: " + schemeName);
                }

                schemeCode = scheme.get().getSchemeCode();
                log.info("Scheme resolved from hierarchy | type={} | schemeCode={}", hierarchyType, schemeCode);
            }

            // FINAL COMMON CALL - Get Scheme Rate
            log.info("Fetching exact scheme rate for schemeCode: {}, amount: {}", schemeCode, request.getConfirmAmount().doubleValue());
            SchemeRate schemeRate = getExactSchemeRate(
                    schemeCode,
                    request.getConfirmAmount().doubleValue()
            );
            log.info("Scheme Rate fetched - SchemeCode: {}, OperatorCode: {}, RateType: {}, Gross: {}, IM: {}",
                    schemeRate.getSchemeCode(),
                    schemeRate.getOperatorCode(),
                    schemeRate.getRateType(),
                    schemeRate.getGross(),
                    schemeRate.getIm()
            );

            // Fetch Operator
            log.info("Fetching Operator for operatorCode: {}", schemeRate.getOperatorCode());
            Operators operator = operatorsRepository
                    .findByOperatorCode(schemeRate.getOperatorCode())
                    .orElseThrow(() -> new RuntimeException(
                            "Operator not found for code: " + schemeRate.getOperatorCode()
                    ));

            log.info("Operator fetched successfully | operatorCode={}, operatorName={}",
                    operator.getOperatorCode(), operator.getOperatorName());

            // Calculate gross charge
            log.debug("Calculating gross charge");
            BigDecimal grossCharge;

            if (schemeRate.getRateType() == RateType.PERCENTAGE) {
                grossCharge = amount.multiply(BigDecimal.valueOf(schemeRate.getGross())).divide(BigDecimal.valueOf(100));
                log.debug("Gross charge calculated (Percentage): {}% of {} = {}", schemeRate.getGross(), amount, grossCharge);
            } else {
                grossCharge = BigDecimal.valueOf(schemeRate.getGross());
                log.debug("Gross charge calculated (Fixed): {}", grossCharge);
            }

            BigDecimal totalRequiredAmount = amount.add(grossCharge);
            log.info("Total required amount: {} (Amount: {} + Gross Charge: {})", totalRequiredAmount, amount, grossCharge);

            // Validate sufficient balance
            log.debug("Checking sufficient balance - Available: {}, Required: {}", availableBalance, totalRequiredAmount);
            if (availableBalance.compareTo(totalRequiredAmount) < 0) {
                log.warn("Insufficient balance. Required: {}, Available: {}",
                        totalRequiredAmount, availableBalance);

                throw new IllegalArgumentException(
                        "Insufficient balance in " + fromWallet +
                                " wallet. Required: " + totalRequiredAmount +
                                ", Available: " + availableBalance
                );
            }
            log.debug("Sufficient balance available");

            // Generate base transaction ID
            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated base transaction ID: {}", baseTransactionId);

            // ====== DEBIT FROM SOURCE WALLET ======
            log.info("Debiting from {} wallet for hierarchyId: {}", fromWallet, hierarchyId);
            AbstractWalletAmountTable fromWalletTable = commonMethods.getWalletByType(fromWallet.name(), hierarchyId);
            log.debug("Source wallet table fetched: {}", fromWalletTable.getClass().getSimpleName());
            updateWallet(fromWalletTable, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT", latitude, longitude);
            log.info("Debited {} from {} wallet. Transaction ID: {}", amount, fromWallet, baseTransactionId);

            // ====== HIERARCHY CHAIN PROCESSING ======
            log.info("Processing hierarchy chain starting from: {}", hierarchyId);
            String currentId = hierarchyId;
            int hierarchyLevel = 0;

            while (true) {
                hierarchyLevel++;
                log.debug("Hierarchy level {} - Processing: {}", hierarchyLevel, currentId);

                Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
                if (parentOpt.isEmpty()) {
                    log.error("No approver found for hierarchy: {}", hierarchyId);
                    throw new RuntimeException("No approver found for hierarchy: " + hierarchyId);
                }

                String parentId = parentOpt.get();
                String parentType = commonMethods.extractHierarchyType(parentId);
                log.info("Found parent hierarchy: {} with type {}", parentId, parentType);

                if ("WL".equals(parentType)) {
                    log.info("=== Processing WL level transfer ===");
                    whiteLabelRepository.findById(parentId).ifPresent(wl -> {
                        log.info("Processing WL level transfer for WL ID: {}", wl.getWhiteLabelId());

                        log.debug("Crediting {} wallet at WL level", fromWallet);
                        AbstractWalletAmountTable from = commonMethods.getWalletByType(fromWallet.name(), wl.getWhiteLabelId());
                        updateWallet(from, parentId, fromWallet, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT", latitude, longitude);
                        log.debug("Credited {} to {} wallet at WL level", amount, fromWallet);

                        log.debug("Debiting {} wallet at WL level", toWallet);
                        AbstractWalletAmountTable to = commonMethods.getWalletByType(toWallet.name(), wl.getWhiteLabelId());
                        updateWallet(to, parentId, toWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT", latitude, longitude);
                        log.debug("Debited {} from {} wallet at WL level", amount, toWallet);

                        log.info("WL level transfer completed for WL ID: {}", wl.getWhiteLabelId());
                    });
                    log.info("=== WL level processing complete ===");
                    break;
                }

                if ("IM".equals(parentType) || "AD".equals(parentType)) {
                    log.info("=== Processing {} level transfer ===", parentType);
                    adminRepository.findById(parentId).ifPresent(ad -> {
                        log.info("Processing {} level transfer for Admin ID: {}", parentType, ad.getId());

                        log.debug("Crediting {} wallet at {} level", fromWallet, parentType);
                        AbstractWalletAmountTable from = commonMethods.getWalletByType(fromWallet.name(), ad.getId());
                        updateWallet(from, parentId, fromWallet, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT", latitude, longitude);
                        log.debug("Credited {} to {} wallet at {} level", amount, fromWallet, parentType);

                        log.debug("Debiting {} wallet at {} level", toWallet, parentType);
                        AbstractWalletAmountTable to = commonMethods.getWalletByType(toWallet.name(), ad.getId());
                        updateWallet(to, parentId, toWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT", latitude, longitude);
                        log.debug("Debited {} from {} wallet at {} level", amount, toWallet, parentType);

                        log.info("{} level transfer completed for Admin ID: {}", parentType, ad.getId());
                    });
                    log.info("=== {} level processing complete ===", parentType);
                    break;
                }

                currentId = parentId;
                log.debug("Moving to parent: {}", currentId);
            }

            // ====== CREDIT TO DESTINATION WALLET ======
            log.info("Crediting to {} wallet for hierarchyId: {}", toWallet, hierarchyId);
            AbstractWalletAmountTable toWalletTable = commonMethods.getWalletByType(toWallet.name(), hierarchyId);
            log.debug("Destination wallet table fetched: {}", toWalletTable.getClass().getSimpleName());
            updateWallet(toWalletTable, hierarchyId, toWallet, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT", latitude, longitude);
            log.info("Credited {} to {} wallet. Transaction ID: {}", amount, toWallet, baseTransactionId);

            // ====== PROCESS CHARGES ======
            log.info("Preparing transaction details for processing charges");
            TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();

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
            dto.setHierarchyId(request.getHierarchyId());
            dto.setProductCode(product.getProductCode());
            dto.setSubProductCode(subProduct.getSubProductCode());
            dto.setIsCommission(product.getIsCommissions());
            dto.setHasCharge(product.getHasCharges());
            dto.setLatitude(latitude);
            dto.setLongitude(longitude);

            log.debug("---- [Callback] DTO prepared: {}", dto);
            log.info("Processing charges - IM Rate: {}, SGST: {}", schemeRate.getIm(), product.getSgst());

            transactionEngineServiceImpl.processCharges(dto, fromWalletTable, BigDecimal.valueOf(schemeRate.getIm()), product.getSgst().toString(), baseTransactionId, "SUCCESS");
            log.info("Charges processed successfully for transaction ID: {}", baseTransactionId);

            // ====== BUILD SUCCESS RESPONSE ======
            CommonResponse<String> response = new CommonResponse<>(200, "Transfer successful", baseTransactionId);
            responseJson = objectMapper.writeValueAsString(response);

            log.info("=== Transfer completed successfully ===");
            log.info("Transaction ID: {}, From: {} -> To: {}, Amount: {}",
                    baseTransactionId, fromWallet, toWallet, amount);
            log.info("Hierarchy: {}, Scheme: {}, Operator: {}",
                    hierarchyId, schemeCode, operator.getOperatorName());

        } catch (Exception ex) {
            log.error("=== Wallet-to-wallet transfer failed ===");
            log.error("Error: {}", ex.getMessage());
            log.error("Stack trace:", ex);
            responseJson = "{\"error\": \"" + ex.getMessage() + "\"}";
            throw ex;

        } finally {
            LocalDateTime responseDateTime = LocalDateTime.now();
            log.info("Saving IM logs - Request: {}, Response: {}",
                    requestJson != null ? "Present" : "Null",
                    responseJson != null ? "Present" : "Null");

            // Save logs whether success or failure
            commonFunctions.saveImLogs(
                    requestJson,
                    requestDateTime,
                    null,
                    responseJson,
                    responseDateTime
            );
            log.info("IM logs saved successfully");
            log.info("=== END Wallet-to-Wallet Transfer ===");
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
