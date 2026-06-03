package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.exception.ResourceNotFoundException;
import com.im.admin.repositories.*;
import com.im.admin.response.BulkTransactionStatusResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TransactionEngineService;
import com.im.admin.util.AbstractRetailerTransactionDetails;
import com.im.admin.util.TransactionHelperService;
import jakarta.persistence.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


@Service
@Slf4j
public class TransactionEngineServiceImpl implements TransactionEngineService {

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private DynamicWalletRepository dynamicWalletRepository;

    @Autowired
    private DynamicTransactionDetailsRepository transactionDetailsRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository whiteLabelPrepaidAmountRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository whiteLabelPostpaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionHelperService transactionHelperService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

    @Autowired
    private PgTransactionMappingRepository pgTransactionMappingRepository;

    @Autowired
    private PayoutPendingTransactionRepository payoutPendingTransactionRepository;

    @Autowired
    private BBPSPendingTransactionsRepository bbpsPendingTransactionsRepository;


    @Override
    public CommonResponse<CardBinDetailsResponseDto> getDetailsByCardBINNumber(CardTypeRequestDto cardTypeRequestDto) {
        log.info("Fetching details for BIN: {}", cardTypeRequestDto.getCardBinNo());

        return cardTypeMasterRepository.findByCardBinNo(cardTypeRequestDto.getCardBinNo())
                .map(cardType -> {
                    log.info("Card Type found - Type: {}, Name: {}", cardType.getCardType(), cardType.getIssuerName());
                    CardBinDetailsResponseDto dto = new CardBinDetailsResponseDto(cardType.getCardType(), cardType.getIssuerName());
                    return CommonResponse.success(dto, "Card BIN details found");
                })
                .orElseGet(() -> {
                    log.warn("No card type found for BIN No: {}", cardTypeRequestDto.getCardBinNo());
                    return CommonResponse.failure("No card details found for BIN No: " + cardTypeRequestDto.getCardBinNo());
                });
    }

    @Override
    public TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request) {
        log.info("Fetching scheme details for hierarchy ID: {}, Operator: {}, Code: {}",
                request.getHierarchyId(), request.getOperatorName(), request.getOperatorCode());

        validateAmount(request);

        List<SchemeAllocation> allocations = schemeAllocationRepository.findByHierarchyId(request.getHierarchyId());
        String schemeCode = validateAndGetSchemeCode(request, allocations);

        Optional<Products> productOpt = productsRepository.findByProductCode(request.getProductCode());
        if (productOpt.isEmpty()) {
            throw new EntityNotFoundException("Product not found with code: " + request.getProductCode());
        }

        Products product = productOpt.get();
        Double gstValue = product.getSgst();
        Double tdsValue = product.getTds();
        Boolean hasCharges = product.getHasCharges();
        Boolean isCommission = product.getIsCommissions();
        String tranxType = product.getTransactionType();

        String walletType = getWalletType(request);
        SchemeRate matchedSchemeRate = findMatchingSchemeRate(request);

        return new TransactionSchemeDetailsResponseDto(matchedSchemeRate, walletType, gstValue.toString(), tdsValue.toString(), hasCharges, isCommission, tranxType);
    }

    @Override
    @Transactional
    public String processTransaction(TransactionSchemeDetailsRequestDto dto, String vendorCallbackStatus) {
        String baseTransactionId = dto.getBaseTranxId();
        String retailerId = dto.getHierarchyId();
        String walletType = dto.getWalletType().toUpperCase();
        String tranxType = dto.getTransactionType().toLowerCase();
        BigDecimal tranxAmount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(dto.getSchemeRate().getGross());
        String taxValue = dto.getGstValue();

        log.info("[TXN_PROCESS] Start | BaseTxnID={} | RetailerID={} | Type={} | Wallet={} | Amount={} | Gross={} | Tax={} | VendorStatus={}",
                baseTransactionId, retailerId, tranxType, walletType, tranxAmount, grossAmount, taxValue, vendorCallbackStatus);

        AbstractWalletAmountTable wallet = getWalletByType(walletType, retailerId);
        log.info("[WALLET] Initial Balance ({}): {}", walletType, wallet.getAvailableBalance());

        Boolean isSettlement = validateAndFetchSettlement(dto.getSubProductCode());
        log.info("[TXN_PROCESS] Settlement Flag: {}", isSettlement);

        if (isCredit(tranxType)) {
            return handleCreditTransaction(dto, wallet, vendorCallbackStatus, tranxAmount, grossAmount, taxValue, baseTransactionId, isSettlement);
        } else if (isDebit(tranxType)) {
            return handleDebitTransaction(dto, wallet, vendorCallbackStatus, tranxAmount, grossAmount, taxValue, baseTransactionId, isSettlement);
        }

        throw new IllegalArgumentException("Unsupported transaction type: " + tranxType);
    }

    private String handleCreditTransaction(TransactionSchemeDetailsRequestDto dto,
                                           AbstractWalletAmountTable wallet,
                                           String vendorStatus,
                                           BigDecimal tranxAmount,
                                           BigDecimal grossAmount,
                                           String taxValue,
                                           String baseTransactionId,
                                           boolean isSettlement) {

        log.info("[TXN_CREDIT] Processing CREDIT flow...");

        switch (vendorStatus.toUpperCase()) {
            case "PENDING":
                insertPendingTransaction(wallet, tranxAmount, baseTransactionId, dto, "Vendor Pending - Credit", "CREDIT");
                return "Transaction pending - no wallet update";

            case "FAILURE":
            case "FAILED":
                insertFailedTransaction(wallet, tranxAmount, baseTransactionId, dto, "Vendor Failure - No Credit", "CREDIT");
                return "Vendor transaction failed";

            default:
                if (isSettlement) {
                    insertPendingTransaction(wallet, tranxAmount, baseTransactionId, dto, "Settlement Pending - Credit", "CREDIT");
                    return "Transaction marked as pending for settlement";
                }
                // On success
                updateWallet(dto, wallet, tranxAmount, "credit", baseTransactionId, vendorStatus, "CREDIT");
                processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, vendorStatus);
                break;
        }

        saveWallet(dto.getWalletType(), wallet);
        return "Credit transaction successful";
    }


    private String handleDebitTransaction(TransactionSchemeDetailsRequestDto dto,
                                          AbstractWalletAmountTable wallet,
                                          String vendorStatus,
                                          BigDecimal tranxAmount,
                                          BigDecimal grossAmount,
                                          String taxValue,
                                          String baseTransactionId,
                                          boolean isSettlement) {

        log.info("[TXN_DEBIT] Processing DEBIT flow...");

        if (!hasSufficientBalance(dto.getHierarchyId(), tranxAmount, grossAmount, dto.getWalletType())) {
            log.warn("[WALLET] Insufficient balance | RetailerID={} | Required={} | Available={}",
                    dto.getHierarchyId(), tranxAmount, wallet.getAvailableBalance());
            return "Insufficient balance";
        }

        switch (vendorStatus.toUpperCase()) {
            case "PENDING":
                return "Transaction pending - debit applied";

            case "FAILURE":
            case "FAILED":
                reverseDebit(wallet, tranxAmount, baseTransactionId, dto);
                return "Vendor transaction failed and reversed";

            default:
                if (isSettlement) {
                    updateTransactionStatus("TXN" + baseTransactionId, "SETTLEMENT_PENDING", dto.getWalletType());
                    return "Transaction marked as pending for settlement";
                }
                // On success
                updateTransactionStatus("TXN" + baseTransactionId, "SUCCESS", dto.getWalletType());
                mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), "debit", tranxAmount,
                        dto.getWalletType(), "TXN" + baseTransactionId, "Credit", "SUCCESS", dto);
                processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, "SUCCESS");
                break;
        }

        saveWallet(dto.getWalletType(), wallet);
        return "Debit transaction successful";
    }

    private boolean isCredit(String type) {
        return "credit".equalsIgnoreCase(type);
    }

    private boolean isDebit(String type) {
        return "debit".equalsIgnoreCase(type);
    }

    private Boolean validateAndFetchSettlement(String subProductCode) {
        Boolean isSettlement = subProductsRepository.findIsSettlementByCode(subProductCode);
        if (isSettlement == null) {
            log.error("[TXN_PROCESS] Invalid SubProductCode: {}", subProductCode);
            throw new IllegalArgumentException("Invalid SubProductCode: " + subProductCode);
        }
        return isSettlement;
    }

    private void insertPendingTransaction(AbstractWalletAmountTable wallet, BigDecimal amount,
                                          String baseTxnId, TransactionSchemeDetailsRequestDto dto,
                                          String remark, String type) {
        insertTransaction(wallet, "PENDING", amount,
                wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                remark, buildTransactionId("TXN", baseTxnId), type, dto);
    }

    private void insertFailedTransaction(AbstractWalletAmountTable wallet, BigDecimal amount,
                                         String baseTxnId, TransactionSchemeDetailsRequestDto dto,
                                         String remark, String type) {
        insertTransaction(wallet, "FAILURE", amount,
                wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                remark, buildTransactionId("FLD", baseTxnId), type, dto);
    }

    private void reverseDebit(AbstractWalletAmountTable wallet, BigDecimal amount,
                              String baseTxnId, TransactionSchemeDetailsRequestDto dto) {
        BigDecimal before = wallet.getAvailableBalance();
        wallet.setAvailableBalance(before.add(amount));
        wallet.setTotalCash(wallet.getTotalCash().add(amount));
        updateTransactionStatus("TXN" + baseTxnId, "FAILURE", dto.getWalletType());
        insertTransaction(wallet, "REVERSAL", amount, before, wallet.getAvailableBalance(),
                "Vendor Failed - Debit Reversed", buildTransactionId("REV", baseTxnId), "DEBIT", dto);
    }

    private static final String WALLET_PREPAID = "PREPAID";
    private static final String WALLET_POSTPAID = "POSTPAID";

    /**
     * Updates transaction status in respective wallet table based on wallet type.
     */
    private void updateTransactionStatus(String transactionId, String status, String walletType) {
        log.info("[TXN_STATUS] Updating transaction | TxnID={} | Status={} | WalletType={}", transactionId, status, walletType);

        switch (walletType.toUpperCase()) {
            case WALLET_PREPAID -> updatePrepaidTransaction(transactionId, status);
            case WALLET_POSTPAID -> updatePostpaidTransaction(transactionId, status);
            default -> throw new IllegalArgumentException("Invalid wallet type: " + walletType);
        }
    }

    private void updatePrepaidTransaction(String transactionId, String status) {
        retailerPrepaidTransactionDetailsRepository.findByTransactionId(transactionId)
                .ifPresentOrElse(txn -> {
                    txn.setActionOnBalanceAmount("Deduct Funds - PREPAID");
                    txn.setTranStatus(status);
                    retailerPrepaidTransactionDetailsRepository.save(txn);
                    log.debug("[TXN_STATUS] Updated PREPAID transaction: {}", transactionId);
                }, () -> {
                    throw new EntityNotFoundException("Prepaid transaction not found: " + transactionId);
                });
    }

    private void updatePostpaidTransaction(String transactionId, String status) {
        retailerPostpaidTransactionDetailsRepository.findByTransactionId(transactionId)
                .ifPresentOrElse(txn -> {
                    txn.setActionOnBalanceAmount("Deduct Funds - POSTPAID");
                    txn.setTranStatus(status);
                    retailerPostpaidTransactionDetailsRepository.save(txn);
                    log.debug("[TXN_STATUS] Updated POSTPAID transaction: {}", transactionId);
                }, () -> {
                    throw new EntityNotFoundException("Postpaid transaction not found: " + transactionId);
                });
    }

    private void saveWallet(String walletType, AbstractWalletAmountTable wallet) {
        if ("PREPAID".equals(walletType)) {
            retailerPrepaidAmountRepository.save((RetailerPrepaidAmount) wallet);
        } else {
            retailerPostpaidAmountRepository.save((RetailerPostpaidAmount) wallet);
        }
    }

    private AbstractWalletAmountTable getWalletByType(String walletType, String hierarchyId) {
        String hierarchyType = extractHierarchyType(hierarchyId); // e.g. "RT", "WL", "AD", etc.

        return switch (hierarchyType) {
            case "RT" -> getRetailerWallet(walletType, hierarchyId);
            case "WL" -> getWhiteLabelWallet(walletType, hierarchyId);
            case "AD", "IM" -> getAdminWallet(walletType, hierarchyId);
            // Add cases for Partner, Distributor if needed
            default -> throw new IllegalArgumentException("Unsupported hierarchy for wallet: " + hierarchyId);
        };
    }

    private AbstractWalletAmountTable getRetailerWallet(String walletType, String retailerId) {
        if ("PREPAID".equals(walletType)) {
            return retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Retailer Prepaid wallet not found: " + retailerId));
        } else if ("POSTPAID".equals(walletType)) {
            return retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Retailer Postpaid wallet not found: " + retailerId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Retailer: " + walletType);
    }

    private AbstractWalletAmountTable getWhiteLabelWallet(String walletType, String whiteLabelId) {
        if ("PREPAID".equals(walletType)) {
            return whiteLabelPrepaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                    .orElseThrow(() -> new RuntimeException("WhiteLabel Prepaid wallet not found: " + whiteLabelId));
        } else if ("POSTPAID".equals(walletType)) {
            return whiteLabelPostpaidAmountRepository.findByWhiteLabelId_WhiteLabelId(whiteLabelId)
                    .orElseThrow(() -> new RuntimeException("WhiteLabel Postpaid wallet not found: " + whiteLabelId));
        }
        throw new IllegalArgumentException("Invalid wallet type for WhiteLabel: " + walletType);
    }

    private AbstractWalletAmountTable getAdminWallet(String walletType, String adminId) {
        if ("PREPAID".equals(walletType)) {
            return adminPrepaidAmountRepository.findByAdminId_Id(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin Prepaid wallet not found: " + adminId));
        } else if ("POSTPAID".equals(walletType)) {
            return adminPostpaidAmountRepository.findByAdminId_Id(adminId)
                    .orElseThrow(() -> new RuntimeException("Admin Postpaid wallet not found: " + adminId));
        }
        throw new IllegalArgumentException("Invalid wallet type for Admin: " + walletType);
    }

    private void processChargesAndCommission(TransactionSchemeDetailsRequestDto dto,
                                             AbstractWalletAmountTable wallet,
                                             BigDecimal grossAmount,
                                             String gstValue,
                                             String baseTransactionId,
                                             String tranStatus) {

        log.info("==== [processChargesAndCommission] START ====");
        log.info("Transaction ID: {}, WalletType: {}, HierarchyId: {}, Status: {}",
                baseTransactionId, dto.getWalletType(), dto.getHierarchyId(), tranStatus);
        log.info("Input -> HasCharge: {}, IsCommission: {}, GrossAmount: {}, GST Value: {}",
                dto.getHasCharge(), dto.getIsCommission(), grossAmount, gstValue);

        try {
            if (Boolean.TRUE.equals(dto.getHasCharge())) {
                log.info("---- [Charges] Processing initiated for Transaction ID: {} ----", baseTransactionId);
                processCharges(dto, wallet, grossAmount, gstValue, baseTransactionId, tranStatus);
                log.info("---- [Charges] Processing completed for Transaction ID: {} ----", baseTransactionId);
            } else {
                log.info("---- [Charges] Skipped (HasCharge=false) ----");
            }

            if (Boolean.TRUE.equals(dto.getIsCommission())) {
                log.info("---- [Commission] Processing initiated for Transaction ID: {} ----", baseTransactionId);
                processCommission(dto, baseTransactionId, tranStatus, dto);
                log.info("---- [Commission] Processing completed for Transaction ID: {} ----", baseTransactionId);
            } else {
                log.info("---- [Commission] Skipped (IsCommission=false) ----");
            }

        } catch (Exception e) {
            log.error("**** [processChargesAndCommission] ERROR during processing Transaction ID: {} ****", baseTransactionId, e);
            throw e;
        }

        log.info("==== [processChargesAndCommission] END for Transaction ID: {} ====", baseTransactionId);
    }

    public void processCharges(TransactionSchemeDetailsRequestDto dto, AbstractWalletAmountTable wallet,
                               BigDecimal grossAmountInput, String gstValue, String baseTransactionId, String tranStatus) {

        log.info("[CHARGES] Start | BaseTxnId={} | WalletType={} | Hierarchy={} | Status={}",
                baseTransactionId, dto.getWalletType(), dto.getHierarchyId(), tranStatus);

        BigDecimal grossAmount = calculateGross(dto, grossAmountInput);
        BigDecimal gstRate = new BigDecimal(gstValue).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // Step 1: Calculate GST and base
        BigDecimal baseAmount = grossAmount.divide(BigDecimal.ONE.add(gstRate), 10, RoundingMode.HALF_UP);
        BigDecimal gstAmount = grossAmount.subtract(baseAmount).setScale(4, RoundingMode.HALF_UP);

        log.debug("[CHARGES] Split amounts | Gross={} | Base={} | GST={} | GST%={}", grossAmount, baseAmount, gstAmount, gstValue);

        // Step 2: Deduct base and GST from wallet
        deductChargeAndGST(wallet, dto, baseAmount, "CHG", "Deduct Funds (Charge)", baseTransactionId, tranStatus);
        deductChargeAndGST(wallet, dto, gstAmount, "GST", "Deduct Funds (GST)", baseTransactionId, tranStatus);

        // Step 3: Update wallet meta
        updateWalletMeta(wallet, baseAmount.add(gstAmount), gstAmount);

        // Step 4: Credit GST to admin/whitelabel
        //log.info("[CHARGES] Crediting GST {} to Admin/Whitelabel for HierarchyId={}", gstAmount, dto.getHierarchyId());
        //creditGstToAdminOrWhitelabel(dto.getHierarchyId(), dto.getWalletType(), gstAmount, baseTransactionId, tranStatus, dto);

        log.info("[CHARGES] Completed | BaseTxnId={} | WalletType={}", baseTransactionId, dto.getWalletType());
    }

    public BigDecimal calculateGross(TransactionSchemeDetailsRequestDto dto, BigDecimal grossAmountInput) {
        if ("PERCENTAGE".equalsIgnoreCase(dto.getSchemeRate().getRateType().toString())) {
            BigDecimal percentage = BigDecimal.valueOf(dto.getSchemeRate().getGross());
            BigDecimal gross = BigDecimal.valueOf(dto.getAmount())
                    .multiply(percentage)
                    .setScale(4, RoundingMode.HALF_UP);
            log.debug("[CHARGES] Gross calculated by percentage | Rate={} | Percentage={} | Gross={}",
                    dto.getSchemeRate().getRateType(), percentage, gross);
            return gross;
        }
        log.debug("[CHARGES] Gross taken as fixed | Gross={}", grossAmountInput);
        return grossAmountInput;
    }


    public void deductChargeAndGST(AbstractWalletAmountTable wallet, TransactionSchemeDetailsRequestDto dto,
                             BigDecimal amount, String prefix, String remark,
                             String baseTransactionId, String tranStatus) {

        BigDecimal before = wallet.getAvailableBalance();
        wallet.setAvailableBalance(before.subtract(amount));
        BigDecimal after = wallet.getAvailableBalance();

        String transactionId = buildTransactionId(prefix, baseTransactionId);
        log.info("[CHARGES] Deduct {} | Before={} | Amount={} | After={} | TxnId={}",
                prefix, before, amount, after, transactionId);

        insertTransaction(wallet, tranStatus, amount, before, after,
                remark + " - " + dto.getWalletType().toUpperCase(), transactionId, "DEBIT", dto);

        mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), "DEBIT", amount, dto.getWalletType(),
                prefix + baseTransactionId, "(" + prefix + ")", tranStatus, dto);
    }

    public void updateWalletMeta(AbstractWalletAmountTable wallet, BigDecimal totalDeducted, BigDecimal lastAmount) {
        wallet.setUsedCash(wallet.getUsedCash().add(totalDeducted));
        wallet.setLastUpdatedAmount(lastAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        log.debug("[WALLET_META] Updated | UsedCash={} | LastAmount={} | Time={} {}",
                wallet.getUsedCash(), wallet.getLastUpdatedAmount(),
                wallet.getLastUpdatedDate(), wallet.getLastUpdatedTime());
    }


//
//    private void processCharges(TransactionSchemeDetailsRequestDto dto, AbstractWalletAmountTable wallet,
//                                BigDecimal grossAmountInput, String gstValue, String baseTransactionId, String tranStatus) {
//
//        log.info("---- [Charges] Starting processCharges | BaseTxnId: {} | WalletType: {} | Hierarchy: {} | TranStatus: {}",
//                baseTransactionId, dto.getWalletType(), dto.getHierarchyId(), tranStatus);
//
//        BigDecimal grossAmount;
//
//        if ("PERCENTAGE".equalsIgnoreCase(dto.getSchemeRate().getRateType().toString())) {
//            BigDecimal percentage = BigDecimal.valueOf(dto.getSchemeRate().getGross());
//                    //.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
//            grossAmount = BigDecimal.valueOf(dto.getAmount())
//                    .multiply(percentage)
//                    .setScale(4, RoundingMode.HALF_UP);
//            log.debug("---- [Charges] Gross calculated as percentage | RateType: {} | Percentage: {} | GrossAmount: {}",
//                    dto.getSchemeRate().getRateType(), percentage, grossAmount);
//        } else {
//            grossAmount = grossAmountInput;
//            log.debug("---- [Charges] Gross taken as fixed | GrossAmountInput: {}", grossAmount);
//        }
//
//        // Step 1: Calculate GST and base
//        BigDecimal gstRate = new BigDecimal(gstValue)
//                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
//        BigDecimal divisor = BigDecimal.ONE.add(gstRate);
//        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);
//        BigDecimal gstAmount = grossAmount.subtract(baseAmount).setScale(4, RoundingMode.HALF_UP);
//
//        log.debug("---- [Charges] GST calculation | GSTValue: {} | GSTRate: {} | BaseAmount: {} | GSTAmount: {} | GrossAmount: {}",
//                gstValue, gstRate, baseAmount, gstAmount, grossAmount);
//
//        // Step 2: Deduct base
//        BigDecimal beforeBase = wallet.getAvailableBalance();
//        wallet.setAvailableBalance(beforeBase.subtract(baseAmount));
//        BigDecimal afterBase = wallet.getAvailableBalance();
//        log.info("---- [Charges] Deducting BaseAmount from Wallet | Before: {} | Deduct: {} | After: {}",
//                beforeBase, baseAmount, afterBase);
//
//        insertTransaction(wallet, tranStatus, baseAmount, beforeBase, afterBase,
//                "Deduct Funds (Charge) - " + dto.getWalletType().toUpperCase(),
//                buildTransactionId("CHG", baseTransactionId), "DEBIT", dto);
//
//        mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), "DEBIT", baseAmount, dto.getWalletType(),
//                "CHG" + baseTransactionId, "(Charge)", tranStatus, dto);
//
//        // Step 3: Deduct GST
//        BigDecimal beforeGst = wallet.getAvailableBalance();
//        wallet.setAvailableBalance(beforeGst.subtract(gstAmount));
//        BigDecimal afterGst = wallet.getAvailableBalance();
//        log.info("---- [Charges] Deducting GSTAmount from Wallet | Before: {} | Deduct: {} | After: {}",
//                beforeGst, gstAmount, afterGst);
//
//        insertTransaction(wallet, tranStatus, gstAmount, beforeGst, afterGst,
//                "Deduct Funds (GST) - " + dto.getWalletType().toUpperCase(),
//                buildTransactionId("GST", baseTransactionId), "DEBIT", dto);
//
//        // Step 4: Update wallet meta
//        wallet.setUsedCash(wallet.getUsedCash().add(baseAmount.add(gstAmount)));
//        wallet.setLastUpdatedAmount(gstAmount);
//        wallet.setLastUpdatedDate(LocalDate.now());
//        wallet.setLastUpdatedTime(LocalTime.now());
//
//        log.debug("---- [Charges] Wallet meta updated | UsedCash: {} | LastUpdatedAmount: {} | DateTime: {} {}",
//                wallet.getUsedCash(), wallet.getLastUpdatedAmount(),
//                wallet.getLastUpdatedDate(), wallet.getLastUpdatedTime());
//
//        // Step 5: Credit GST to Admin
//        log.info("---- [Charges] Crediting GST {} to Admin for HierarchyId: {}", gstAmount, dto.getHierarchyId());
//        creditGstToAdminOrWhitelabel(dto.getHierarchyId(), dto.getWalletType(), gstAmount,
//                baseTransactionId, tranStatus, dto);
//
//        log.info("---- [Charges] Completed processCharges | BaseTxnId: {} | WalletType: {}", baseTransactionId, dto.getWalletType());
//    }

    private void creditGstToAdmin(String receiverHierarchyId, String retailerId, String walletType,
                                  BigDecimal gstAmount, String baseTransactionId, String tranStatus,
                                  TransactionSchemeDetailsRequestDto request) {

        log.info("---- [GST] Start creditGstToAdmin | Receiver: {} | Retailer: {} | Wallet: {} | GST: {} | BaseTxnId: {}",
                receiverHierarchyId, retailerId, walletType, gstAmount, baseTransactionId);

        String hierarchyType = extractHierarchyType(receiverHierarchyId);
        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        log.debug("---- [GST] Derived hierarchyType: {} | TransactionTable: {} | AmountTable: {}",
                hierarchyType, transactionTable, amountTable);

        // Step 1: Get current balance
        BigDecimal beforeBalance = getCurrentBalance(receiverHierarchyId, walletType);
        BigDecimal afterBalance = beforeBalance.add(gstAmount);
        String gstCreditTranxId = buildTransactionId("GST", baseTransactionId);

        log.info("---- [GST] Crediting GST to Admin | BeforeBalance: {} | Credit: {} | AfterBalance: {} | TranxId: {}",
                beforeBalance, gstAmount, afterBalance, gstCreditTranxId);

        // Step 2: Insert GST credit transaction
        insertTransactionForAdminHierarchy(
                transactionTable,
                "CREDIT",
                gstAmount,
                "GST collected",
                receiverHierarchyId,
                retailerId,
                walletType,
                beforeBalance,
                afterBalance,
                "Add Funds (GST) - " + walletType.toUpperCase(),
                gstCreditTranxId, tranStatus, request, hierarchyType
        );

        log.debug("---- [GST] Transaction inserted successfully for Receiver: {} | Table: {}", receiverHierarchyId, transactionTable);

        // Step 3: Update balance table
        updateAmountTable(hierarchyType, receiverHierarchyId, amountTable, gstAmount);
        log.info("---- [GST] Balance table updated successfully for Receiver: {} | WalletType: {} | Amount: {}",
                receiverHierarchyId, walletType, gstAmount);

        log.info("---- [GST] Completed creditGstToAdmin | Receiver: {} | TranxId: {}", receiverHierarchyId, gstCreditTranxId);
    }


//    private void processCommission(TransactionSchemeDetailsRequestDto dto,
//                                   String baseTransactionId,
//                                   String tranStatus,
//                                   TransactionSchemeDetailsRequestDto request) {
//
//        log.info("==== [processCommission] START ====");
//        log.info("Transaction ID: {}, HierarchyId: {}, WalletType: {}, Status: {}, Amount: {}",
//                baseTransactionId, dto.getHierarchyId(), dto.getWalletType(), tranStatus, dto.getAmount());
//        log.info("TDS Rate: {}, SchemeRateType: {}", dto.getTdsRate(), dto.getSchemeRate().getRateType());
//
//        try {
//            BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());
//
//            // Step 1: Retailer commission
//            if (dto.getSchemeRate().getRetailer() > 0) {
//                log.info("---- [Commission Step 1] Processing Retailer commission for HierarchyId: {} ----", dto.getHierarchyId());
//                BigDecimal retailerCommission = calculateCommissionAmount(
//                        dto.getSchemeRate().getRetailer(),
//                        BigDecimal.valueOf(dto.getAmount()),
//                        dto.getSchemeRate().getRateType().toString()
//                );
//                log.info("Retailer Commission Calculated: {}", retailerCommission);
//
//                distributeCommissionToHierarchy(
//                        "RT", dto.getHierarchyId(),
//                        retailerCommission, dto.getWalletType(),
//                        tdsRate, baseTransactionId, tranStatus, request
//                );
//                log.info("Retailer Commission Distributed Successfully for HierarchyId: {}", dto.getHierarchyId());
//            } else {
//                log.info("---- [Commission Step 1] Skipped (Retailer commission rate = 0) ----");
//            }
//
//            // Step 2: Parent hierarchy commission
//            log.info("---- [Commission Step 2] Traversing and distributing commission to parent hierarchies ----");
//            Retailer retailer = retailerRepository.findByRetailerId(dto.getHierarchyId())
//                    .orElseThrow(() -> new RuntimeException("Retailer not found: " + dto.getHierarchyId()));
//
//            distributeCommissionToHierarchies(
//                    BigDecimal.valueOf(dto.getAmount()),
//                    retailer.getParentAccount(),
//                    dto.getSchemeRate(),
//                    dto.getWalletType(),
//                    tdsRate,
//                    baseTransactionId,
//                    new HashSet<>(),
//                    tranStatus,
//                    request
//            );
//            log.info("Parent Hierarchy Commission Distribution Completed for Base Transaction: {}", baseTransactionId);
//
//            // Step 3: Admin-level commission
//            if (dto.getSchemeRate().getIm() > 0) {
//                log.info("---- [Commission Step 3] Processing Admin-level commission ----");
//                BigDecimal adminCommission = calculateCommissionAmount(
//                        dto.getSchemeRate().getIm(),
//                        BigDecimal.valueOf(dto.getAmount()),
//                        dto.getSchemeRate().getRateType().toString()
//                );
//                log.info("Admin Commission Calculated: {}", adminCommission);
//
//                getAdminHierarchyId(dto.getHierarchyId()).ifPresentOrElse(adminId -> {
//                    log.info("Admin Hierarchy Found: {}. Crediting Admin Commission...", adminId);
//                    distributeCommissionToHierarchy(
//                            "AD", adminId,
//                            adminCommission, dto.getWalletType(),
//                            tdsRate, baseTransactionId, tranStatus, request
//                    );
//                    log.info("Admin Commission Distributed Successfully to AdminId: {}", adminId);
//                }, () -> log.warn("No Admin Hierarchy found for HierarchyId: {}", dto.getHierarchyId()));
//            } else {
//                log.info("---- [Commission Step 3] Skipped (Admin commission rate = 0) ----");
//            }
//
//        } catch (Exception e) {
//            log.error("**** [processCommission] ERROR during commission processing for Transaction ID: {} ****", baseTransactionId, e);
//            throw e;
//        }
//
//        log.info("==== [processCommission] END for Transaction ID: {} ====", baseTransactionId);
//    }

    public void processCommission(TransactionSchemeDetailsRequestDto dto,
                                  String baseTransactionId,
                                  String tranStatus,
                                  TransactionSchemeDetailsRequestDto request) {

        log.info("[COMMISSION] START | TxnId={} | HierarchyId={} | WalletType={} | Status={} | Amount={}",
                baseTransactionId, dto.getHierarchyId(), dto.getWalletType(), tranStatus, dto.getAmount());

        try {
            BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());
            BigDecimal txnAmount = BigDecimal.valueOf(dto.getAmount());
            String rateType = dto.getSchemeRate().getRateType().toString();

            processRetailerCommission(dto, txnAmount, tdsRate, baseTransactionId, tranStatus, request, rateType);
            processParentHierarchyCommission(dto, txnAmount, tdsRate, baseTransactionId, tranStatus, request);
            processAdminCommission(dto, txnAmount, tdsRate, baseTransactionId, tranStatus, request, rateType);

        } catch (Exception e) {
            log.error("[COMMISSION] ERROR | TxnId={} | Msg={}", baseTransactionId, e.getMessage(), e);
            throw e;
        }

        log.info("[COMMISSION] END | TxnId={} | HierarchyId={}", baseTransactionId, dto.getHierarchyId());
    }

    private void processRetailerCommission(TransactionSchemeDetailsRequestDto dto,
                                           BigDecimal txnAmount, BigDecimal tdsRate,
                                           String baseTxnId, String tranStatus,
                                           TransactionSchemeDetailsRequestDto request, String rateType) {

        BigDecimal retailerRate = BigDecimal.valueOf(dto.getSchemeRate().getRetailer());
        if (retailerRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[COMMISSION] Retailer commission skipped | Rate=0");
            return;
        }

        BigDecimal commission = calculateCommissionAmount(retailerRate.doubleValue(), txnAmount, rateType);
        log.info("[COMMISSION] Retailer Commission | HierarchyId={} | Rate={} | Amount={}",
                dto.getHierarchyId(), retailerRate, commission);

        creditHierarchy("RT", dto.getHierarchyId(), commission, dto.getWalletType(),
                tdsRate, baseTxnId, tranStatus, request);

    }


    private void processParentHierarchyCommission(TransactionSchemeDetailsRequestDto dto,
                                                  BigDecimal txnAmount, BigDecimal tdsRate,
                                                  String baseTxnId, String tranStatus,
                                                  TransactionSchemeDetailsRequestDto request) {

        Retailer retailer = retailerRepository.findByRetailerId(dto.getHierarchyId())
                .orElseThrow(() -> new RuntimeException("Retailer not found: " + dto.getHierarchyId()));

        log.info("[COMMISSION] Starting parent commission traversal | ParentId={}", retailer.getParentAccount());

        distributeToHierarchyChain(txnAmount, retailer.getParentAccount(), dto.getSchemeRate(),
                dto.getWalletType(), tdsRate, baseTxnId, new HashSet<>(), tranStatus, request);
    }

    private void processAdminCommission(TransactionSchemeDetailsRequestDto dto,
                                        BigDecimal txnAmount, BigDecimal tdsRate,
                                        String baseTxnId, String tranStatus,
                                        TransactionSchemeDetailsRequestDto request, String rateType) {

        BigDecimal adminRate = BigDecimal.valueOf(dto.getSchemeRate().getIm());
        if (adminRate.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("[COMMISSION] Admin commission skipped | Rate=0");
            return;
        }

        BigDecimal adminCommission = calculateCommissionAmount(adminRate.doubleValue(), txnAmount, rateType);
        log.info("[COMMISSION] Admin Commission | Rate={} | Amount={}", adminRate, adminCommission);

        getAdminHierarchyId(dto.getHierarchyId()).ifPresentOrElse(adminId -> {
            creditHierarchy("AD", adminId, adminCommission, dto.getWalletType(),
                    tdsRate, baseTxnId, tranStatus, request);
        }, () -> log.warn("[COMMISSION] Admin not found for HierarchyId={}", dto.getHierarchyId()));
    }


    private void creditHierarchy(String hierarchyType, String hierarchyId, BigDecimal commissionAmount,
                                 String walletType, BigDecimal tdsRate, String baseTxnId,
                                 String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[COMMISSION] Crediting {} | Hierarchy={} | Amount={} | Wallet={} | Status={}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tranStatus);

        boolean skipTdsAndMirror = shouldSkipTdsAndMirror(hierarchyType, hierarchyId);

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        BigDecimal before = getCurrentBalance(hierarchyId, walletType);
        BigDecimal after = before.add(commissionAmount);
        String comTxnId = buildTransactionId("COM", baseTxnId);

        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Commission credited", hierarchyId, walletType, before, after,
                "Add Funds (Commission) - " + walletType, comTxnId, tranStatus, request);

        updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

        // Handle Mirror + TDS only if required
        if (!skipTdsAndMirror && !"PENDING".equalsIgnoreCase(tranStatus)) {
            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "DEBIT",
                    commissionAmount, walletType, comTxnId,
                    "Deduct Funds (Commission) - " + walletType, tranStatus, request);

            applyTds(hierarchyType, hierarchyId, commissionAmount, walletType,
                    tdsRate, baseTxnId, tranStatus, request);
        }
    }


    private void distributeToHierarchyChain(BigDecimal amount, String hierarchyId, SchemeRate schemeRate,
                                            String walletType, BigDecimal tdsRate, String baseTxnId,
                                            Set<String> visited, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        if (hierarchyId == null || hierarchyId.isEmpty() || !visited.add(hierarchyId)) return;

        String type = extractHierarchyType(hierarchyId);
        BigDecimal rate = BigDecimal.valueOf(getCommissionRateByType(type, schemeRate));
        BigDecimal commission = calculateCommissionAmount(rate.doubleValue(), amount, schemeRate.getRateType().toString());

        if (commission.compareTo(BigDecimal.ZERO) <= 0) return;

        if (isFinalAdmin(type, hierarchyId)) {
            creditHierarchy("AD", hierarchyId, commission, walletType, tdsRate, baseTxnId, tranStatus, request);
            return;
        }

        creditHierarchy(type, hierarchyId, commission, walletType, tdsRate, baseTxnId, tranStatus, request);

        findParentHierarchy(hierarchyId).ifPresent(parent ->
                distributeToHierarchyChain(amount, parent, schemeRate, walletType, tdsRate, baseTxnId, visited, tranStatus, request)
        );
    }


    private void applyTds(String hierarchyType, String hierarchyId, BigDecimal commissionAmount, String walletType,
                          BigDecimal tdsRate, String baseTxnId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        BigDecimal tdsAmount = commissionAmount.multiply(tdsRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        if (tdsAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal before = getCurrentBalance(hierarchyId, walletType);
        BigDecimal after = before.subtract(tdsAmount);
        String tdsTxnId = buildTransactionId("TDS", baseTxnId);

        insertTransactionForHierarchy(getTransactionTable(hierarchyType, walletType), "DEBIT", tdsAmount,
                "TDS Deduction", hierarchyId, walletType, before, after,
                "Deduct Funds (TDS) - " + walletType, tdsTxnId, tranStatus, request);

        mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "CREDIT", tdsAmount,
                walletType, tdsTxnId, "Add Funds (TDS) - " + walletType, tranStatus, request);

        updateAmountTable(hierarchyType, hierarchyId, getAmountTable(hierarchyType, walletType), tdsAmount.negate());
    }

    private boolean shouldSkipTdsAndMirror(String type, String id) {
        if ("IM".equals(type) || "AD".equals(type)) return true;

        if ("WL".equals(type)) {
            return whiteLabelsRepository.findById(id)
                    .map(WhiteLabels::getIsAdmin)
                    .orElse(false);
        }
        return false;
    }

    private boolean isFinalAdmin(String type, String id) {
        if ("AD".equals(type) || "IM".equals(type)) return true;

        if ("WL".equals(type)) {
            return whiteLabelsRepository.findById(id)
                    .map(WhiteLabels::getIsAdmin)
                    .orElse(false);
        }
        return false;
    }

    private Optional<String> getAdminHierarchyId(String startHierarchyId) {
        String currentId = startHierarchyId;

        while (currentId != null && !currentId.isEmpty()) {
            String type = extractHierarchyType(currentId);
            if ("AD".equals(type)) return Optional.of(currentId);

            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isPresent()) {
                currentId = parentOpt.get();
            } else {
                break;
            }
        }

        return Optional.empty(); // Admin not found
    }

    public boolean hasSufficientBalance(String retailerId, BigDecimal requestedAmount, BigDecimal grossAmount, String walletType) {

        String amountTable = getAmountTable("RT", walletType);

        String sql = "SELECT available_balance FROM " + amountTable + " WHERE retailer_id = :retailerId";

        BigDecimal availableBalance = (BigDecimal) entityManager.createNativeQuery(sql)
                .setParameter("retailerId", retailerId)
                .getSingleResult();

        BigDecimal totalRequired = requestedAmount.add(grossAmount);

        log.info("Retailer ID: {} | Available Balance: {} | Required Amount: {}",
                retailerId, availableBalance, totalRequired);

        // Return TRUE only when the balance is enough
        return availableBalance != null && availableBalance.compareTo(totalRequired) >= 0;
    }

    private void distributeCommissionToHierarchies(BigDecimal amount,
                                                   String hierarchyId,
                                                   SchemeRate schemeRate,
                                                   String walletType,
                                                   BigDecimal tdsRate,
                                                   String baseTransactionId,
                                                   Set<String> processedMirrorSet,
                                                   String tranStatus,
                                                   TransactionSchemeDetailsRequestDto request) {

        log.info("==== [distributeCommissionToHierarchies] START for HierarchyId: {} ====", hierarchyId);

        // Step 1: Validation
        if (hierarchyId == null || hierarchyId.isEmpty()) {
            log.warn("HierarchyId is null or empty — skipping commission distribution.");
            return;
        }

        // Step 2: Avoid duplicate processing
        if (!processedMirrorSet.add(hierarchyId)) {
            log.info("Skipping duplicate commission processing for hierarchy: {}", hierarchyId);
            return;
        }

        try {
            String hierarchyType = extractHierarchyType(hierarchyId);
            log.info("Processing hierarchyId: {}, Type: {}", hierarchyId, hierarchyType);

            // Step 3: Calculate commission
            double rate = getCommissionRateByType(hierarchyType, schemeRate);
            BigDecimal commissionAmount = calculateCommissionAmount(rate, amount, schemeRate.getRateType().toString());
            log.info("Calculated commission for hierarchy {} -> Rate: {}, Amount: {}", hierarchyId, rate, commissionAmount);

            // Step 4: Handle commission distribution
            if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
                if ("AD".equals(hierarchyType)) {
                    log.info("Hierarchy {} is Admin. Crediting directly to Admin wallet.", hierarchyId);
                    distributeCommissionToAdmin(hierarchyId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                    log.info("Admin commission credited successfully. Stopping recursion here.");
                    return; // Stop recursion
                }

                if ("WL".equals(hierarchyType)) {
                    log.info("Hierarchy {} is a WhiteLabel. Checking if it's also Admin...", hierarchyId);
                    WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                            .orElseThrow(() -> new RuntimeException("Whitelabel not found: " + hierarchyId));

                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        log.info("Whitelabel {} isAdmin = true → Crediting full commission and stopping traversal.", hierarchyId);
                        distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId, tranStatus, request);
                        return; // Stop recursion
                    }
                }

                // Default case: Normal hierarchy level
                log.info("Crediting commission to hierarchy {} of type {} | Amount: {}", hierarchyId, hierarchyType, commissionAmount);
                distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId, tranStatus, request);
                log.info("Commission distributed successfully to hierarchy {}.", hierarchyId);
            } else {
                log.info("No commission generated for hierarchy {} (rate=0 or computed 0 amount).", hierarchyId);
            }

            // Step 5: Continue traversal to parent
            Optional<String> parentIdOpt = findParentHierarchy(hierarchyId);
            if (parentIdOpt.isPresent()) {
                String parentId = parentIdOpt.get();
                String parentType = extractHierarchyType(parentId);
                log.info("Found parent: {} (Type: {}) → continuing commission distribution upward.", parentId, parentType);

                distributeCommissionToHierarchies(amount, parentId, schemeRate, walletType, tdsRate, baseTransactionId, processedMirrorSet, tranStatus, request);
            } else {
                log.warn("No parent found for hierarchyId: {} → stopping traversal.", hierarchyId);
            }

        } catch (Exception e) {
            log.error("**** [distributeCommissionToHierarchies] ERROR while processing hierarchyId: {} | Transaction ID: {} ****",
                    hierarchyId, baseTransactionId, e);
            throw e;
        }

        log.info("==== [distributeCommissionToHierarchies] END for HierarchyId: {} ====", hierarchyId);
    }

    private void mirrorTransactionToWhitelabelOrAdmin(
            String sourceHierarchyId, String sourceHierarchyType, String txnType, BigDecimal amount, String walletType,
            String mirrorTxnId, String actionOnBalance, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[mirrorTransactionToWhitelabelOrAdmin] Start | SourceID: {} | Type: {} | TxnType: {} | Amount: {} | Wallet: {} | TxnId: {}",
                sourceHierarchyId, sourceHierarchyType, txnType, amount, walletType, mirrorTxnId);

        String currentId = sourceHierarchyId;
        int level = 0; // track upward traversal depth

        while (true) {
            level++;
            log.debug("[mirrorTransactionToWhitelabelOrAdmin] Level {} | Current ID: {}", level, currentId);

            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("[mirrorTransactionToWhitelabelOrAdmin] No parent found for hierarchy: {} | Stopping mirror process.", currentId);
                return;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);
            log.info("[mirrorTransactionToWhitelabelOrAdmin] Found Parent | ID: {} | Type: {}", parentId, parentType);

            // Defensive check for invalid hierarchy type
            if ("UNKNOWN".equals(parentType)) {
                log.error("[mirrorTransactionToWhitelabelOrAdmin] Invalid hierarchy type for ID: {} | Aborting mirror transaction.", parentId);
                return;
            }

            // Compute balances
            BigDecimal beforeBalance = getCurrentBalance(parentId, walletType);
            BigDecimal afterBalance = txnType.equalsIgnoreCase("CREDIT")
                    ? beforeBalance.add(amount)
                    : beforeBalance.subtract(amount);

            log.debug("[mirrorTransactionToWhitelabelOrAdmin] Parent Balance Update | Before: {} | After: {} | TxnType: {}", beforeBalance, afterBalance, txnType);

            String remarks = String.format(
                    "Mirror %s Entry for %s (%s) - From: %s",
                    txnType, sourceHierarchyType, walletType, sourceHierarchyId
            );

            // === CASE 1: WhiteLabel Handling ===
            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);

                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    boolean isAdminWL = Boolean.TRUE.equals(wl.getIsAdmin());
                    log.debug("[mirrorTransactionToWhitelabelOrAdmin] WhiteLabel Found | ID: {} | isAdmin: {}", parentId, isAdminWL);

                    if (isAdminWL) {
                        log.info("[mirrorTransactionToWhitelabelOrAdmin] Admin WL detected (ID: {}). Creating mirror transaction.", parentId);

                        insertTransactionForHierarchy(
                                getTransactionTable("WL", walletType),
                                txnType,
                                amount,
                                remarks,
                                parentId,
                                walletType,
                                beforeBalance,
                                afterBalance,
                                actionOnBalance,
                                mirrorTxnId,
                                tranStatus,
                                request
                        );

                        updateAmountTable("WL", parentId, getAmountTable("WL", walletType),
                                afterBalance.subtract(beforeBalance));

                        log.info("[mirrorTransactionToWhitelabelOrAdmin] Mirror transaction completed for Admin WL: {} | Type: {} | Amount: {}",
                                parentId, txnType, amount);
                        return;
                    } else {
                        log.debug("[mirrorTransactionToWhitelabelOrAdmin] WL {} is not Admin. Moving up hierarchy.", parentId);
                        currentId = parentId;
                        continue;
                    }
                } else {
                    log.error("[mirrorTransactionToWhitelabelOrAdmin] WhiteLabel not found in repository for ID: {}. Aborting mirror.", parentId);
                    return;
                }
            }

            // === CASE 2: IM or AD Handling ===
            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                log.info("[mirrorTransactionToWhitelabelOrAdmin] Found Admin/IM Parent | ID: {} | Type: {} | Creating mirror entry.", parentId, parentType);

                insertTransactionForHierarchy(
                        getTransactionTable(parentType, walletType),
                        txnType,
                        amount,
                        remarks,
                        parentId,
                        walletType,
                        beforeBalance,
                        afterBalance,
                        actionOnBalance,
                        mirrorTxnId,
                        tranStatus,
                        request
                );

                updateAmountTable(parentType, parentId, getAmountTable(parentType, walletType),
                        afterBalance.subtract(beforeBalance));

                log.info("[mirrorTransactionToWhitelabelOrAdmin] Mirror transaction completed for {}: {} | TxnType: {} | Amount: {}",
                        parentType, parentId, txnType, amount);
                return;
            }

            // === CASE 3: Continue Upward ===
            log.debug("[mirrorTransactionToWhitelabelOrAdmin] Moving up from {} (Type: {}) → Parent: {} (Type: {})",
                    currentId, sourceHierarchyType, parentId, parentType);
            currentId = parentId;
        }
    }


//    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
//                                                 BigDecimal commissionAmount, String walletType,
//                                                 BigDecimal tdsRate, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
//
//        log.info("Processing commission for {} | Hierarchy ID: {} | Amount (Incl. TDS): {} | Wallet Type: {} | TDS Rate: {}",
//                hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);
//
//        // === CASE 1: IM or AD — Credit only, no TDS or Mirror ===
//        if ("IM".equals(hierarchyType) || "AD".equals(hierarchyType)) {
//            log.info("{} hierarchy detected. Crediting full commission without TDS or mirror.", hierarchyType);
//
//            String transactionTable = getTransactionTable(hierarchyType, walletType);
//            String amountTable = getAmountTable(hierarchyType, walletType);
//
//            BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
//            BigDecimal afterBalance = beforeBalance.add(commissionAmount);
//            String txnId = buildTransactionId("COM", baseTransactionId);
//
//            insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
//                    "Commission credited (Direct IM/AD)", hierarchyId, walletType,
//                    beforeBalance, afterBalance,
//                    "Add Funds (Commission) - " + walletType.toUpperCase(), txnId, tranStatus, request);
//
//            updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);
//
//            return; // Stop further processing (no TDS or mirror)
//        }
//
//        // === CASE 2: WhiteLabel isAdmin = true — Credit only, no TDS or Mirror ===
//        boolean isAdminWL = false;
//        if ("WL".equals(hierarchyType)) {
//            isAdminWL = whiteLabelsRepository.findById(hierarchyId)
//                    .map(WhiteLabels::getIsAdmin)
//                    .orElse(false);
//
//            if (isAdminWL) {
//                log.info("WhiteLabel {} isAdmin = true. Crediting full commission, skipping TDS and mirror.", hierarchyId);
//
//                String transactionTable = getTransactionTable(hierarchyType, walletType);
//                String amountTable = getAmountTable(hierarchyType, walletType);
//
//                BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
//                BigDecimal afterBalance = beforeBalance.add(commissionAmount);
//                String txnId = buildTransactionId("COM", baseTransactionId);
//
//                insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
//                        "Commission credited (Admin WL)", hierarchyId, walletType,
//                        beforeBalance, afterBalance,
//                        "Add Funds (Commission) - " + walletType.toUpperCase(), txnId, tranStatus, request);
//
//                updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);
//
//                return; // Stop further processing (no TDS or mirror)
//            }
//        }
//
//        // === CASE 3: All other hierarchies — Credit, Mirror, TDS (if applicable) ===
//        String transactionTable = getTransactionTable(hierarchyType, walletType);
//        String amountTable = getAmountTable(hierarchyType, walletType);
//
//        BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
//        BigDecimal afterCreditBalance = beforeBalance.add(commissionAmount);
//        String comTxnId = buildTransactionId("COM", baseTransactionId);
//
//        // Credit commission
//        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
//                "Commission credited", hierarchyId, walletType,
//                beforeBalance, afterCreditBalance,
//                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, tranStatus, request);
//
//        updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);
//
//        // Mirror commission (DEBIT from Admin/WL)
//        if (!"pending".equalsIgnoreCase(tranStatus)) {
//            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "DEBIT", commissionAmount, walletType,
//                    comTxnId, "Deduct Funds (Commission) - " + walletType.toUpperCase(), tranStatus, request);
//        }
//
//        // TDS deduction (if applicable)
//        BigDecimal tdsAmount = commissionAmount
//                .multiply(tdsRate.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP))
//                .setScale(2, RoundingMode.HALF_UP);
//
//        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
//            BigDecimal afterTdsBalance = afterCreditBalance.subtract(tdsAmount);
//            String tdsTxnId = buildTransactionId("TDS", baseTransactionId);
//
//            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount,
//                    "TDS Deduction", hierarchyId, walletType,
//                    afterCreditBalance, afterTdsBalance,
//                    "Deduct Funds (TDS) - " + walletType.toUpperCase(), tdsTxnId, tranStatus, request);
//
//            // Mirror TDS (CREDIT to Admin/WL)
//            if (!"pending".equalsIgnoreCase(tranStatus)) {
//                mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "CREDIT", tdsAmount, walletType,
//                        tdsTxnId, "Add Funds (TDS) - " + walletType.toUpperCase(), tranStatus, request);
//            }
//            updateAmountTable(hierarchyType, hierarchyId, amountTable, tdsAmount.negate());
//        }
//    }

    private void distributeCommissionToAdmin(String hierarchyId, BigDecimal commissionAmount, String walletType,
                                             String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        log.info("[distributeCommissionToAdmin] Start | Hierarchy ID: {} | Amount: {} | Wallet: {} | BaseTxnId: {} | TranStatus: {}",
                hierarchyId, commissionAmount, walletType, baseTransactionId, tranStatus);

        String currentId = hierarchyId;
        int level = 0; // track depth for debugging

        while (true) {
            level++;
            log.debug("[distributeCommissionToAdmin] Level {} | Current ID: {}", level, currentId);

            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("[distributeCommissionToAdmin] No parent found for ID: {}. Ending commission distribution.", currentId);
                return;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            log.info("[distributeCommissionToAdmin] Found Parent | ID: {} | Type: {}", parentId, parentType);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    log.debug("[distributeCommissionToAdmin] WhiteLabel details | ID: {} | isAdmin: {}", parentId, wl.getIsAdmin());

                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        log.info("[distributeCommissionToAdmin] Final Admin WhiteLabel detected (isAdmin=true). Crediting commission.");
                        creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                        return;
                    } else {
                        log.debug("[distributeCommissionToAdmin] WL isAdmin=false. Continuing hierarchy traversal.");
                    }
                } else {
                    log.warn("[distributeCommissionToAdmin] WhiteLabel not found in repository for ID: {}. Continuing search.", parentId);
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                log.info("[distributeCommissionToAdmin] Final Admin ({}). Crediting direct commission.", parentType);
                creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                return;
            }

            currentId = parentId; // continue climbing the hierarchy
        }
    }

//    private void creditCommissionToFinalAdmin(String adminId, BigDecimal commissionAmount, String walletType,
//                                              String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
//        log.info("[creditCommissionToFinalAdmin] Start | Admin ID: {} | Amount: {} | Wallet: {} | BaseTxnId: {}",
//                adminId, commissionAmount, walletType, baseTransactionId);
//
//        BigDecimal beforeTransactionBalance = getCurrentBalance(adminId, walletType);
//        BigDecimal afterTransactionBalance = beforeTransactionBalance.add(commissionAmount);
//
//        log.debug("[creditCommissionToFinalAdmin] Before Balance: {} | After Balance: {}", beforeTransactionBalance, afterTransactionBalance);
//
//        String amountTable = getAmountTable("AD", walletType);
//        String transactionTable = getTransactionTable("AD", walletType);
//        String comTxnId = buildTransactionId("COM", baseTransactionId);
//
//        log.info("[creditCommissionToFinalAdmin] Prepared tables | TransactionTable: {} | AmountTable: {} | TxnId: {}",
//                transactionTable, amountTable, comTxnId);
//
//        insertTransactionForAdminHierarchy(
//                transactionTable, "CREDIT", commissionAmount,
//                "Admin Commission credited", adminId, walletType,
//                beforeTransactionBalance, afterTransactionBalance,
//                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, tranStatus, request
//        );
//
//        log.info("[creditCommissionToFinalAdmin] Transaction inserted successfully for Admin ID: {}", adminId);
//
//        updateAmountTable("AD", adminId, amountTable, commissionAmount);
//        log.info("[creditCommissionToFinalAdmin] Balance updated successfully | Admin ID: {} | Added Amount: {}", adminId, commissionAmount);
//
//        log.info("[creditCommissionToFinalAdmin] End | Admin ID: {} | Final Balance: {}", adminId, afterTransactionBalance);
//    }

    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
                                                 BigDecimal commissionAmount, String walletType,
                                                 BigDecimal tdsRate, String baseTransactionId,
                                                 String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[COMMISSION] Start | Type={} | ID={} | Amount={} | Wallet={} | Status={}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tranStatus);

        // CASE 1: Skip Mirror/TDS for Admin/IM or Admin Whitelabel
        if (shouldSkipMirrorAndTds(hierarchyType, hierarchyId)) {
            creditCommission(hierarchyType, hierarchyId, commissionAmount, walletType,
                    baseTransactionId, tranStatus, request, "Direct Credit - No TDS/Mirror");
            return;
        }

        // CASE 2: Default flow (Credit + Mirror + TDS)
        creditCommission(hierarchyType, hierarchyId, commissionAmount, walletType,
                baseTransactionId, tranStatus, request, "Commission Credit");

        if (!"PENDING".equalsIgnoreCase(tranStatus)) {
            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "DEBIT",
                    commissionAmount, walletType,
                    buildTransactionId("COM", baseTransactionId),
                    "Deduct Funds (Commission) - " + walletType.toUpperCase(),
                    tranStatus, request);
        }

        applyTdsDeduction(hierarchyType, hierarchyId, commissionAmount, walletType,
                tdsRate, baseTransactionId, tranStatus, request);
    }


    private void creditCommission(String hierarchyType, String hierarchyId,
                                  BigDecimal amount, String walletType,
                                  String baseTransactionId, String tranStatus,
                                  TransactionSchemeDetailsRequestDto request, String remark) {

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);
        BigDecimal before = getCurrentBalance(hierarchyId, walletType);
        BigDecimal after = before.add(amount);
        String txnId = buildTransactionId("COM", baseTransactionId);

        insertTransactionForHierarchy(transactionTable, "CREDIT", amount,
                remark, hierarchyId, walletType,
                before, after,
                "Add Funds (Commission) - " + walletType.toUpperCase(),
                txnId, tranStatus, request);

        updateAmountTable(hierarchyType, hierarchyId, amountTable, amount);

        log.info("[COMMISSION] Credited {} | Type={} | ID={} | Amount={} | Before={} | After={}",
                remark, hierarchyType, hierarchyId, amount, before, after);
    }

    private void applyTdsDeduction(String hierarchyType, String hierarchyId, BigDecimal commissionAmount,
                                   String walletType, BigDecimal tdsRate, String baseTransactionId,
                                   String tranStatus, TransactionSchemeDetailsRequestDto request) {

        BigDecimal tdsAmount = commissionAmount
                .multiply(tdsRate)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.HALF_UP);

        if (tdsAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        BigDecimal before = getCurrentBalance(hierarchyId, walletType);
        BigDecimal after = before.subtract(tdsAmount);
        String txnId = buildTransactionId("TDS", baseTransactionId);

        insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount,
                "TDS Deduction", hierarchyId, walletType,
                before, after,
                "Deduct Funds (TDS) - " + walletType.toUpperCase(), txnId, tranStatus, request);

        updateAmountTable(hierarchyType, hierarchyId, amountTable, tdsAmount.negate());

        if (!"PENDING".equalsIgnoreCase(tranStatus)) {
            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "CREDIT", tdsAmount, walletType,
                    txnId, "Add Funds (TDS) - " + walletType.toUpperCase(), tranStatus, request);
        }

        log.info("[TDS] Applied | Hierarchy={} | Amount={} | Rate={}%", hierarchyId, tdsAmount, tdsRate);
    }

    private boolean shouldSkipMirrorAndTds(String hierarchyType, String hierarchyId) {
        if ("IM".equals(hierarchyType) || "AD".equals(hierarchyType)) return true;
        if ("WL".equals(hierarchyType)) {
            return whiteLabelsRepository.findById(hierarchyId)
                    .map(WhiteLabels::getIsAdmin)
                    .orElse(false);
        }
        return false;
    }

    private void creditCommissionToFinalAdmin(String adminId, BigDecimal commissionAmount,
                                              String walletType, String baseTransactionId,
                                              String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[ADMIN_CREDIT] Final Admin | ID={} | Amount={}", adminId, commissionAmount);
        creditCommission("AD", adminId, commissionAmount, walletType,
                baseTransactionId, tranStatus, request, "Admin Commission credited");
    }

    public String extractHierarchyType(String hierarchyId) {
        String[] parts = hierarchyId.split("-");
        return (parts.length >= 2) ? parts[1] : "UNKNOWN";
    }

    private Optional<String> findParentHierarchy(String hierarchyId) {
        String hierarchyType = extractHierarchyType(hierarchyId);

        // Stop processing if the hierarchy is "Admin"
        if ("AD".equals(hierarchyType)) {
            log.info("Reached Admin level. No further processing.");
            return Optional.empty();
        }

        Optional<?> entity = switch (hierarchyType) {
            case "ET" -> enterprisesRepository.findById(hierarchyId);
            case "WL" -> whiteLabelsRepository.findById(hierarchyId);
            case "SD" -> superDistributorRepository.findById(hierarchyId);
            case "PT" -> partnerRepository.findById(hierarchyId);
            case "DT" -> distributorRepository.findById(hierarchyId);
            case "RT" -> retailerRepository.findById(hierarchyId);
            default -> Optional.empty();
        };

        return entity.map(this::extractParentAccount);
    }

    private String extractParentAccount(Object entity) {
        try {
            Field parentField = entity.getClass().getDeclaredField("parentAccount");
            parentField.setAccessible(true);
            return (String) parentField.get(entity);
        } catch (Exception e) {
            log.error("Error extracting parent account from entity: {}", entity.getClass().getSimpleName(), e);
            return null;
        }
    }

    public String getTransactionTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);

        String tableName = entityName +
                (walletType.equalsIgnoreCase("PREPAID")
                        ? "_prepaid_transaction_details"
                        : "_postpaid_transaction_details");

        log.debug("[getTransactionTable] HierarchyType: {} | WalletType: {} | Resolved Table: {}",
                hierarchyType, walletType, tableName);

        return tableName;
    }

    public String getAmountTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);

        String tableName = entityName +
                (walletType.equalsIgnoreCase("PREPAID")
                        ? "_prepaid_amount"
                        : "_postpaid_amount");

        log.debug("[getAmountTable] HierarchyType: {} | WalletType: {} | Resolved Table: {}",
                hierarchyType, walletType, tableName);

        return tableName;
    }


    private String getEntityName(String hierarchyType) {
        return switch (hierarchyType) {
            case "AD" -> "admin";
            case "ET" -> "enterprises";
            case "WL" -> "white_label";
            case "SD" -> "super_distributor";
            case "PT" -> "partner";
            case "DT" -> "distributor";
            case "RT" -> "retailer";
            default -> throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        };
    }

    @Transactional
    private void insertTransactionForAdminHierarchy(
            String transactionTable, String service, BigDecimal amount, String remarks, String adminId, String hierarchyId,
            String walletType, BigDecimal beforeBalance, BigDecimal afterBalance,
            String actionOnBalanceAmount, String tranxId, String tranStatus, TransactionSchemeDetailsRequestDto request, String hierarhcyType) {

        log.info("[ADMIN_TXN_INSERT] Starting insert for Admin Hierarchy | Table={} | HierarchyID={} | Amount={} | WalletType={} | TranxID={}",
                transactionTable, hierarchyId, amount, walletType, tranxId);

        String hierarchyColumn = switch (hierarhcyType) {
            case "AD" -> "admin_id";
            case "WL" -> "white_label_id";
            default -> throw new IllegalArgumentException(
                    "Unsupported hierarchy type: " + hierarhcyType);
        };

        String operatorName = request.getOperatorName();
        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();

        String sql = "INSERT INTO " + transactionTable +
                " (" + hierarchyColumn + ", hierarchy_id, service, amount, remarks, " +
                "date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, " +
                "final_balance, transaction_id, tran_status, operator_name, latitude, longitude) " +
                "VALUES (:adminId, :hierarchyId, :service, :amount, :remarks, " +
                ":dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, " +
                ":actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, " +
                ":operatorName, :latitude, :longitude)";

        log.debug("[ADMIN_TXN_INSERT] Generated SQL: {}", sql);

        try {
            entityManager.createNativeQuery(sql)
                    .setParameter("adminId", adminId)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("remarks", remarks)
                    .setParameter("dateOfTransaction", java.sql.Date.valueOf(LocalDate.now()))
                    .setParameter("timeOfTransaction", java.sql.Time.valueOf(LocalTime.now()))
                    .setParameter("beforeTransactionBalance", beforeBalance)
                    .setParameter("afterTransactionBalance", afterBalance)
                    .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                    .setParameter("finalBalance", afterBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("operatorName", operatorName)
                    .setParameter("latitude", latitude)
                    .setParameter("longitude", longitude)
                    .executeUpdate();

            log.info("[ADMIN_TXN_INSERT] Successfully inserted transaction for Admin | HierarchyID={} | TranxID={} | Amount={} | Status={}",
                    hierarchyId, tranxId, amount, tranStatus);

        } catch (PersistenceException e) {

            if (e.getMessage().contains("constraint") || e.getMessage().contains("Duplicate")) {
                log.warn("Duplicate transaction skipped (DB constraint) | TranxID={}", tranxId);
                return;
            }

            log.error("[ADMIN_TXN_INSERT] Failed to insert transaction for Admin | HierarchyID={} | TranxID={} | Error={}",
                    hierarchyId, tranxId, e.getMessage(), e);
            throw e;
        }
    }

    public void insertTransactionForHierarchy(
            String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId,
            String walletType, BigDecimal beforeTransactionBalance, BigDecimal afterTransactionBalance,
            String actionOnBalance, String tranxId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[COMM_TXN_INSERT] Starting insert for Hierarchy | Table={} | HierarchyID={} | Type={} | Amount={} | TranxID={} | Status={}",
                transactionTable, hierarchyId, service, amount, tranxId, tranStatus);

        SubProducts subProduct = subProductsRepository.findBySubProductCode(request.getSubProductCode())
                .orElseThrow(() -> new ResourceNotFoundException("SubProduct not found: " + request.getSubProductCode()));

        Scheme scheme = schemeRepository.findBySchemeCode(request.getSchemeRate().getSchemeCode())
                .orElseThrow(() -> new RuntimeException("Scheme Not Found"));

        String subProductName = subProduct.getSubProductTitle();
        String operatorName = request.getOperatorName();

        String hierarchyType = extractHierarchyType(hierarchyId);
        String hierarchyColumnName = transactionHelperService.getHierarchyColumnName(hierarchyType);

        Double latitude = request.getLatitude();
        Double longitude = request.getLongitude();
        log.debug("[COMM_TXN_INSERT] Detected Hierarchy Type={} | ColumnName={}", hierarchyType, hierarchyColumnName);

        String sql = "INSERT INTO " + transactionTable +
                " (" + hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, sub_product_name, scheme_name, operator_name, latitude, longitude) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalance, :finalBalance, :tranxId, :tranStatus, :subProductName, :schemeName, :operatorName, :latitude, :longitude)";

        log.debug("[COMM_TXN_INSERT] Generated SQL: {}", sql);

        try {
            entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("remarks", remarks)
                    .setParameter("dateOfTransaction", new Date())
                    .setParameter("timeOfTransaction", new Time(System.currentTimeMillis()))
                    .setParameter("beforeTransactionBalance", beforeTransactionBalance)
                    .setParameter("afterTransactionBalance", afterTransactionBalance)
                    .setParameter("actionOnBalance", actionOnBalance)
                    .setParameter("finalBalance", afterTransactionBalance)
                    .setParameter("tranxId", tranxId)
                    .setParameter("tranStatus", tranStatus)
                    .setParameter("subProductName", subProductName)
                    .setParameter("schemeName", scheme.getSchemeName())
                    .setParameter("operatorName", operatorName)
                    .setParameter("latitude", latitude)
                    .setParameter("longitude", longitude)
                    .executeUpdate();

            log.info("[COMM_TXN_INSERT] Successfully inserted commission transaction | HierarchyID={} | TranxID={} | Amount={} | Status={}",
                    hierarchyId, tranxId, amount, tranStatus);

        } catch (PersistenceException e) {

            if (e.getMessage().contains("constraint") || e.getMessage().contains("Duplicate")) {
                log.warn("Duplicate transaction skipped (DB constraint) | TranxID={}", tranxId);
                return;
            }

            log.error("[COMM_TXN_INSERT] Failed to insert commission transaction | HierarchyID={} | TranxID={} | Error={}",
                    hierarchyId, tranxId, e.getMessage(), e);
            throw e;
        }
    }

    public void updateAmountTable(String hierarchyType, String hierarchyId, String amountTable, BigDecimal amount) {
        log.info("Updating {} | Hierarchy Type: {} | Hierarchy ID: {} | Final Amount to Add: {}",
                amountTable, hierarchyType, hierarchyId, amount);

        String columnName = transactionHelperService.getHierarchyColumnName(hierarchyType);

        String sql = "UPDATE " + amountTable + " SET " +
                "available_balance = available_balance + :amount, " +
                "last_updated_amount = :amount, " +
                " last_updated_date = :date, " +
                " last_updated_time = :time " +
                "WHERE " + columnName + " = :hierarchyId";

        int updated = entityManager.createNativeQuery(sql)
                .setParameter("amount", amount)
                .setParameter("date", new Date())
                .setParameter("time", new Time(System.currentTimeMillis()))
                .setParameter("hierarchyId", hierarchyId)
                .executeUpdate();

        entityManager.flush();

        if (updated == 0) {
            throw new RuntimeException("Wallet update failed for: " + hierarchyId);
        }

    }

    public BigDecimal getCurrentBalance(String hierarchyId, String walletType) {
        String amountTable = getAmountTable(extractHierarchyType(hierarchyId), walletType);
        String columnName = transactionHelperService.getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "SELECT available_balance FROM " + amountTable + " WHERE " + columnName + " = :hierarchyId";

        try {
            return (BigDecimal) entityManager.createNativeQuery(sql)
                    .setParameter("hierarchyId", hierarchyId)
                    .getSingleResult();
        } catch (NoResultException e) {
            log.warn("No balance record found for Hierarchy ID: {} in table: {}", hierarchyId, amountTable);
            return BigDecimal.ZERO; // Default balance if no record exists
        }
    }

    private <T extends AbstractWalletAmountTable> void updateWallet(TransactionSchemeDetailsRequestDto dto,
                                                                    T wallet,
                                                                    BigDecimal requestedAmount,
                                                                    String tranxType,
                                                                    String baseTransactionId,
                                                                    String tranStatus,
                                                                    String service) {

        log.info("=== [updateWallet] START ===");
        log.info("Input Params | TranxType: {} | Status: {} | BaseTxnId: {} | Service: {} | WalletType: {} | RequestedAmount: {}",
                tranxType, tranStatus, baseTransactionId, service, dto.getWalletType(), requestedAmount);

        log.debug("Wallet Before Update | HierarchyID: {} | AvailableBalance: {} | TotalCash: {}",
                dto.getHierarchyId(), wallet.getAvailableBalance(), wallet.getTotalCash());

        String actionOnBalance;

        // Handle Pending Transaction
        if ("PENDING".equalsIgnoreCase(tranStatus)) {
            log.info("Transaction marked as PENDING — delegating to handlePendingTransaction()");
            handlePendingTransaction(wallet, tranxType, requestedAmount, baseTransactionId, service, dto);
            log.info("=== [updateWallet] END (PENDING Transaction) ===");
            return;
        }

        // *** Credit or Debit Operation ***
        BigDecimal balanceBefore = wallet.getAvailableBalance();
        log.debug("Initial Balance Before Operation: {}", balanceBefore);

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(requestedAmount));
            actionOnBalance = "Add Funds - " + dto.getWalletType().toUpperCase();
            log.info("Wallet CREDIT operation: Added {} | New AvailableBalance: {}", requestedAmount, wallet.getAvailableBalance());
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(requestedAmount));
            actionOnBalance = "Deduct Funds - " + dto.getWalletType().toUpperCase();
            log.info("Wallet DEBIT operation: Deducted {} | New AvailableBalance: {}", requestedAmount, wallet.getAvailableBalance());
        }

        // Update metadata
        wallet.setLastUpdatedAmount(requestedAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        log.debug("Wallet Metadata Updated | LastUpdatedAmount: {} | LastUpdatedDate: {} | LastUpdatedTime: {}",
                wallet.getLastUpdatedAmount(), wallet.getLastUpdatedDate(), wallet.getLastUpdatedTime());

        // Insert main transaction
        log.info("Inserting transaction record for {} | Action: {} | BalanceBefore: {} | BalanceAfter: {}",
                tranxType.toUpperCase(), actionOnBalance, balanceBefore, wallet.getAvailableBalance());

        insertTransaction(wallet, tranStatus, requestedAmount, balanceBefore,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service, dto);

        // Begin Hierarchy Traversal for Mirroring
        String currentId = dto.getHierarchyId();
        boolean mirrorHandled = false;

        log.info("Starting hierarchy traversal for mirroring | Current Hierarchy ID: {}", currentId);

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                log.info("No further parent found. Stopping hierarchy traversal.");
                break;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);
            log.debug("Found Parent: {} | Type: {}", parentId, parentType);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    log.info("Mirroring to WhiteLabel (isAdmin=true) | ParentID: {}", parentId);
                    mirrorTransactionToHierarchy(parentId, dto.getHierarchyId(), tranxType.toUpperCase(), requestedAmount,
                            dto.getWalletType().toUpperCase(), "TXN" + baseTransactionId, "WhiteLabel Admin", tranStatus, dto);
                    mirrorHandled = true;
                    break;
                } else {
                    log.debug("WhiteLabel found but not Admin. Continuing traversal...");
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                log.info("Reached Admin hierarchy level (Type: {}).", parentType);
                log.info("Mirroring transaction to Admin fallback | AdminID: {}", parentId);
                mirrorTransactionToHierarchy(parentId, dto.getHierarchyId(), tranxType.toUpperCase(), requestedAmount,
                        dto.getWalletType().toUpperCase(), "TXN" + baseTransactionId, "Admin", tranStatus, dto);
                break;
            }

            currentId = parentId;
        }

        log.info("=== [updateWallet] END SUCCESS === | Final AvailableBalance: {} | TotalCash: {}",
                wallet.getAvailableBalance(), wallet.getTotalCash());
    }


    private <T extends AbstractWalletAmountTable> void handlePendingTransaction(T wallet,
                                                                                String tranxType,
                                                                                BigDecimal requestedAmount,
                                                                                String baseTransactionId,
                                                                                String service, TransactionSchemeDetailsRequestDto request) {
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if ("debit".equalsIgnoreCase(tranxType)) {
            wallet.setTotalCash(wallet.getTotalCash().subtract(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(requestedAmount));
        }

        wallet.setLastUpdatedAmount(requestedAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        insertTransaction(wallet, "PENDING", requestedAmount, balanceBefore,
                wallet.getAvailableBalance(), "Vendor Pending - " + tranxType.toUpperCase(),
                buildTransactionId("TXN", baseTransactionId), service, request);
    }


    public void insertTransaction(AbstractWalletAmountTable wallet,
                                  String tranStatus,
                                  BigDecimal amount,
                                  BigDecimal balanceBeforeTranx,
                                  BigDecimal balanceAfterTranx,
                                  String actionOnBalance,
                                  String tranxId,
                                  String service,
                                  TransactionSchemeDetailsRequestDto dto) {

        log.info("=== [insertTransaction] START ===");
        log.info("Transaction Details | TranxId: {} | Service: {} | Status: {} | Amount: {} | WalletType: {}",
                tranxId, service, tranStatus, amount, wallet.getClass().getSimpleName());

        try {

            Scheme scheme = schemeRepository.findBySchemeCode(dto.getSchemeRate().getSchemeCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Scheme not found with Scheme Code: " + dto.getSchemeRate().getSchemeCode()));

            if (wallet instanceof RetailerPrepaidAmount) {

                // Fast duplicate check
                if (retailerPrepaidTransactionDetailsRepository.existsByTransactionId(tranxId)) {
                    log.warn("Duplicate prepaid transaction skipped (exists check) | TranxId: {}", tranxId);
                    return;
                }

                RetailerPrepaidTransactionDetails transaction = new RetailerPrepaidTransactionDetails();
                transaction.setTransactionId(tranxId);
                transaction.setDateOfTransaction(LocalDate.now());
                transaction.setTimeOfTransaction(LocalTime.now());
                transaction.setService(service);
                transaction.setBalBeforeTran(balanceBeforeTranx);
                transaction.setAmount(amount);
                transaction.setBalAfterTran(balanceAfterTranx);
                transaction.setActionOnBalanceAmount(actionOnBalance);
                transaction.setTranStatus(tranStatus);
                transaction.setFinalBalance(balanceAfterTranx);
                transaction.setRetailerId(((RetailerPrepaidAmount) wallet).getRetailerId().getRetailerId());
                transaction.setOperatorName(dto.getOperatorName());
                transaction.setSubProductName(dto.getSubProductCode());
                transaction.setSchemeName(scheme.getSchemeName());
                transaction.setLatitude(dto.getLatitude());
                transaction.setLongitude(dto.getLongitude());
                transaction.setVendorName(dto.getVendorName());
                transaction.setVendorTransactionId(dto.getVendorTxnId());

                retailerPrepaidTransactionDetailsRepository.save(transaction);
                log.info("Prepaid transaction saved successfully | TranxId: {}", tranxId);

            } else if (wallet instanceof RetailerPostpaidAmount) {

                // Fast duplicate check
                if (retailerPostpaidTransactionDetailsRepository.existsByTransactionId(tranxId)) {
                    log.warn("Duplicate postpaid transaction skipped (exists check) | TranxId: {}", tranxId);
                    return;
                }

                RetailerPostpaidTransactionDetails transaction = new RetailerPostpaidTransactionDetails();
                transaction.setTransactionId(tranxId);
                transaction.setDateOfTransaction(LocalDate.now());
                transaction.setTimeOfTransaction(LocalTime.now());
                transaction.setService(service);
                transaction.setBalBeforeTran(balanceBeforeTranx);
                transaction.setAmount(amount);
                transaction.setBalAfterTran(balanceAfterTranx);
                transaction.setActionOnBalanceAmount(actionOnBalance);
                transaction.setTranStatus(tranStatus);
                transaction.setFinalBalance(balanceAfterTranx);
                transaction.setRetailerId(((RetailerPostpaidAmount) wallet).getRetailerId().getRetailerId());
                transaction.setOperatorName(dto.getOperatorName());
                transaction.setSubProductName(dto.getSubProductCode());
                transaction.setSchemeName(scheme.getSchemeName());
                transaction.setLatitude(dto.getLatitude());
                transaction.setLongitude(dto.getLongitude());
                transaction.setVendorName(dto.getVendorName());
                transaction.setVendorTransactionId(dto.getVendorTxnId());

                retailerPostpaidTransactionDetailsRepository.save(transaction);
                log.info("Postpaid transaction saved successfully | TranxId: {}", tranxId);

            } else {
                log.warn("Unsupported wallet type detected: {}", wallet.getClass().getSimpleName());
            }

        } catch (org.springframework.dao.DataIntegrityViolationException ex) {

            // DB-level duplicate protection (race condition safe)
            log.warn("Duplicate transaction skipped (DB constraint) | TranxId: {}", tranxId);

        } catch (Exception e) {
            log.error("Failed to insert transaction | TranxId: {} | Error: {}", tranxId, e.getMessage(), e);
            throw e;
        }

        log.info("=== [insertTransaction] END ===");
    }


    private void validateAmount(TransactionSchemeDetailsRequestDto request) {
        log.info("=== [validateAmount] START === | Amount: {} | ConfirmAmount: {}", request.getAmount(), request.getConfirmAmount());

        if (!Objects.equals(request.getAmount(), request.getConfirmAmount())) {
            log.error(" Amount mismatch detected! Transaction failed. Amount: {}, ConfirmAmount: {}",
                    request.getAmount(), request.getConfirmAmount());
            throw new RuntimeException("Amount mismatch! Transaction failed.");
        }

        log.info(" Amount validation successful — Amounts match.");
        log.info("=== [validateAmount] END ===");
    }


    private String validateAndGetSchemeCode(TransactionSchemeDetailsRequestDto request, List<SchemeAllocation> allocations) {
        if (allocations == null || allocations.isEmpty()) {
            log.warn("No scheme allocations found for hierarchy: {}", request.getHierarchyId());
            throw new RuntimeException("No scheme allocations found for hierarchy: " + request.getHierarchyId());
        }

        // Match against hierarchyId, subProductCode, and operatorCode
        return allocations.stream()
                .filter(allocation ->
                                allocation.getHierarchyId().equalsIgnoreCase(request.getHierarchyId()) &&
                                        allocation.getSubProductCode().equalsIgnoreCase(request.getSubProductCode()) //&&
//                                allocation.getOperatorCode().equalsIgnoreCase(request.getOperatorCode())
                )
                .findFirst()
                .map(SchemeAllocation::getSchemeCode)
                .orElseThrow(() -> {
                    log.warn("No matching allocation found for hierarchy: {}, subProductCode: {}, productCode: {}, operatorCode: {}",
                            request.getHierarchyId(), request.getSubProductCode(), request.getProductCode(), request.getOperatorCode());

                    return new RuntimeException("No matching scheme allocation found for hierarchyId: " +
                            request.getHierarchyId() + ", subProductCode: " + request.getSubProductCode() +
                            ", productCode: " + request.getProductCode() + ", operatorCode: " + request.getOperatorCode());
                });
    }

    private String getWalletType(TransactionSchemeDetailsRequestDto request) {
        SubProducts subProducts = subProductsRepository.findBySubProductCode(request.getSubProductCode())
                .orElseThrow(() -> {
                    log.error("Sub Product not found for code: {}", request.getProductCode());
                    return new RuntimeException("Sub Product not found for code: " + request.getProductCode());
                });

        log.info("Sub Product found: ID={}, Name={}, WalletType={}", subProducts.getId(), subProducts.getSubProductTitle(), subProducts.getWalletType());
        return subProducts.getWalletType();
    }

    private SchemeRate findMatchingSchemeRate(TransactionSchemeDetailsRequestDto request) {
        List<SchemeRate> schemeRates = schemeRateRepository.findAllByOperatorNameAndOperatorCode(
                request.getOperatorName(), request.getOperatorCode());

        if (schemeRates.isEmpty()) {
            log.error("No Scheme Rates found for Operator: {}, Code: {}", request.getOperatorName(), request.getOperatorCode());
            throw new RuntimeException("No Scheme Rates found for given operator.");
        }

        return schemeRates.stream()
                .filter(rate -> {
                    double amount = request.getAmount();
                    return amount >= rate.getSlabStart() && amount <= rate.getSlabEnd();
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No matching Scheme Rate found for amount: " + request.getAmount()));
    }

    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }


    private BigDecimal calculateCommissionAmount(double commissionValue, BigDecimal transactionAmount, String commissionType) {
        BigDecimal commissionAmount;
        BigDecimal commissionRate = BigDecimal.valueOf(commissionValue);

        if ("PERCENTAGE".equalsIgnoreCase(commissionType)) {
            commissionAmount = transactionAmount.multiply(commissionRate);
        } else {
            // Flat commission amount
            commissionAmount = commissionRate;
        }

        // Ensure commission does not exceed transaction amount
        if (commissionAmount.compareTo(transactionAmount) > 0) {
            commissionAmount = transactionAmount;
        }

        return commissionAmount.setScale(2, RoundingMode.HALF_UP);
    }

    private double getCommissionRateByType(String hierarchyType, SchemeRate schemeRate) {
        return switch (hierarchyType) {
            case "AD" -> schemeRate.getIm();
            case "ET" -> schemeRate.getEnterprise();
            case "WL" -> schemeRate.getWhitelabel();
            case "SD" -> schemeRate.getSuperDistributor();
            case "PT" -> schemeRate.getPartner();
            case "DT" -> schemeRate.getDistributor();
            case "RT" -> schemeRate.getRetailer();
            default -> 0.0;
        };
    }

    @Transactional
    private void mirrorTransactionToHierarchy(String targetHierarchyId, String sourceHierarchyId, String originalTxnType,
                                              BigDecimal amount, String walletType, String txnId, String action, String tranStatus,
                                              TransactionSchemeDetailsRequestDto request) {

        log.info("=== [mirrorTransactionToHierarchy] START ===");
        log.info("Mirroring Transaction | From: {} | To: {} | OriginalTxnType: {} | WalletType: {} | Amount: {} | TxnId: {} | Status: {}",
                sourceHierarchyId, targetHierarchyId, originalTxnType, walletType, amount, txnId, tranStatus);

        String targetType = extractHierarchyType(targetHierarchyId);
        String mirrorTxnType = originalTxnType.equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";

        log.debug("Derived Mirror Transaction Type: {} for TargetType: {}", mirrorTxnType, targetType);

        BigDecimal currentBalance = getCurrentBalance(targetHierarchyId, walletType);
        log.debug("Current Balance for Target [{} - {}]: {}", targetHierarchyId, walletType, currentBalance);

        if ("DEBIT".equalsIgnoreCase(mirrorTxnType) && currentBalance.compareTo(amount) < 0) {
            log.error("{} ({}) has insufficient balance to mirror DEBIT transaction. Required: {}, Available: {}",
                    targetType, targetHierarchyId, amount, currentBalance);
            throw new RuntimeException(targetType + " wallet has insufficient balance to mirror transaction.");
        }

        BigDecimal afterBalance = mirrorTxnType.equalsIgnoreCase("CREDIT")
                ? currentBalance.add(amount)
                : currentBalance.subtract(amount);

        String actionOnBalanceAmount = mirrorTxnType.equalsIgnoreCase("DEBIT")
                ? "Debit Funds " + action + " - " + walletType.toUpperCase()
                : "Add Funds " + action + " - " + walletType.toUpperCase();

        log.info("Mirror Transaction Prepared | TargetType: {} | MirrorTxnType: {} | BeforeBalance: {} | AfterBalance: {} | Action: {}",
                targetType, mirrorTxnType, currentBalance, afterBalance, actionOnBalanceAmount);

        try {
            insertTransactionForAdminHierarchy(
                    getTransactionTable(targetType, walletType),
                    mirrorTxnType,
                    amount,
                    "Mirror Transaction for " + originalTxnType,
                    targetHierarchyId,
                    sourceHierarchyId,
                    walletType,
                    currentBalance,
                    afterBalance,
                    actionOnBalanceAmount,
                    txnId, tranStatus, request, targetType
            );
            log.info("Mirror transaction inserted successfully for target [{} - {}]", targetHierarchyId, targetType);
        } catch (Exception e) {
            log.error("Failed to insert mirror transaction for [{} - {}]: {}", targetHierarchyId, targetType, e.getMessage(), e);
            throw e;
        }

        try {
            updateAmountTable(targetType, targetHierarchyId, getAmountTable(targetType, walletType), currentBalance.subtract(afterBalance));
            log.info("Updated amount table successfully for target [{} - {}]", targetHierarchyId, targetType);
        } catch (Exception e) {
            log.error("Failed to update amount table for [{} - {}]: {}", targetHierarchyId, targetType, e.getMessage(), e);
            throw e;
        }

        log.info("=== [mirrorTransactionToHierarchy] END ===");
    }


    private void mirrorTransactionToAdminOrWhitelabel(String hierarchyId, String txnType, BigDecimal amount, String walletType,
                                                      String txnId, String action, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("=== [mirrorTransactionToAdminOrWhitelabel] START ===");
        log.info("Starting mirror chain from HierarchyID: {} | TxnType: {} | Amount: {} | WalletType: {} | TxnId: {}",
                hierarchyId, txnType, amount, walletType, txnId);

        String currentId = hierarchyId;
        boolean mirrorDone = false;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("No parent found for hierarchy ID: {} — stopping mirror chain", currentId);
                break;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            log.info("Found Parent | Current: {} | Parent: {} | ParentType: {}", currentId, parentId, parentType);

            if ("WL".equalsIgnoreCase(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    log.info("Detected WhiteLabel Admin [{}]. Proceeding with mirror transaction.", parentId);
                    mirrorTransactionToHierarchy(parentId, hierarchyId, txnType, amount, walletType, txnId, action, tranStatus, request);
                    mirrorDone = true;
                    break;
                } else {
                    log.debug("Parent [{}] is WhiteLabel but not Admin — continuing search.", parentId);
                }
            }

            if ("IM".equalsIgnoreCase(parentType) || "AD".equalsIgnoreCase(parentType)) {
                log.info("Detected Admin Hierarchy [{} - {}]. Proceeding with mirror transaction.", parentId, parentType);
                mirrorTransactionToHierarchy(parentId, hierarchyId, txnType, amount, walletType, txnId, action, tranStatus, request);
                mirrorDone = true;
                break;
            }

            currentId = parentId;
        }

        if (!mirrorDone) {
            log.warn("Mirror transaction not executed for hierarchy [{}]. No eligible parent found.", hierarchyId);
        }

        log.info("=== [mirrorTransactionToAdminOrWhitelabel] END ===");
    }

    private boolean isAdminType(String hierarchyType) {
        return "IM".equalsIgnoreCase(hierarchyType) || "AD".equalsIgnoreCase(hierarchyType);
    }

    private void creditGstToAdminOrWhitelabel(String sourceHierarchyId, String walletType, BigDecimal gstAmount,
                                              String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("[GST_FLOW] Initiated | SourceID={} | Wallet={} | GST={} | BaseTxnId={} | Status={}",
                sourceHierarchyId, walletType, gstAmount, baseTransactionId, tranStatus);

        String currentId = sourceHierarchyId;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("[GST_FLOW] No eligible parent found for {} → GST credit terminated.", currentId);
                return;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            log.info("[GST_FLOW] Checking parent | ID={} | Type={} | From={}", parentId, parentType, currentId);

            // Case 1: Whitelabel (check admin flag)
            if ("WL".equalsIgnoreCase(parentType)) {
                boolean isAdminWL = whiteLabelsRepository.findById(parentId)
                        .map(WhiteLabels::getIsAdmin)
                        .orElse(false);

                if (isAdminWL) {
                    log.info("[GST_CREDIT] Eligible WL Admin found | WL_ID={} | Crediting GST...", parentId);
                    creditGstToAdmin(parentId, sourceHierarchyId, walletType, gstAmount, baseTransactionId, tranStatus, request);
                    log.info("[GST_CREDIT_SUCCESS] GST successfully credited to WL Admin | WL_ID={}", parentId);
                    return;
                }

                log.debug("[GST_FLOW] WL {} is not Admin. Moving up hierarchy...", parentId);
            }

            // Case 2: IM or AD → Direct admin credit
            if (isAdminType(parentType)) {
                log.info("[GST_CREDIT] Admin hierarchy found | Type={} | ID={} | Crediting GST...", parentType, parentId);
                creditGstToAdmin(parentId, sourceHierarchyId, walletType, gstAmount, baseTransactionId, tranStatus, request);
                log.info("[GST_CREDIT_SUCCESS] GST credited successfully to {} | ID={}", parentType, parentId);
                return;
            }

            // Continue searching upward
            log.debug("[GST_FLOW] Parent {} (Type={}) not eligible → Traversing upward...", parentId, parentType);
            currentId = parentId;
        }
    }


//    @Override
//    @Transactional
//    public CommonResponse<BulkTransactionStatusResponse> updatePendingTransactionStatus(UpdateTransactionStatusRequest request) {
//
//        StringBuilder result = new StringBuilder();
//
//        if (request == null || request.getTransactionIds() == null || request.getTransactionIds().isEmpty()) {
//            throw new IllegalArgumentException("TransactionIds cannot be null or empty");
//        }
//
//        BulkTransactionStatusResponse statusResponse = new BulkTransactionStatusResponse();
//
//        List<String> successList = new ArrayList<>();
//        List<String> skippedList = new ArrayList<>();
//        List<String> failedList = new ArrayList<>();
//        List<String> messages = new ArrayList<>();
//
//        final String walletType = request.getWalletType() != null
//                ? request.getWalletType().toUpperCase()
//                : null;
//
//        for (String tranxId : request.getTransactionIds()) {
//
//            try {
//                // Normalize txnId
//                String baseTxnId = tranxId.replaceFirst("^TXN", "");
//                String fullTxnId = "TXN" + baseTxnId;
//
//                // Fetch Transaction (Postpaid → else Prepaid)
//                AbstractRetailerTransactionDetails txn =
//                        retailerPostpaidTransactionDetailsRepository.findByTransactionId(fullTxnId)
//                                .map(t -> (AbstractRetailerTransactionDetails) t)
//                                .orElseGet(() ->
//                                        retailerPrepaidTransactionDetailsRepository.findByTransactionId(fullTxnId)
//                                                .orElseThrow(() ->
//                                                        new RuntimeException("Transaction not found: " + fullTxnId)));
//
//                // SKIP if already SUCCESS
//                if ("SUCCESS".equalsIgnoreCase(txn.getTranStatus())) {
//                    skippedList.add(fullTxnId);
//                    messages.add(fullTxnId + " already SUCCESS");
//                    continue;
//                }
//
//                // Fetch SubProduct
//                SubProducts subProduct = subProductsRepository
//                        .findBySubProductTitle(txn.getSubProductName())
//                        .orElseThrow(() ->
//                                new RuntimeException("SubProduct not found: " + txn.getSubProductName()));
//
//                // Fetch Scheme Allocation
//                SchemeAllocation schemeAllocation = schemeAllocationRepository
//                        .findByHierarchyIdAndSubProductCode(
//                                txn.getRetailerId(),
//                                subProduct.getSubProductCode()
//                        )
//                        .orElseThrow(() -> new RuntimeException(
//                                "SchemeAllocation not found | hierarchyId="
//                                        + txn.getRetailerId()
//                                        + ", subProductCode="
//                                        + subProduct.getSubProductCode()
//                        ));
//
//                String schemeCode = schemeAllocation.getSchemeCode();
//
//                // Prepare Amount
//                double txnAmt = txn.getAmount().doubleValue();
//
//                // Fetch Matching Schemes
//                List<SchemeRate> matchingSchemes =
//                        schemeRateRepository.findBySchemeCodeAndOperatorName(
//                                schemeCode,
//                                txn.getOperatorName()
//                        );
//
//                if (matchingSchemes == null || matchingSchemes.isEmpty()) {
//                    skippedList.add(fullTxnId);
//                    messages.add(fullTxnId + " no scheme found");
//                    continue;
//                }
//
//                // Select Scheme
//                SchemeRate selectedScheme = matchingSchemes.stream()
//                        .filter(s -> s.getSlabType() == SlabType.VARIABLE
//                                && txnAmt >= s.getSlabStart()
//                                && txnAmt <= s.getSlabEnd())
//                        .findFirst()
//                        .orElse(matchingSchemes.get(0));
//
//                // Fetch Scheme → Product → SubProduct
//                Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
//                        .orElseThrow(() -> new RuntimeException("Scheme not found: " + selectedScheme.getSchemeCode()));
//
//                // SKIP if scheme inactive
//                if (scheme.getIsActive() == null || !scheme.getIsActive()) {
//                    skippedList.add(fullTxnId);
//                    messages.add(fullTxnId + " scheme inactive");
//                    continue;
//                }
//
//                String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");
//                short productId = Short.parseShort(hashParts[0]);
//                String subProductId = hashParts[1];
//
//                Products product = productsRepository.findById(productId)
//                        .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
//
//                SubProducts mappedSubProduct = subProductsRepository.findById(subProductId)
//                        .orElseThrow(() -> new RuntimeException("SubProduct not found: " + subProductId));
//
//                // Build DTO
//                TransactionSchemeDetailsRequestDto dto = new TransactionSchemeDetailsRequestDto();
//
//                dto.setBaseTranxId(baseTxnId);
//                dto.setAmount(txnAmt);
//                dto.setConfirmAmount(txnAmt);
//                dto.setOperatorName(txn.getOperatorName());
//                dto.setOperatorCode(selectedScheme.getOperatorCode());
//                dto.setSchemeRate(selectedScheme);
//                dto.setTransactionType(product.getTransactionType());
//                dto.setWalletType(walletType != null ? walletType : mappedSubProduct.getWalletType());
//
//                dto.setGstValue(String.valueOf(product.getSgst()));
//                dto.setTdsRate(String.valueOf(product.getTds()));
//                dto.setHierarchyId(txn.getRetailerId());
//                dto.setProductCode(product.getProductCode());
//                dto.setSubProductCode(mappedSubProduct.getSubProductCode());
//
//                dto.setIsCommission(product.getIsCommissions());
//                dto.setHasCharge(product.getHasCharges());
//
//                // Optional fields (set safely if available)
//                dto.setVendorName("Manually Update");
//                dto.setVendorTxnId(fullTxnId);
//
//                log.info("DTO prepared for txnId {}: {}", fullTxnId, dto);
//
//                AbstractWalletAmountTable wallet = getWalletByType(walletType, txn.getRetailerId());
//
//                processChargesAndCommission(
//                        dto,
//                        wallet,
//                        BigDecimal.valueOf(selectedScheme.getGross()),
//                        product.getSgst().toString(),
//                        baseTxnId,
//                        request.getNewStatus()
//                );
//
//                successList.add(fullTxnId);
//
//                updatePendingTransactionTables(baseTxnId, request.getNewStatus());
//
//                result.append("SUCCESS: ").append(fullTxnId).append(" processed\n");
//
//            } catch (Exception ex) {
//                log.error("Error processing txnId {}: {}", tranxId, ex.getMessage(), ex);
//
//                result.append("FAILED: ")
//                        .append(tranxId)
//                        .append(" -> ")
//                        .append(ex.getMessage())
//                        .append("\n");
//            }
//        }
//
//        statusResponse.setTotal(request.getTransactionIds().size());
//        statusResponse.setSuccessCount(successList.size());
//        statusResponse.setSkippedCount(skippedList.size());
//        statusResponse.setFailedCount(0);
//
//        statusResponse.setSuccessTxnIds(successList);
//        statusResponse.setSkippedTxnIds(skippedList);
//        statusResponse.setFailedTxnIds(failedList);
//        statusResponse.setMessages(messages);
//
//        return new CommonResponse<>(200, "Bulk transaction processing completed", statusResponse);
//    }


    @Override
    @Transactional
    public CommonResponse<BulkTransactionStatusResponse> updatePendingTransactionStatus(
            UpdateTransactionStatusRequest request
    ) {

        if (request == null
                || request.getTransactionIds() == null
                || request.getTransactionIds().isEmpty()) {

            throw new IllegalArgumentException("TransactionIds cannot be null or empty");
        }

        BulkTransactionStatusResponse statusResponse =
                new BulkTransactionStatusResponse();

        List<String> successList = new ArrayList<>();
        List<String> skippedList = new ArrayList<>();
        List<String> failedList = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        for (String tranxId : request.getTransactionIds()) {

            try {

                String baseTxnId = tranxId.replaceFirst("^TXN", "");
                String fullTxnId = "TXN" + baseTxnId;

                // ================= FETCH TXN =================
                Pair<AbstractRetailerTransactionDetails, String> result =
                        fetchTxnWithWalletType(fullTxnId);

                AbstractRetailerTransactionDetails txn = result.getLeft();
                String walletType = result.getRight();

                // ================= SKIP =================
                if ("SUCCESS".equalsIgnoreCase(txn.getTranStatus())
                        || "FAILED".equalsIgnoreCase(txn.getTranStatus())) {

                    skippedList.add(fullTxnId);
                    messages.add(fullTxnId + " already processed");
                    continue;
                }

                boolean isCredit =
                        "CREDIT".equalsIgnoreCase(txn.getService());

                boolean isDebit =
                        "DEBIT".equalsIgnoreCase(txn.getService());

                String hierarchyType =
                        extractHierarchyType(txn.getRetailerId());

                // =========================================================
                // ================= CREDIT FLOW ===========================
                // =========================================================
                if (isCredit) {

                    // ================= CREDIT SUCCESS =================
                    if ("SUCCESS".equalsIgnoreCase(request.getNewStatus())) {

                        BigDecimal currentBalance =
                                getCurrentBalance(
                                        txn.getRetailerId(),
                                        walletType
                                );

                        BigDecimal txnAmount = txn.getAmount();

                        BigDecimal balBefore = currentBalance;
                        BigDecimal balAfter = currentBalance.add(txnAmount);

                        // Update TXN
                        updateTransactionStatusAndBalance(
                                hierarchyType,
                                txn.getRetailerId(),
                                walletType,
                                fullTxnId,
                                "SUCCESS",
                                balBefore,
                                balAfter
                        );

                        // Fetch Wallet
                        AbstractWalletAmountTable wallet =
                                getWalletByType(
                                        walletType,
                                        txn.getRetailerId()
                                );

                        // Wallet Credit
                        wallet.setTotalCash(
                                wallet.getTotalCash().add(txnAmount)
                        );

                        wallet.setAvailableBalance(
                                wallet.getAvailableBalance().add(txnAmount)
                        );

                        wallet.setLastUpdatedAmount(txnAmount);
                        wallet.setLastUpdatedDate(LocalDate.now());
                        wallet.setLastUpdatedTime(LocalTime.now());

                        saveWallet(walletType, wallet);

                        // Prepare Scheme Context
                        SchemeProcessingContext context =
                                prepareSchemeContext(
                                        txn,
                                        walletType,
                                        baseTxnId,
                                        fullTxnId,
                                        skippedList,
                                        messages
                                );

                        if (context == null) {
                            continue;
                        }

                        TransactionSchemeDetailsRequestDto dto =
                                context.getDto();

                        SchemeRate selectedScheme =
                                context.getSchemeRate();

                        Products product =
                                context.getProduct();

                        log.info(
                                "DTO prepared for txnId {}: {}",
                                fullTxnId,
                                dto
                        );

                        processChargesAndCommission(
                                dto,
                                wallet,
                                BigDecimal.valueOf(
                                        selectedScheme.getGross()
                                ),
                                product.getSgst().toString(),
                                baseTxnId,
                                request.getNewStatus()
                        );
                    }

                    // ================= CREDIT FAILED =================
                    else {

                        handleFailedTransaction(
                                hierarchyType,
                                walletType,
                                fullTxnId,
                                "FAILED"
                        );
                    }

                    successList.add(fullTxnId);
                }

                // =========================================================
                // ================= DEBIT FLOW ============================
                // =========================================================
                else if (isDebit) {

                    // ================= DEBIT SUCCESS =================
                    if ("SUCCESS".equalsIgnoreCase(request.getNewStatus())) {

                        // Update Main TXN
                        updateTransactionStatusOnly(
                                hierarchyType,
                                txn.getRetailerId(),
                                walletType,
                                fullTxnId,
                                "SUCCESS"
                        );

                        List<AbstractRetailerTransactionDetails> relatedTxns =
                                getRelatedTransactions(
                                        baseTxnId,
                                        walletType
                                );

                        boolean hasChargeOrGst =
                                relatedTxns.stream().anyMatch(t ->
                                        t.getTransactionId().startsWith("CHG")
                                                || t.getTransactionId().startsWith("GST")
                                );

                        // Update CHG/GST if exists
                        if (hasChargeOrGst) {

                            for (AbstractRetailerTransactionDetails t : relatedTxns) {

                                if (t.getTransactionId().startsWith("CHG")
                                        || t.getTransactionId().startsWith("GST")) {

                                    updateTransactionStatusOnly(
                                            hierarchyType,
                                            txn.getRetailerId(),
                                            walletType,
                                            t.getTransactionId(),
                                            "SUCCESS"
                                    );
                                }
                            }
                        }

                        // Prepare Scheme Context
                        SchemeProcessingContext context =
                                prepareSchemeContext(
                                        txn,
                                        walletType,
                                        baseTxnId,
                                        fullTxnId,
                                        skippedList,
                                        messages
                                );

                        if (context == null) {
                            continue;
                        }

                        TransactionSchemeDetailsRequestDto dto =
                                context.getDto();

                        SchemeRate selectedScheme =
                                context.getSchemeRate();

                        Products product =
                                context.getProduct();

                        log.info(
                                "DTO prepared for txnId {}: {}",
                                fullTxnId,
                                dto
                        );

                        // If CHG/GST already exists
                        if (hasChargeOrGst) {

                            processCommission(
                                    dto,
                                    baseTxnId,
                                    request.getNewStatus(),
                                    dto
                            );
                        }

                        // Insert All
                        else {

                            AbstractWalletAmountTable wallet =
                                    getWalletByType(
                                            walletType,
                                            txn.getRetailerId()
                                    );

                            processChargesAndCommission(
                                    dto,
                                    wallet,
                                    BigDecimal.valueOf(
                                            selectedScheme.getGross()
                                    ),
                                    product.getSgst().toString(),
                                    baseTxnId,
                                    request.getNewStatus()
                            );
                        }
                    }

                    // ================= DEBIT FAILED =================
                    else {

                        List<AbstractRetailerTransactionDetails> relatedTxns =
                                getRelatedTransactions(
                                        baseTxnId,
                                        walletType
                                );

                        // Handle All Related Transactions
                        for (AbstractRetailerTransactionDetails relatedTxn : relatedTxns) {

                            handleFailedTransaction(
                                    hierarchyType,
                                    walletType,
                                    relatedTxn.getTransactionId(),
                                    "FAILED"
                            );
                        }

                        log.info(
                                "Refund completed for baseTxnId={}",
                                baseTxnId
                        );
                    }

                    successList.add(fullTxnId);
                }

                // =========================================================
                // UPDATE PENDING TABLE
                // =========================================================
                updatePendingTransactionTables(
                        baseTxnId,
                        request.getNewStatus()
                );

            } catch (Exception ex) {

                log.error(
                        "Error processing txnId {}: {}",
                        tranxId,
                        ex.getMessage(),
                        ex
                );

                failedList.add(tranxId);

                messages.add(
                        tranxId + " -> " + ex.getMessage()
                );
            }
        }

        // =========================================================
        // FINAL RESPONSE
        // =========================================================

        statusResponse.setTotal(
                request.getTransactionIds().size()
        );

        statusResponse.setSuccessCount(
                successList.size()
        );

        statusResponse.setSkippedCount(
                skippedList.size()
        );

        statusResponse.setFailedCount(
                failedList.size()
        );

        statusResponse.setSuccessTxnIds(successList);

        statusResponse.setSkippedTxnIds(skippedList);

        statusResponse.setFailedTxnIds(failedList);

        statusResponse.setMessages(messages);

        return new CommonResponse<>(
                200,
                "Bulk transaction processing completed",
                statusResponse
        );
    }

    private SchemeProcessingContext prepareSchemeContext(
            AbstractRetailerTransactionDetails txn,
            String walletType,
            String baseTxnId,
            String fullTxnId,
            List<String> skippedList,
            List<String> messages
    ) {

        // Fetch SubProduct
        SubProducts subProduct = subProductsRepository
                .findBySubProductTitle(txn.getSubProductName())
                .orElseThrow(() ->
                        new RuntimeException("SubProduct not found: " + txn.getSubProductName()));

        // Fetch Scheme Allocation
        SchemeAllocation schemeAllocation = schemeAllocationRepository
                .findByHierarchyIdAndSubProductCode(
                        txn.getRetailerId(),
                        subProduct.getSubProductCode()
                )
                .orElseThrow(() -> new RuntimeException(
                        "SchemeAllocation not found | hierarchyId="
                                + txn.getRetailerId()
                                + ", subProductCode="
                                + subProduct.getSubProductCode()
                ));

        String schemeCode = schemeAllocation.getSchemeCode();

        // Prepare Amount
        double txnAmt = txn.getAmount().doubleValue();

        // Fetch Matching Schemes
        List<SchemeRate> matchingSchemes =
                schemeRateRepository.findBySchemeCodeAndOperatorName(
                        schemeCode,
                        txn.getOperatorName()
                );

        if (matchingSchemes == null || matchingSchemes.isEmpty()) {
            skippedList.add(fullTxnId);
            messages.add(fullTxnId + " no scheme found");
            return null;
        }

        // Select Scheme
        SchemeRate selectedScheme = matchingSchemes.stream()
                .filter(s -> s.getSlabType() == SlabType.VARIABLE
                        && txnAmt >= s.getSlabStart()
                        && txnAmt <= s.getSlabEnd())
                .findFirst()
                .orElse(matchingSchemes.get(0));

        // Fetch Scheme
        Scheme scheme = schemeRepository.findBySchemeCode(selectedScheme.getSchemeCode())
                .orElseThrow(() ->
                        new RuntimeException("Scheme not found: " + selectedScheme.getSchemeCode()));

        if (scheme.getIsActive() == null || !scheme.getIsActive()) {
            skippedList.add(fullTxnId);
            messages.add(fullTxnId + " scheme inactive");
            return null;
        }

        String[] hashParts = scheme.getSchemeCombinationHashCode().split("-");

        short productId = Short.parseShort(hashParts[0]);
        String subProductId = hashParts[1];

        Products product = productsRepository.findById(productId)
                .orElseThrow(() ->
                        new RuntimeException("Product not found: " + productId));

        SubProducts mappedSubProduct = subProductsRepository.findById(subProductId)
                .orElseThrow(() ->
                        new RuntimeException("SubProduct not found: " + subProductId));

        // Build DTO
        TransactionSchemeDetailsRequestDto dto =
                buildTransactionSchemeDto(
                        txn,
                        walletType,
                        baseTxnId,
                        fullTxnId,
                        selectedScheme,
                        product,
                        mappedSubProduct
                );

        return new SchemeProcessingContext(
                dto,
                selectedScheme,
                product
        );
    }

    private TransactionSchemeDetailsRequestDto buildTransactionSchemeDto(
            AbstractRetailerTransactionDetails txn,
            String walletType,
            String baseTxnId,
            String fullTxnId,
            SchemeRate selectedScheme,
            Products product,
            SubProducts mappedSubProduct
    ) {

        TransactionSchemeDetailsRequestDto dto =
                new TransactionSchemeDetailsRequestDto();

        double txnAmt = txn.getAmount().doubleValue();

        dto.setBaseTranxId(baseTxnId);
        dto.setAmount(txnAmt);
        dto.setConfirmAmount(txnAmt);

        dto.setOperatorName(txn.getOperatorName());
        dto.setOperatorCode(selectedScheme.getOperatorCode());

        dto.setSchemeRate(selectedScheme);

        dto.setTransactionType(product.getTransactionType());

        dto.setWalletType(walletType);

        dto.setGstValue(String.valueOf(product.getSgst()));
        dto.setTdsRate(String.valueOf(product.getTds()));

        dto.setHierarchyId(txn.getRetailerId());

        dto.setProductCode(product.getProductCode());
        dto.setSubProductCode(mappedSubProduct.getSubProductCode());

        dto.setIsCommission(product.getIsCommissions());
        dto.setHasCharge(product.getHasCharges());

        dto.setVendorName("Manually Update");
        dto.setVendorTxnId(fullTxnId);

        return dto;
    }

    private Pair<AbstractRetailerTransactionDetails, String> fetchTxnWithWalletType(String txnId) {

        return retailerPostpaidTransactionDetailsRepository.findByTransactionId(txnId)
                .map(t -> Pair.<AbstractRetailerTransactionDetails, String>of(t, "POSTPAID"))
                .orElseGet(() ->
                        retailerPrepaidTransactionDetailsRepository.findByTransactionId(txnId)
                                .map(t -> Pair.<AbstractRetailerTransactionDetails, String>of(t, "PREPAID"))
                                .orElseThrow(() ->
                                        new RuntimeException("Transaction not found: " + txnId))
                );
    }

    private List<AbstractRetailerTransactionDetails> getRelatedTransactions(String baseTxnId, String walletType) {

        if ("PREPAID".equalsIgnoreCase(walletType)) {     

            return retailerPrepaidTransactionDetailsRepository
                    .findByBaseTransactionId(baseTxnId)
                    .stream()
                    .map(t -> (AbstractRetailerTransactionDetails) t)
                    .toList();

        } else {

            return retailerPostpaidTransactionDetailsRepository
                    .findByBaseTransactionId(baseTxnId)
                    .stream()
                    .map(t -> (AbstractRetailerTransactionDetails) t)
                    .toList();
        }
    }

    private void updateTransactionStatusAndBalance(String hierarchyType, String id, String walletType,
                                                   String txnId, String status,
                                                   BigDecimal balBefore, BigDecimal balAfter) {

        String table = getTransactionTable(hierarchyType, walletType);

        int updated = jdbcTemplate.update(
                "UPDATE " + table + " SET tran_status=?, bal_before_tran=?, bal_after_tran=?, " +
                        "updated_date_of_transaction=NOW(), updated_time_of_transaction=NOW() " +
                        "WHERE transaction_id=?",
                status, balBefore, balAfter, txnId
        );

        if (updated == 0) {
            log.warn("No record found for txnId: {}", txnId);
        }
    }

    private void handleFailedTransaction(
            String hierarchyType,
            String walletType,
            String transactionId,
            String status
    ) {

        // =========================================
        // FETCH TXN
        // =========================================
        Pair<AbstractRetailerTransactionDetails, String> pair =
                fetchTxnWithWalletType(transactionId);

        AbstractRetailerTransactionDetails txn = pair.getLeft();

        if (txn == null) {
            throw new RuntimeException("Transaction not found: " + transactionId);
        }

        // =========================================
        // FETCH WALLET
        // =========================================
        AbstractWalletAmountTable wallet =
                getWalletByType(walletType, txn.getRetailerId());

        if (wallet == null) {
            throw new RuntimeException(
                    "Wallet not found | walletType="
                            + walletType
                            + ", retailerId="
                            + txn.getRetailerId()
            );
        }

        // =========================================
        // BALANCE CALCULATION
        // =========================================
        BigDecimal currentBalance = wallet.getAvailableBalance();
        BigDecimal refundAmount = txn.getAmount();

        BigDecimal balAfterTran = currentBalance.add(refundAmount);

        // =========================================
        // UPDATE TXN TABLE
        // =========================================
        String tableName = getTransactionTable(hierarchyType, walletType);

        String sql = """
            UPDATE %s
            SET
                tran_status = ?,
                updated_date = ?,
                updated_time = ?,
                original_transaction_id = ?,
                bal_before_tran = ?,
                bal_after_tran = ?,
                final_balance = ?
            WHERE transaction_id = ?
            """.formatted(tableName);

        jdbcTemplate.update(
                sql,
                status,
                LocalDate.now(),
                LocalTime.now(),
                "REV" + transactionId,
                currentBalance,
                balAfterTran,
                balAfterTran,
                transactionId
        );

        // =========================================
        // REFUND WALLET
        // =========================================
        wallet.setAvailableBalance(
                wallet.getAvailableBalance().add(refundAmount)
        );

        wallet.setTotalCash(
                wallet.getTotalCash().add(refundAmount)
        );

        wallet.setLastUpdatedAmount(refundAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // =========================================
        // SAVE WALLET
        // =========================================
        saveWallet(walletType, wallet);

        log.info(
                "FAILED txn handled successfully | txnId={} | refund={} | before={} | after={}",
                transactionId,
                refundAmount,
                currentBalance,
                balAfterTran
        );
    }

    private void updateTransactionStatusOnly(String hierarchyType, String id, String walletType,
                                             String txnId, String status) {

        String table = getTransactionTable(hierarchyType, walletType);

        int updated = jdbcTemplate.update(
                "UPDATE " + table + " SET tran_status=?, " +
                        "updated_date_of_transaction=NOW(), updated_time_of_transaction=NOW() " +
                        "WHERE transaction_id=?",
                status, txnId
        );

        if (updated == 0) {
            log.warn("No record found for txnId: {}", txnId);
        }
    }


    private void updatePendingTransactionTables(String baseTxnId, String status) {

        String orderId = "ORD" + baseTxnId;
        String txnId = "TXN" + baseTxnId;

        // PG Transaction Mapping (PRIORITY 1)
        Optional<PgTransactionMapping> pgOpt = pgTransactionMappingRepository.findByOrderId(orderId);

        if (pgOpt.isPresent()) {
            PgTransactionMapping pg = pgOpt.get();
            pg.setStatus(status);
            pgTransactionMappingRepository.save(pg);

            log.info("Updated PG_TRANSACTION_MAPPING for orderId={} with status={}", orderId, status);
            return;
        }

        // Payout Pending (PRIORITY 2)
        Optional<PayoutPendingTransactions> payoutOpt =
                payoutPendingTransactionRepository.findByTransactionId(txnId);

        if (payoutOpt.isPresent()) {
            PayoutPendingTransactions payout = payoutOpt.get();
            payout.setStatus(status);
            payoutPendingTransactionRepository.save(payout);

            log.info("Updated PAYOUT_PENDING_TRANSACTIONS for txnId={} with status={}", txnId, status);
            return;
        }

        // BBPS Pending (PRIORITY 3)
        Optional<BBPSPendingTransactions> bbpsOpt =
                Optional.ofNullable(bbpsPendingTransactionsRepository.findByTransactionId(txnId));

        if (bbpsOpt.isPresent()) {
            BBPSPendingTransactions bbps = bbpsOpt.get();
            bbps.setStatus(status);
            bbpsPendingTransactionsRepository.save(bbps);

            log.info("Updated BBPS_PENDING_TRANSACTIONS for txnId={} with status={}", txnId, status);
            return;
        }

        log.warn("No pending transaction record found for baseTxnId={} (orderId={}, txnId={})",
                baseTxnId, orderId, txnId);
    }

    @Override
    @Transactional
    public String updateTransactionStatus(UpdateTransactionStatusRequest request) {

        StringBuilder lastResult = new StringBuilder();

        for (String tranxId : request.getTransactionIds()) {
            try {
                AbstractRetailerTransactionDetails txn;
                String walletType = request.getWalletType().toUpperCase();
                String baseTransactionId = tranxId.replaceFirst("^TXN", "");


                String fullTxnId = "TXN" + baseTransactionId;

                if ("PREPAID".equals(walletType)) {
                    RetailerPrepaidTransactionDetails prepaidTxn = retailerPrepaidTransactionDetailsRepository
                            .findByTransactionId(fullTxnId)
                            .orElseThrow(() -> new RuntimeException("Prepaid transaction not found"));

                    //old logic chk SETTLEMENT PENDING with tranStatus field
//                    if (!"SETTLEMENT PENDING".equalsIgnoreCase(prepaidTxn.getTranStatus())) {
//                        lastResult.append("SKIPPED: ").append(fullTxnId).append(" already processed with status: ").append(prepaidTxn.getTranStatus()).append("\n");
//                        continue;
//                    }

                    //new logic chk SETTLEMENT PENDING with actionOnBalanceAmount field
                    String action = prepaidTxn.getActionOnBalanceAmount();
                    if (action == null ||
                            !action.trim().toLowerCase().startsWith("settlement pending")) {

                        lastResult.append("SKIPPED: ")
                                .append(fullTxnId)
                                .append(" already processed with action: ")
                                .append(action)
                                .append("\n");
                        continue;
                    }
                    txn = prepaidTxn;

                } else if ("POSTPAID".equals(walletType)) {
                    RetailerPostpaidTransactionDetails postpaidTxn = retailerPostpaidTransactionDetailsRepository
                            .findByTransactionId(fullTxnId)
                            .orElseThrow(() -> new RuntimeException("Postpaid transaction not found"));

                    //old logic chk SETTLEMENT PENDING with tranStatus field
//                    if (!"SETTLEMENT PENDING".equalsIgnoreCase(postpaidTxn.getTranStatus())) {
//                        lastResult.append("SKIPPED: ").append(fullTxnId).append(" already processed with status: ").append(postpaidTxn.getTranStatus()).append("\n");
//                        continue;
//                    }

                    //new logic chk SETTLEMENT PENDING with actionOnBalanceAmount field
                    String action = postpaidTxn.getActionOnBalanceAmount();

                    if (action == null ||
                            !action.trim().toLowerCase().startsWith("settlement pending")) {

                        lastResult.append("SKIPPED: ")
                                .append(fullTxnId)
                                .append(" already processed with action: ")
                                .append(action)
                                .append("\n");
                        continue;
                    }

                    txn = postpaidTxn;

                } else {
                    lastResult.append("FAILED: Invalid wallet type specified for txnId: ").append(fullTxnId).append("\n");
                    continue;
                }

                // Extract basic details
                String retailerId = txn.getRetailerId();
                BigDecimal amount = txn.getAmount();
                String operatorName = txn.getOperatorName();
                String subProductName = txn.getSubProductName();

                TransactionSchemeDetailsRequestDto newRequest = new TransactionSchemeDetailsRequestDto();
                newRequest.setHierarchyId(retailerId);
                newRequest.setOperatorName(operatorName);
                newRequest.setWalletType(walletType);
                newRequest.setAmount(amount.doubleValue());
                newRequest.setConfirmAmount(amount.doubleValue());

                SubProducts subProduct = subProductsRepository.findBySubProductTitle(subProductName)
                        .orElseThrow(() -> new RuntimeException("SubProduct not found: " + subProductName));

                Operators operator = operatorsRepository
                        .findByOperatorNameIgnoreCaseAndSubProductId(operatorName, subProduct.getId())
                        .orElseThrow(() -> new RuntimeException(
                                "Operator not found: " + operatorName + ", subProductId: " + subProduct.getId()
                        ));


                newRequest.setOperatorCode(operator.getOperatorCode());
                newRequest.setProductCode(subProduct.getProduct().getProductCode());
                newRequest.setSubProductCode(subProduct.getSubProductCode());

                TransactionSchemeDetailsResponseDto schemeDetails = makeRequestForTransaction(newRequest);

                newRequest.setSchemeRate(schemeDetails.getMatchedSchemeRate());
                newRequest.setGstValue(schemeDetails.getGstRate());
                newRequest.setTdsRate(schemeDetails.getTdsRate());
                newRequest.setHasCharge(schemeDetails.getHasCharge());
                newRequest.setIsCommission(schemeDetails.getIsCommission());
                newRequest.setTransactionType(schemeDetails.getTransactionType());
                newRequest.setBaseTranxId(baseTransactionId);
                newRequest.setTransactionType(txn.getService());

                AbstractWalletAmountTable wallet = getWalletByType(walletType, retailerId);

                if (("PENDING".equalsIgnoreCase(request.getOldStatus()) && "SUCCESS".equalsIgnoreCase(request.getNewStatus())) ||
                        ("FAILURE".equalsIgnoreCase(request.getOldStatus()) || "SUCCESS".equalsIgnoreCase(request.getNewStatus())) ||
                        ("SETTLEMENT PENDING".equalsIgnoreCase(request.getOldStatus()) && "SUCCESS".equalsIgnoreCase(request.getNewStatus()))) {

                    processTransactionStatusChange(newRequest, request.getNewStatus());
                    lastResult.append("SUCCESS: Transaction ").append(fullTxnId).append(" updated to SUCCESS.\n");

                } else if (("PENDING".equalsIgnoreCase(request.getOldStatus()) && "FAILURE".equalsIgnoreCase(request.getNewStatus())) ||
                        ("SETTLEMENT PENDING".equalsIgnoreCase(request.getOldStatus()) && "FAILURE".equalsIgnoreCase(request.getNewStatus()))) {

                    insertTransaction(wallet, "FAILURE", amount, wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                            "Manual Update to FAILURE", buildTransactionId("FLD", baseTransactionId), "CREDIT", newRequest);

                    lastResult.append("SUCCESS: Transaction ").append(fullTxnId).append(" marked as FAILURE.\n");

                } else if ("SUCCESS".equalsIgnoreCase(request.getOldStatus()) && "FAILURE".equalsIgnoreCase(request.getNewStatus())) {

                    List<String> hierarchyList = List.of("retailer", "distributor", "partner", "super_distributor", "white_label", "enterprises", "admin");

                    for (String hierarchy : hierarchyList) {
                        String txnTable = hierarchy + "_" + walletType + "_transaction_details";
                        String amountTable = hierarchy + "_" + walletType + "_amount";
                        String idColumnName = getIdColumnFromTable(amountTable);

                        List<DynamicTransactionDTO> records = transactionDetailsRepository.findTransactions(txnTable, fullTxnId);

                        if (records != null && !records.isEmpty()) {
                            for (DynamicTransactionDTO tranx : records) {
                                String reverseAction = tranx.getActionOnBalanceAmount().equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";
                                String hierarchyId = tranx.getHierarchyId();

                                BigDecimal currentBalance = wallet.getAvailableBalance();
                                BigDecimal newBalance = reverseAction.equals("CREDIT")
                                        ? currentBalance.add(amount)
                                        : currentBalance.subtract(amount);

                                dynamicWalletRepository.updateWallet(amountTable, idColumnName, hierarchyId, newBalance, amount);

                                DynamicTransactionDTO reversal = new DynamicTransactionDTO();
                                reversal.setTransactionId("FLD" + baseTransactionId);
                                reversal.setAmount(amount);
                                reversal.setBalBeforeTran(currentBalance);
                                reversal.setBalAfterTran(newBalance);
                                reversal.setFinalBalance(newBalance);
                                reversal.setTranStatus("FAILURE");
                                reversal.setActionOnBalanceAmount(reverseAction);
                                reversal.setService(tranx.getService());
                                reversal.setRemarks("Reversal of TXN: " + fullTxnId);
                                reversal.setNarration("Auto-reversal due to FAILED status");
                                reversal.setHierarchyId(tranx.getHierarchyId());
                                reversal.setOperatorName(tranx.getOperatorName());
                                reversal.setMobileNo(tranx.getMobileNo());
                                reversal.setSchemeName(tranx.getSchemeName());
                                reversal.setSubProductName(tranx.getSubProductName());

                                transactionDetailsRepository.insertTransaction(txnTable, reversal);
                            }
                        }
                    }

                    lastResult.append("SUCCESS: Reversal for txnId ").append(fullTxnId).append(" completed.\n");
                }

            } catch (Exception ex) {
                lastResult.append("FAILED: Error processing txnId ").append(tranxId).append(": ").append(ex.getMessage()).append("\n");
                log.error("Error updating transaction {}: {}", tranxId, ex.getMessage(), ex);
            }
        }

        return lastResult.toString();
    }

    public String getIdColumnFromTable(String tableName) {
        if (tableName.contains("_prepaid_")) {
            return tableName.substring(0, tableName.indexOf("_prepaid_")) + "_id";
        } else if (tableName.contains("_postpaid_")) {
            return tableName.substring(0, tableName.indexOf("_postpaid_")) + "_id";
        } else {
            throw new IllegalArgumentException("Invalid wallet table name: " + tableName);
        }
    }

    public TransactionSchemeDetailsResponseDto makeRequestForTransaction(TransactionSchemeDetailsRequestDto request) {
        log.info("Fetching scheme details for hierarchy ID: {}, Operator: {}, Code: {}",
                request.getHierarchyId(), request.getOperatorName(), request.getOperatorCode());

        List<SchemeAllocation> allocations = schemeAllocationRepository.findByHierarchyId(request.getHierarchyId());
        validateAndGetSchemeCode(request, allocations);

        Optional<Products> productOpt = productsRepository.findByProductCode(request.getProductCode());
        if (productOpt.isEmpty()) {
            throw new EntityNotFoundException("Product not found with code: " + request.getProductCode());
        }

        Products product = productOpt.get();
        Double gstValue = product.getSgst();
        Double tdsValue = product.getTds();
        Boolean hasCharges = product.getHasCharges();
        Boolean isCommission = product.getIsCommissions();
        String tranxType = product.getTransactionType();

        String walletType = getWalletType(request);
        SchemeRate matchedSchemeRate = findMatchingSchemeRate(request);

        return new TransactionSchemeDetailsResponseDto(matchedSchemeRate, walletType, gstValue.toString(), tdsValue.toString(), hasCharges, isCommission, tranxType);
    }

    @Transactional
    public String processTransactionStatusChange(TransactionSchemeDetailsRequestDto dto, String vendorResponse) {

        String retailerId = dto.getHierarchyId();
        BigDecimal tranxAmount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(dto.getSchemeRate().getGross());
        String walletType = dto.getWalletType().toUpperCase();
        String taxValue = dto.getGstValue();
        String tranxType = dto.getTransactionType().toLowerCase();
        String baseTransactionId = dto.getBaseTranxId();

        AbstractWalletAmountTable wallet = getWalletByType(walletType, retailerId);

        if (tranxType.equals("credit")) {

            // On Success: update wallet, mirror, then process charges/commission
            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId, vendorResponse, "CREDIT");
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, vendorResponse);
        } else if (tranxType.equals("debit")) {
            // Step 1: Check balance before assuming pending
            if (hasSufficientBalance(retailerId, tranxAmount, grossAmount, walletType)) {
                return "Insufficient balance";
            }

            // Step 2: Assume PENDING - update wallet now
            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId, "PENDING", "DEBIT");

            mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), tranxType, tranxAmount, walletType, "TXN" + baseTransactionId, "Credit", "SUCCESS", dto);
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, "SUCCESS");
        }

        // Save wallet changes
        saveWallet(walletType, wallet);
        // CLEANUP OLD SETTLEMENT PENDING ENTRY
        deleteOldSettlementPendingEntry("TXN" + baseTransactionId, walletType);
        return "Transaction Successful";
    }


    @Override
    public List<Map<String, Object>> getSettlementTransactionsByFilter(
            LocalDate fromDate,
            LocalDate toDate,
            String subProductName,
            String settlementType,
            String walletType) {

        String tableName = getTransactionTable("RT", walletType);
        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");

        sql.append(" AND date_of_transaction BETWEEN ? AND ?");
        params.add(fromDate);
        params.add(toDate);

        sql.append(" AND sub_product_name = ?");
        params.add(subProductName);

        if ("PRE".equalsIgnoreCase(settlementType)) {
            sql.append(" AND tran_status = ?");
            params.add("SETTLEMENT PENDING");
        } else if ("POST".equalsIgnoreCase(settlementType)) {
            sql.append(" AND LOWER(action_on_balance_amount) LIKE ? AND tran_status = ?");
            params.add("%settlement%");
            params.add("SUCCESS");
        }

        sql.append(" ORDER BY date_of_transaction DESC, time_of_transaction DESC");

        return jdbcTemplate.queryForList(sql.toString(), params.toArray());
    }

    private final BlockingQueue<UpdateTransactionStatusRequest> queue = new LinkedBlockingQueue<>();

    @Override
    public CommonResponse<List<Map<String, String>>> bulkUpdateSettlementStatus(
            List<String> transactionIds, String walletType, String newStatus) {

        List<Map<String, String>> resultList = new ArrayList<>();

        for (String txnId : transactionIds) {
            UpdateTransactionStatusRequest request = new UpdateTransactionStatusRequest(
                    List.of(txnId),
                    "SETTLEMENT PENDING",
                    newStatus.toUpperCase(),
                    walletType,
                    null
            );

            Map<String, String> result = new HashMap<>();
            result.put("transactionId", txnId);

            try {
                updateTransactionStatus(request);
                result.put("status", "SUCCESS");
            } catch (Exception e) {
                log.error("Failed to update txn {}: {}", txnId, e.getMessage(), e);
                result.put("status", "FAILED: " + e.getMessage());
            }

            resultList.add(result);
        }

        return new CommonResponse<>(200, "Bulk update completed", resultList);
    }

    @Transactional
    public void deleteOldSettlementPendingEntry(
            String txnId,
            String walletType) {

        if ("POSTPAID".equalsIgnoreCase(walletType)) {
            retailerPostpaidTransactionDetailsRepository
                    .deleteSettlementPendingRecord(txnId);
        } else if ("PREPAID".equalsIgnoreCase(walletType)) {
            retailerPrepaidTransactionDetailsRepository
                    .deleteSettlementPendingRecord(txnId);
        }
    }


}
