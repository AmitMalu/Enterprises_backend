package com.im.admin.service.impl;

import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.TransactionService;
import com.im.admin.util.AbstractRetailerTransactionDetails;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;


@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

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
    private DynamicTransactionRepositoryImpl dynamicTransactionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CardTypeMasterRepository cardTypeMasterRepository;

//    @Override
//    public String getOperatorDetailsByCardBIN(CardTypeRequestDto cardTypeRequestDto) {
//        log.info("Fetching operator details for BIN: {}", cardTypeRequestDto.getCardBinNo());
//
//        return operatorsRepository.findByCardBinNo(cardTypeRequestDto.getCardBinNo())
//                .map(operator -> {
//                    log.info("Operator found - Name: {}, Code: {}", operator.getOperatorName(), operator.getOperatorCode());
//                    return "Operator Name: " + operator.getOperatorName() + ", Operator Code: " + operator.getOperatorCode();
//                })
//                .orElseGet(() -> {
//                    log.warn("No operator found for BIN No: {}", cardTypeRequestDto.getCardBinNo());
//                    return "No operator found for BIN No: " + cardTypeRequestDto.getCardBinNo();
//                });
//    }

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
        String baseTransactionId = commonMethods.generateBaseTransactionId(); // Single base ID for full flow

        String retailerId = dto.getHierarchyId();
        BigDecimal tranxAmount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(dto.getSchemeRate().getGross());
        String walletType = dto.getWalletType().toUpperCase();
        String taxValue = dto.getGstValue();
        String tranxType = dto.getTransactionType().toLowerCase();

        AbstractWalletAmountTable wallet = getWalletByType(walletType, retailerId);

        Boolean isSettlement = subProductsRepository.findIsSettlementByCode(dto.getSubProductCode());
        if (isSettlement == null) {
            throw new IllegalArgumentException("Invalid SubProductCode: " + dto.getSubProductCode());
        }

        if (tranxType.equalsIgnoreCase("credit")) {
            // Vendor call BEFORE wallet impact
            //String vendorResponse = callVendor(dto);

            if ("PENDING".equalsIgnoreCase(vendorCallbackStatus)) {
                // Just insert transaction with pending, no wallet impact
                insertTransaction(wallet, "PENDING", tranxAmount,
                        wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                        "Vendor Pending - Credit", buildTransactionId("TXN", baseTransactionId), "CREDIT", dto);
                return "Transaction pending - no wallet update";
            }

            if ("FAILURE".equalsIgnoreCase(vendorCallbackStatus) || "FAILED".equalsIgnoreCase(vendorCallbackStatus)) {
                insertTransaction(wallet, "FAILURE", tranxAmount,
                        wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                        "Vendor Failure - No Credit", buildTransactionId("FLD", baseTransactionId), "CREDIT", dto);
                return "Vendor transaction failed";
            }

            // Vendor SUCCESS, but settlement = true → mark as pending for future processing
            if (Boolean.TRUE.equals(isSettlement)) {
                insertTransaction(wallet, "SETTLEMENT_PENDING", tranxAmount,
                        wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                        "Settlement Pending - Credit " + walletType.toUpperCase(), buildTransactionId("TXN", baseTransactionId), "CREDIT", dto);
                return "Transaction marked as pending for settlement";
            }

            // On Success: update wallet, mirror, then process charges/commission
            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId, vendorCallbackStatus, "CREDIT");
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, vendorCallbackStatus);
        } else if (tranxType.equalsIgnoreCase("debit")) {
            // Step 1: Check balance before assuming pending
            if (!hasSufficientBalance(retailerId, tranxAmount, grossAmount, walletType)) {
                return "Insufficient balance";
            }

            // Step 2: Assume PENDING - update wallet now
            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId, "PENDING", "DEBIT");

            // Step 3: Call Vendor AFTER debiting
            String vendorResponse = callVendor(dto);

            if ("PENDING".equalsIgnoreCase(vendorResponse)) {
                return "Transaction pending - debit applied";
            }

            if ("FAILURE".equalsIgnoreCase(vendorResponse)) {
                // Reversal logic
                BigDecimal beforeReversal = wallet.getAvailableBalance();
                wallet.setAvailableBalance(beforeReversal.add(tranxAmount));
                wallet.setTotalCash(wallet.getTotalCash().add(tranxAmount));
                BigDecimal afterReversal = wallet.getAvailableBalance();

                updateTransactionStatus("TXN" + baseTransactionId, "FAILURE", walletType);

                insertTransaction(wallet, "REVERSAL", tranxAmount,
                        beforeReversal, afterReversal,
                        "Vendor Failed - Debit Reversed", buildTransactionId("REV", baseTransactionId), "DEBIT", dto);
                return "Vendor transaction failed and reversed";
            }

            if (Boolean.TRUE.equals(isSettlement)) {
                // Vendor SUCCESS, but settlement = true → mark as pending
                updateTransactionStatus("TXN" + baseTransactionId, "SETTLEMENT_PENDING", walletType);
                return "Transaction marked as pending for settlement";
            }

            // On Success: update PENDING → SUCCESS status and process mirror + charges
            updateTransactionStatus("TXN" + baseTransactionId, "SUCCESS", walletType);

            // Now perform mirroring, charges, commissions
            mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), tranxType, tranxAmount, walletType, "TXN" + baseTransactionId, "Credit", "SUCCESS", dto);
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, "SUCCESS");
        }

        // Save wallet changes
        saveWallet(walletType, wallet);
        return "Transaction Successful";
    }

    private void updateTransactionStatus(String transactionId, String status, String walletType) {
        if ("PREPAID".equalsIgnoreCase(walletType)) {
            Optional<RetailerPrepaidTransactionDetails> txnOpt = retailerPrepaidTransactionDetailsRepository.findByTransactionId(transactionId);
            if (txnOpt.isPresent()) {
                RetailerPrepaidTransactionDetails txn = txnOpt.get();
                txn.setActionOnBalanceAmount("Deduct Funds - PREPAID");
                txn.setTranStatus(status);
                retailerPrepaidTransactionDetailsRepository.save(txn);
            } else {
                throw new RuntimeException("Transaction not found in Prepaid table: " + transactionId);
            }
        } else if ("POSTPAID".equalsIgnoreCase(walletType)) {
            Optional<RetailerPostpaidTransactionDetails> txnOpt = retailerPostpaidTransactionDetailsRepository.findByTransactionId(transactionId);
            if (txnOpt.isPresent()) {
                RetailerPostpaidTransactionDetails txn = txnOpt.get();
                txn.setActionOnBalanceAmount("Deduct Funds - POSTPAID");
                txn.setTranStatus(status);
                retailerPostpaidTransactionDetailsRepository.save(txn);
            } else {
                throw new RuntimeException("Transaction not found in Postpaid table: " + transactionId);
            }
        } else {
            throw new IllegalArgumentException("Invalid wallet type: " + walletType);
        }
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

        switch (hierarchyType) {
            case "RT":
                return getRetailerWallet(walletType, hierarchyId);
            case "WL":
                return getWhiteLabelWallet(walletType, hierarchyId);
            case "AD":
            case "IM":
                return getAdminWallet(walletType, hierarchyId);
            // Add cases for Partner, Distributor if needed
            default:
                throw new IllegalArgumentException("Unsupported hierarchy for wallet: " + hierarchyId);
        }
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

    private String callVendor(TransactionSchemeDetailsRequestDto dto) {
        // Replace this with actual vendor API integration
        return "SUCCESS"; // or PENDING, FAILURE, SUCCESS
    }

    private void processChargesAndCommission(TransactionSchemeDetailsRequestDto dto, AbstractWalletAmountTable wallet,
                                             BigDecimal grossAmount, String gstValue, String baseTransactionId, String tranStatus) {
        if (dto.getHasCharge()) {
            processCharges(dto, wallet, grossAmount, gstValue, baseTransactionId, tranStatus); // GST only
        }

        if (dto.getIsCommission()) {
            processCommission(dto, baseTransactionId, tranStatus, dto); // TDS only
        }
    }

    private void processCharges(TransactionSchemeDetailsRequestDto dto, AbstractWalletAmountTable wallet,
                                BigDecimal grossAmountInput, String gstValue, String baseTransactionId, String tranStatus) {

        BigDecimal grossAmount;

        if ("PERCENTAGE".equalsIgnoreCase(dto.getSchemeRate().getRateType().toString())) {
            // gross is percentage of transaction amount
            BigDecimal percentage = BigDecimal.valueOf(dto.getSchemeRate().getGross());
            grossAmount = BigDecimal.valueOf(dto.getAmount())
                    .multiply(percentage)
                    .setScale(4, RoundingMode.HALF_UP);
        } else {
            grossAmount = grossAmountInput;
        }

        // Step 1: Calculate GST and base
        BigDecimal gstRate = new BigDecimal(gstValue)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(gstRate);
        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);
        BigDecimal gstAmount = grossAmount.subtract(baseAmount).setScale(4, RoundingMode.HALF_UP);

        // Step 2: Deduct base
        BigDecimal beforeBase = wallet.getAvailableBalance();
        wallet.setAvailableBalance(beforeBase.subtract(baseAmount));
        BigDecimal afterBase = wallet.getAvailableBalance();

        insertTransaction(wallet, tranStatus, baseAmount, beforeBase, afterBase,
                "Deduct Funds (Charge) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("CHG", baseTransactionId), "DEBIT", dto);

        //Credit the Gross/Charge amount to admin
        mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), "DEBIT", baseAmount, dto.getWalletType(), "CHG" + baseTransactionId, "(Charge)", tranStatus, dto);

        // Step 3: Deduct GST
        BigDecimal beforeGst = wallet.getAvailableBalance();
        wallet.setAvailableBalance(beforeGst.subtract(gstAmount));
        BigDecimal afterGst = wallet.getAvailableBalance();

        insertTransaction(wallet, tranStatus, gstAmount, beforeGst, afterGst,
                "Deduct Funds (GST) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("GST", baseTransactionId), "DEBIT", dto);

        // Step 4: Update wallet meta
        wallet.setUsedCash(wallet.getUsedCash().add(baseAmount.add(gstAmount)));
        wallet.setLastUpdatedAmount(gstAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Step 5: Credit GST to Admin
        creditGstToAdminOrWhitelabel(dto.getHierarchyId(), dto.getWalletType(), gstAmount, baseTransactionId, tranStatus, dto);

    }

    private void creditGstToAdmin(String receiverHierarchyId, String retailerId, String walletType, BigDecimal gstAmount, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        String hierarchyType = extractHierarchyType(receiverHierarchyId);
        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        // Step 1: Get current balance
        BigDecimal beforeBalance = getCurrentBalance(receiverHierarchyId, walletType);
        BigDecimal afterBalance = beforeBalance.add(gstAmount);

        String gstCreditTranxId = buildTransactionId("GST", baseTransactionId);

        // Step 2: Insert GST credit transaction
        insertTransactionForAdminHierarchy(
                transactionTable,
                "CREDIT",
                gstAmount,
                "GST collected",
                receiverHierarchyId,
                walletType,
                beforeBalance,
                afterBalance,
                "Add Funds (GST) - " + walletType.toUpperCase(),
                gstCreditTranxId, tranStatus, request
        );

        // Step 3: Update balance table
        updateAmountTable(hierarchyType, receiverHierarchyId, amountTable, gstAmount);
    }

    private void processCommission(TransactionSchemeDetailsRequestDto dto, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());

        // 1. Commission to Retailer
        if (dto.getSchemeRate().getRetailer() > 0) {
            BigDecimal retailerCommission = calculateCommissionAmount(
                    dto.getSchemeRate().getRetailer(),
                    BigDecimal.valueOf(dto.getAmount()),
                    dto.getSchemeRate().getRateType().toString()
            );
            distributeCommissionToHierarchy("RT", dto.getHierarchyId(), retailerCommission, dto.getWalletType(), tdsRate, baseTransactionId, tranStatus, request);
        }

        // 2. Traverse and distribute to parent hierarchies
        Retailer retailer = retailerRepository.findByRetailerId(dto.getHierarchyId())
                .orElseThrow(() -> new RuntimeException("Retailer not found: " + dto.getHierarchyId()));

        distributeCommissionToHierarchies(
                BigDecimal.valueOf(dto.getAmount()),
                retailer.getParentAccount(),
                dto.getSchemeRate(),
                dto.getWalletType(),
                tdsRate,
                baseTransactionId,
                new HashSet<>(),
                tranStatus, request
        );

        // 3. Admin-level commission (dynamically fetched)
        if (dto.getSchemeRate().getIm() > 0) {
            BigDecimal adminCommission = calculateCommissionAmount(
                    dto.getSchemeRate().getIm(),
                    BigDecimal.valueOf(dto.getAmount()),
                    dto.getSchemeRate().getRateType().toString()
            );

            getAdminHierarchyId(dto.getHierarchyId()).ifPresent(adminId -> {
                distributeCommissionToHierarchy("AD", adminId, adminCommission, dto.getWalletType(), tdsRate, baseTransactionId, tranStatus, request);
            });
        }
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

        log.info("Retailer ID: {} | Available Balance: {} | Total Deduction Amount: {}", retailerId, availableBalance, requestedAmount.add(grossAmount));

        return availableBalance != null && availableBalance.compareTo(requestedAmount.add(grossAmount)) >= 0;
    }

    private void distributeCommissionToHierarchies(BigDecimal amount, String hierarchyId, SchemeRate schemeRate,
                                                   String walletType, BigDecimal tdsRate, String baseTransactionId,
                                                   Set<String> processedMirrorSet, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        if (hierarchyId == null || hierarchyId.isEmpty()) return;

        if (!processedMirrorSet.add(hierarchyId)) {
            log.info("Skipping duplicate mirror/commission for hierarchy: {}", hierarchyId);
            return; // already processed
        }

        String hierarchyType = extractHierarchyType(hierarchyId);
        double rate = getCommissionRateByType(hierarchyType, schemeRate);
        BigDecimal commissionAmount = calculateCommissionAmount(rate, amount, schemeRate.getRateType().toString());

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("AD".equals(hierarchyType)) {
                log.info("Distributing commission directly to Admin: {}", hierarchyId);
                distributeCommissionToAdmin(hierarchyId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                return; // Admin reached, stop here
            }

            if ("WL".equals(hierarchyType)) {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Whitelabel not found: " + hierarchyId));

                if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                    log.info("Whitelabel {} isAdmin = true, full commission credited and stopping traversal", hierarchyId);
                    distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId, tranStatus, request);
                    return; // Admin WL reached, stop here
                }
            }

            // All other hierarchy levels
            distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId, tranStatus, request);
        }

        // Fix: Always continue traversal unless Admin is reached or Admin WL
        Optional<String> parentIdOpt = findParentHierarchy(hierarchyId);
        if (parentIdOpt.isPresent()) {
            String parentId = parentIdOpt.get();
            String parentType = extractHierarchyType(parentId);

            // Only skip if Admin was already handled above
            if (!"AD".equals(parentType)) {
                distributeCommissionToHierarchies(amount, parentId, schemeRate, walletType, tdsRate, baseTransactionId, processedMirrorSet, tranStatus, request);
            } else {
                // Admin found → still process it
                distributeCommissionToHierarchies(amount, parentId, schemeRate, walletType, tdsRate, baseTransactionId, processedMirrorSet, tranStatus, request);
            }
        } else {
            log.warn("No parent found for hierarchyId: {}", hierarchyId);
        }
    }

//    private void processChargesAndRetailerCommissionOnly(TransactionSchemeDetailsRequestDto dto, AbstractRetailerAmount wallet,
//                                                         BigDecimal grossAmount, String gstValue, String baseTransactionId, String tranStatus) {
//
//        if (dto.getHasCharge()) {
//            // Process charges but do NOT mirror them to Admin/Whitelabel
//            processChargesWithoutMirror(dto, wallet, grossAmount, gstValue, baseTransactionId, tranStatus);
//        }
//
//        if (dto.getIsCommission() && dto.getSchemeRate().getRetailer() > 0) {
//            // Only give commission to retailer — no further distribution
//            BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());
//            BigDecimal retailerCommission = calculateCommissionAmount(
//                    dto.getSchemeRate().getRetailer(),
//                    BigDecimal.valueOf(dto.getAmount()),
//                    dto.getSchemeRate().getRateType().toString()
//            );
//
//            distributeCommissionToHierarchy("RT", dto.getHierarchyId(), retailerCommission,
//                    dto.getWalletType(), tdsRate, baseTransactionId, tranStatus);
//        }
//    }


//    private void processChargesWithoutMirror(TransactionSchemeDetailsRequestDto dto, AbstractRetailerAmount wallet,
//                                             BigDecimal grossAmountInput, String gstValue, String baseTransactionId, String tranStatus) {
//
//        BigDecimal grossAmount = "PERCENTAGE".equalsIgnoreCase(String.valueOf(dto.getSchemeRate().getRateType()))
//                ? BigDecimal.valueOf(dto.getAmount()).multiply(BigDecimal.valueOf(dto.getSchemeRate().getGross())).setScale(4, RoundingMode.HALF_UP)
//                : grossAmountInput;
//
//        BigDecimal gstRate = new BigDecimal(gstValue).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
//        BigDecimal divisor = BigDecimal.ONE.add(gstRate);
//
//        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);
//        BigDecimal gstAmount = grossAmount.subtract(baseAmount).setScale(4, RoundingMode.HALF_UP);
//
//        BigDecimal beforeBase = wallet.getAvailableBalance();
//        wallet.setAvailableBalance(beforeBase.subtract(baseAmount));
//        insertTransaction(wallet, tranStatus, baseAmount, beforeBase, wallet.getAvailableBalance(),
//                "Deduct Funds (Charge) - " + dto.getWalletType(), buildTransactionId("CHG", baseTransactionId));
//
//        BigDecimal beforeGst = wallet.getAvailableBalance();
//        wallet.setAvailableBalance(beforeGst.subtract(gstAmount));
//        insertTransaction(wallet, tranStatus, gstAmount, beforeGst, wallet.getAvailableBalance(),
//                "Deduct Funds (GST) - " + dto.getWalletType(), buildTransactionId("GST", baseTransactionId));
//
//        wallet.setUsedCash(wallet.getUsedCash().add(baseAmount.add(gstAmount)));
//        wallet.setLastUpdatedAmount(gstAmount);
//        wallet.setLastUpdateDate(LocalDate.now());
//        wallet.setLastUpdateTime(LocalTime.now());
//    }


//    private <T extends AbstractRetailerAmount> void updateWalletWithoutMirror(TransactionSchemeDetailsRequestDto dto, T wallet, BigDecimal requestedAmount,
//                                                                              String tranxType, String baseTransactionId, String tranStatus) {
//        BigDecimal balanceBefore = wallet.getAvailableBalance();
//
//        if ("CREDIT".equalsIgnoreCase(tranxType)) {
//            wallet.setTotalCash(wallet.getTotalCash().add(requestedAmount));
//            wallet.setAvailableBalance(wallet.getAvailableBalance().add(requestedAmount));
//        } else {
//            wallet.setTotalCash(wallet.getTotalCash().subtract(requestedAmount));
//            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(requestedAmount));
//        }
//
//        wallet.setLastUpdatedAmount(requestedAmount);
//        wallet.setLastUpdateDate(LocalDate.now());
//        wallet.setLastUpdateTime(LocalTime.now());
//
//        insertTransaction(wallet, tranStatus, requestedAmount, balanceBefore,
//                wallet.getAvailableBalance(), "Pending Wallet Update", "TXN" + baseTransactionId);
//    }
//
//
//    private void mirrorCommissionToAdmin(String fromHierarchyId, BigDecimal commissionAmount, String walletType, String baseTransactionId, String tranStatus) {
//        Optional<String> parentOpt = findParentHierarchy(fromHierarchyId);
//        if (parentOpt.isEmpty()) return;
//
//        String parentId = parentOpt.get();
//        String parentType = extractHierarchyType(parentId);
//
//        while (true) {
//            if ("WL".equals(parentType)) {
//                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
//                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
//                    creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus); // Full amount, no TDS
//                    return;
//                }
//            }
//
//            if ("IM".equals(parentType) || "AD".equals(parentType)) {
//                creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus); // Full amount, no TDS
//                return;
//            }
//
//            parentOpt = findParentHierarchy(parentId);
//            if (parentOpt.isEmpty()) return;
//
//            parentId = parentOpt.get();
//            parentType = extractHierarchyType(parentId);
//        }
//    }
//
//    private void creditTdsToAdmin(String sourceHierarchyId, String walletType, BigDecimal tdsAmount, String baseTransactionId, String tranStatus) {
//        String currentId = sourceHierarchyId;
//
//        while (true) {
//            Optional<String> parentOpt = findParentHierarchy(currentId);
//            if (parentOpt.isEmpty()) return;
//
//            String parentId = parentOpt.get();
//            String parentType = extractHierarchyType(parentId);
//
//            if ("WL".equals(parentType)) {
//                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
//                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
//                    // Mirror to Whitelabel
//                    creditTdsToFinalAdmin(parentId, walletType, tdsAmount, baseTransactionId, tranStatus);
//                    return;
//                }
//            }
//
//            if ("IM".equals(parentType) || "AD".equals(parentType)) {
//                creditTdsToFinalAdmin(parentId, walletType, tdsAmount, baseTransactionId, tranStatus);
//                return;
//            }
//
//            currentId = parentId;
//        }
//    }

//    private void creditTdsToFinalAdmin(String adminHierarchyId, String walletType, BigDecimal tdsAmount, String baseTransactionId, String tranStatus) {
//        String transactionTable = getTransactionTable("AD", walletType);
//        String amountTable = getAmountTable("AD", walletType);
//
//        BigDecimal beforeBalance = getCurrentBalance(adminHierarchyId, walletType);
//        BigDecimal afterBalance = beforeBalance.add(tdsAmount);
//
//        String tdsCreditTranxId = buildTransactionId("TDS", baseTransactionId);
//
//        insertTransactionForAdminHierarchy(
//                transactionTable, "CREDIT", tdsAmount, "TDS collected from: " + adminHierarchyId,
//                adminHierarchyId, walletType, beforeBalance, afterBalance,
//                "Add Funds (TDS) - " + walletType.toUpperCase(), tdsCreditTranxId, tranStatus
//        );
//
//        updateAmountTable("AD", adminHierarchyId, amountTable, tdsAmount);
//    }


    private void mirrorTransactionToWhitelabelOrAdmin(
            String sourceHierarchyId, String sourceHierarchyType, String txnType, BigDecimal amount, String walletType,
            String mirrorTxnId, String actionOnBalance, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        String currentId = sourceHierarchyId;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("No parent found for: {}", currentId);
                return;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            // Defensive check
            if (parentType.equals("UNKNOWN")) {
                log.error("Invalid hierarchy type for ID: {}", parentId);
                return;
            }

            // Fetch current balance
            BigDecimal beforeBalance = getCurrentBalance(parentId, walletType);
            BigDecimal afterBalance = txnType.equalsIgnoreCase("CREDIT")
                    ? beforeBalance.add(amount)
                    : beforeBalance.subtract(amount);

            String remarks = String.format(
                    "Mirror %s Entry for %s (%s) - From: %s",
                    txnType, sourceHierarchyType, walletType, sourceHierarchyId
            );

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent()) {
                    boolean isAdminWL = Boolean.TRUE.equals(wlOpt.get().getIsAdmin());

                    if (isAdminWL) {
                        // Insert mirror transaction only if WL is Admin
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

                        log.info("Stopped mirror at Admin WL: {}", parentId);
                        return;
                    } else {
                        // Do not insert into WL if not admin – go up to Admin
                        currentId = parentId;
                        continue;
                    }
                } else {
                    log.error("WhiteLabel not found for ID: {}", parentId);
                    return;
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
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
                return;
            }

            // Continue upward
            currentId = parentId;
        }
    }

    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
                                                 BigDecimal commissionAmount, String walletType,
                                                 BigDecimal tdsRate, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("Processing commission for {} | Hierarchy ID: {} | Amount (Incl. TDS): {} | Wallet Type: {} | TDS Rate: {}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);

        // === CASE 1: IM or AD — Credit only, no TDS or Mirror ===
        if ("IM".equals(hierarchyType) || "AD".equals(hierarchyType)) {
            log.info("{} hierarchy detected. Crediting full commission without TDS or mirror.", hierarchyType);

            String transactionTable = getTransactionTable(hierarchyType, walletType);
            String amountTable = getAmountTable(hierarchyType, walletType);

            BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
            BigDecimal afterBalance = beforeBalance.add(commissionAmount);
            String txnId = buildTransactionId("COM", baseTransactionId);

            insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                    "Commission credited (Direct IM/AD)", hierarchyId, walletType,
                    beforeBalance, afterBalance,
                    "Add Funds (Commission) - " + walletType.toUpperCase(), txnId, tranStatus, request);

            updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

            return; // Stop further processing (no TDS or mirror)
        }

        // === CASE 2: WhiteLabel isAdmin = true — Credit only, no TDS or Mirror ===
        boolean isAdminWL = false;
        if ("WL".equals(hierarchyType)) {
            isAdminWL = whiteLabelsRepository.findById(hierarchyId)
                    .map(WhiteLabels::getIsAdmin)
                    .orElse(false);

            if (isAdminWL) {
                log.info("WhiteLabel {} isAdmin = true. Crediting full commission, skipping TDS and mirror.", hierarchyId);

                String transactionTable = getTransactionTable(hierarchyType, walletType);
                String amountTable = getAmountTable(hierarchyType, walletType);

                BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
                BigDecimal afterBalance = beforeBalance.add(commissionAmount);
                String txnId = buildTransactionId("COM", baseTransactionId);

                insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                        "Commission credited (Admin WL)", hierarchyId, walletType,
                        beforeBalance, afterBalance,
                        "Add Funds (Commission) - " + walletType.toUpperCase(), txnId, tranStatus, request);

                updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

                return; // Stop further processing (no TDS or mirror)
            }
        }

        // === CASE 3: All other hierarchies — Credit, Mirror, TDS (if applicable) ===
        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);
        BigDecimal afterCreditBalance = beforeBalance.add(commissionAmount);
        String comTxnId = buildTransactionId("COM", baseTransactionId);

        // Credit commission
        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Commission credited", hierarchyId, walletType,
                beforeBalance, afterCreditBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, tranStatus, request);

        updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

        // Mirror commission (DEBIT from Admin/WL)
        if (!"pending".equalsIgnoreCase(tranStatus)) {
            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "DEBIT", commissionAmount, walletType,
                    comTxnId, "Deduct Funds (Commission) - " + walletType.toUpperCase(), tranStatus, request);
        }

        // TDS deduction (if applicable)
        BigDecimal tdsAmount = commissionAmount.multiply(tdsRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afterTdsBalance = afterCreditBalance.subtract(tdsAmount);
            String tdsTxnId = buildTransactionId("TDS", baseTransactionId);

            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount,
                    "TDS Deduction", hierarchyId, walletType,
                    afterCreditBalance, afterTdsBalance,
                    "Deduct Funds (TDS) - " + walletType.toUpperCase(), tdsTxnId, tranStatus, request);

            // Mirror TDS (CREDIT to Admin/WL)
            if (!"pending".equalsIgnoreCase(tranStatus)) {
                mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "CREDIT", tdsAmount, walletType,
                        tdsTxnId, "Add Funds (TDS) - " + walletType.toUpperCase(), tranStatus, request);
            }
            updateAmountTable(hierarchyType, hierarchyId, amountTable, tdsAmount.negate());
        }
    }

    private void distributeCommissionToAdmin(String hierarchyId, BigDecimal commissionAmount, String walletType, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        String currentId = hierarchyId;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) return;

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        // This is final Admin, credit full commission without TDS
                        creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                        return;
                    }
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                // Final Admin - direct commission (not mirror) should also come here
                creditCommissionToFinalAdmin(parentId, commissionAmount, walletType, baseTransactionId, tranStatus, request);
                return;
            }

            currentId = parentId;
        }
    }

    private void creditCommissionToFinalAdmin(String adminId, BigDecimal commissionAmount, String walletType, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        log.info("Processing commission for Admin | Admin ID: {} | Amount: {} | Wallet Type: {} (No TDS Deduction)",
                adminId, commissionAmount, walletType);

        BigDecimal beforeTransactionBalance = getCurrentBalance(adminId, walletType);
        BigDecimal afterTransactionBalance = beforeTransactionBalance.add(commissionAmount);

        String amountTable = getAmountTable("AD", walletType);
        String transactionTable = getTransactionTable("AD", walletType);
        String comTxnId = buildTransactionId("COM", baseTransactionId);

        insertTransactionForAdminHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Admin Commission credited", adminId, walletType,
                beforeTransactionBalance, afterTransactionBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, tranStatus, request);

        updateAmountTable("AD", adminId, amountTable, commissionAmount);
    }


    private String extractHierarchyType(String hierarchyId) {
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

    private String getTransactionTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);
        return entityName + (walletType.equalsIgnoreCase("PREPAID") ? "_prepaid_transaction_details" : "_postpaid_transaction_details");
    }

    private String getAmountTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);
        return entityName + (walletType.equalsIgnoreCase("PREPAID") ? "_prepaid_amount" : "_postpaid_amount");
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

    //This method is for admin commission credit only
    @Transactional
    private void insertTransactionForAdminHierarchy(String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId, String walletType, BigDecimal beforeBalance,
                                                    BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeBalance, afterBalance);

        String operatorName = request.getOperatorName();

        String sql = "INSERT INTO " + transactionTable +
                " (hierarchy_id,service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, operator_name) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus, :operatorName)";

        try {
            entityManager.createNativeQuery(sql)
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
                    .executeUpdate();
        } catch (Exception e) {
            log.error(" Failed to insert transaction for admin: {}", e.getMessage(), e);
            throw e; // or handle more gracefully
        }

    }

    //This method is for commission credit entry
    private void insertTransactionForHierarchy(String transactionTable, String service, BigDecimal amount, String remarks,
                                               String hierarchyId, String walletType, BigDecimal beforeTransactionBalance,
                                               BigDecimal afterTransactionBalance, String actionOnBalance, String tranxId, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        //GST entry for ADMIN 1st insertTrasnaction Method
        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {} | Action: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeTransactionBalance, afterTransactionBalance, actionOnBalance);

        String hierarchyColumnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "INSERT INTO " + transactionTable +
                " (" + hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalance, :finalBalance, :tranxId, :tranStatus)";

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
                .executeUpdate();
    }

    private void updateAmountTable(String hierarchyType, String hierarchyId, String amountTable, BigDecimal amount) {
        log.info("Updating {} | Hierarchy Type: {} | Hierarchy ID: {} | Final Amount to Add: {}",
                amountTable, hierarchyType, hierarchyId, amount);

        String columnName = getHierarchyColumnName(hierarchyType);

        String sql = "UPDATE " + amountTable + " SET available_balance = available_balance + :amount, " +
                "last_updated_amount = :amount, last_updated_date = :date, last_updated_time = :time " +
                "WHERE " + columnName + " = :hierarchyId";

        entityManager.createNativeQuery(sql)
                .setParameter("amount", amount)
                .setParameter("date", new Date())
                .setParameter("time", new Time(System.currentTimeMillis()))
                .setParameter("hierarchyId", hierarchyId)
                .executeUpdate();
    }

    private String getHierarchyColumnName(String hierarchyType) {
        return switch (hierarchyType) {
            case "ET" -> "enterprises_id";
            case "PT" -> "partner_id";
            case "RT" -> "retailer_id";
            case "DT" -> "distributor_id";
            case "SD" -> "super_distributor_id";
            case "WL" -> "white_label_id";
            case "AD" -> "admin_id";
            default -> throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        };
    }

    private BigDecimal getCurrentBalance(String hierarchyId, String walletType) {
        String amountTable = getAmountTable(extractHierarchyType(hierarchyId), walletType);
        String columnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

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

    private <T extends AbstractWalletAmountTable> void updateWallet(TransactionSchemeDetailsRequestDto dto, T wallet, BigDecimal requestedAmount,
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service) {

        String actionOnBalance;
        //String service = null;

        if ("PENDING".equalsIgnoreCase(tranStatus)) {
            handlePendingTransaction(wallet, tranxType, requestedAmount, baseTransactionId, service, dto);
            return;
        }

        // *** Credit or Debit Operation ***
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(requestedAmount));
            actionOnBalance = "Add Funds - " + dto.getWalletType().toUpperCase();
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(requestedAmount));
            actionOnBalance = "Deduct Funds - " + dto.getWalletType().toUpperCase();
        }

        wallet.setLastUpdatedAmount(requestedAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Insert transaction
        insertTransaction(wallet, tranStatus, requestedAmount, balanceBefore,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service, dto);

        // Traverse to check for WhiteLabel with isAdmin = true
        String currentId = dto.getHierarchyId();
        boolean mirrorHandled = false;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);

            if (parentOpt.isEmpty()) {
                break;
            }

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    // Mirror transaction to WhiteLabel (isAdmin=true)
                    mirrorTransactionToHierarchy(parentId, dto.getHierarchyId(), tranxType.toUpperCase(), requestedAmount,
                            dto.getWalletType().toUpperCase(), "TXN" + baseTransactionId, "WhiteLabel Admin", tranStatus, dto);
                    mirrorHandled = true;
                    break;
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                // Reached Admin (IM/AD)
                if (!mirrorHandled) {
                    // For Admin (fallback)
                    mirrorTransactionToHierarchy(parentId, dto.getHierarchyId(), tranxType.toUpperCase(), requestedAmount,
                            dto.getWalletType().toUpperCase(), "TXN" + baseTransactionId, "Admin", tranStatus, dto);
                }
                break;
            }

            currentId = parentId;
        }
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


    public void insertTransaction(AbstractWalletAmountTable wallet, String tranStatus, BigDecimal amount,
                                  BigDecimal balanceBeforeTranx, BigDecimal balanceAfterTranx, String actionOnBalance, String tranxId, String service, TransactionSchemeDetailsRequestDto dto) {

        if (wallet instanceof RetailerPrepaidAmount) {
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
            retailerPrepaidTransactionDetailsRepository.save(transaction);
        } else if (wallet instanceof RetailerPostpaidAmount) {
            RetailerPostpaidTransactionDetails transaction = new RetailerPostpaidTransactionDetails();
            transaction.setTransactionId(tranxId);
            transaction.setDateOfTransaction(LocalDate.now());
            transaction.setTimeOfTransaction(LocalTime.now());
            transaction.setService(service);
            transaction.setBalBeforeTran(balanceBeforeTranx);
            transaction.setActionOnBalanceAmount(actionOnBalance);
            transaction.setAmount(amount);
            transaction.setBalAfterTran(balanceAfterTranx);
            transaction.setTranStatus(tranStatus);
            transaction.setFinalBalance(balanceAfterTranx);
            transaction.setRetailerId(((RetailerPostpaidAmount) wallet).getRetailerId().getRetailerId());
            transaction.setOperatorName(dto.getOperatorName());
            transaction.setSubProductName(dto.getSubProductCode());
            retailerPostpaidTransactionDetailsRepository.save(transaction);
        }
    }

    private void validateAmount(TransactionSchemeDetailsRequestDto request) {
        if (!Objects.equals(request.getAmount(), request.getConfirmAmount())) {
            log.error("Amount mismatch! Transaction failed. Amount: {}, ConfirmAmount: {}", request.getAmount(), request.getConfirmAmount());
            throw new RuntimeException("Amount mismatch! Transaction failed.");
        }
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
                    if (rate.getSlabType() == SlabType.FIXED) {
                        return Double.compare(amount, rate.getFixedSlabValue()) == 0;
                    } else {
                        return amount >= rate.getSlabStart() && amount <= rate.getSlabEnd();
                    }
                })
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No matching Scheme Rate found for amount: " + request.getAmount()));
    }

    public String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }

    private BigDecimal calculateCommissionAmount(double rate, BigDecimal transactionAmount, String rateType) {
        BigDecimal commissionRate = BigDecimal.valueOf(rate);
        BigDecimal commission;

        if ("PERCENTAGE".equalsIgnoreCase(rateType)) {
            commission = transactionAmount.multiply(commissionRate);
        } else {
            commission = commissionRate;
        }

        // Cap commission to max of transaction amount
        if (commission.compareTo(transactionAmount) > 0) {
            commission = transactionAmount;
        }

        return commission.setScale(2, RoundingMode.HALF_UP);
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
                                              BigDecimal amount, String walletType, String txnId, String action, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        String targetType = extractHierarchyType(targetHierarchyId);
        String mirrorTxnType = originalTxnType.equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";

        BigDecimal currentBalance = getCurrentBalance(targetHierarchyId, walletType);

        if ("DEBIT".equalsIgnoreCase(mirrorTxnType) && currentBalance.compareTo(amount) < 0) {
            log.error("{} does not have enough balance to mirror DEBIT transaction. Required: {}, Available: {}",
                    targetType, amount, currentBalance);
            throw new RuntimeException(targetType + " wallet has insufficient balance to mirror transaction.");
        }

        BigDecimal beforeBalance = currentBalance;
        BigDecimal afterBalance = mirrorTxnType.equals("CREDIT")
                ? beforeBalance.add(amount)
                : beforeBalance.subtract(amount);

        String actionOnBalanceAmount = mirrorTxnType.equalsIgnoreCase("DEBIT")
                ? "Debit Funds " + action + " - " + walletType.toUpperCase()
                : "Add Funds " + action + " - " + walletType.toUpperCase();

        insertTransactionForAdminHierarchy(
                getTransactionTable(targetType, walletType),
                mirrorTxnType,
                amount,
                "Mirror Transaction for " + originalTxnType,
                sourceHierarchyId,
                walletType,
                beforeBalance,
                afterBalance,
                actionOnBalanceAmount,
                txnId, tranStatus, request
        );

        // Update target's balance table
        updateAmountTable(targetType, targetHierarchyId, getAmountTable(targetType, walletType), beforeBalance.subtract(afterBalance));
    }

    private void mirrorTransactionToAdminOrWhitelabel(String hierarchyId, String txnType, BigDecimal amount, String walletType, String txnId, String action, String tranStatus, TransactionSchemeDetailsRequestDto request) {
        String currentId = hierarchyId;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    // Mirror to WhiteLabel Admin
                    mirrorTransactionToHierarchy(parentId, hierarchyId, txnType, amount, walletType, txnId, action, tranStatus, request);
                    return;
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                mirrorTransactionToHierarchy(parentId, hierarchyId, txnType, amount, walletType, txnId, action, tranStatus, request);
                return;
            }

            currentId = parentId;
        }

    }

    private void creditGstToAdminOrWhitelabel(String sourceHierarchyId, String walletType, BigDecimal gstAmount, String baseTransactionId, String tranStatus, TransactionSchemeDetailsRequestDto request) {

        String retailerId = sourceHierarchyId;
        String currentId = sourceHierarchyId;

        while (true) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) return;

            String parentId = parentOpt.get();
            String parentType = extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    // Mirror GST to Whitelabel
                    creditGstToAdmin(parentId, retailerId, walletType, gstAmount, baseTransactionId, tranStatus, request);
                    return;
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                creditGstToAdmin(parentId, retailerId, walletType, gstAmount, baseTransactionId, tranStatus, request);
                return;
            }

            currentId = parentId;
        }
    }

    private void reverseTransaction(Object transaction, String tranxTable, String amountTable, String failureRemark, String mirrorTargetId) {
        BigDecimal amount = new BigDecimal(String.valueOf(getField(transaction, "amount")));
        String service = (String) getField(transaction, "service");

        // Dynamically resolve ID column
        String idColumn = getIdColumn(amountTable);
        String userId = (String) getField(transaction, idColumn);

        // Fetch balance
        BigDecimal currentBalance = dynamicTransactionRepository.getBalance(amountTable, userId);

        // Invert service
        String reversedService = service.equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";

        BigDecimal newBalance = reversedService.equalsIgnoreCase("CREDIT")
                ? currentBalance.subtract(amount)
                : currentBalance.add(amount);

        // Clone transaction
        Object reversal = cloneTransaction(transaction);

        // === Set core reversal fields ===
        setField(reversal, "tran_status", "FAILURE");
        setField(reversal, "service", reversedService);
        setField(reversal, "remarks", "Auto-Reversed due to status update");
        setField(reversal, "transaction_id", "REV" + getField(transaction, "transaction_id"));
        setField(reversal, "bal_before_tran", currentBalance);
        setField(reversal, "bal_after_tran", newBalance);

        // === Set additional fields ===
        setField(reversal, "amount", amount);
        setField(reversal, "final_balance", newBalance);
        setField(reversal, "failure_remarks", failureRemark);
        setField(reversal, "action_on_balance_amount", reversedService);
        setField(reversal, "updated_date_of_transaction", LocalDate.now());
        setField(reversal, "updated_time_of_transaction", LocalTime.now());

        // === Set hierarchy-specific IDs ===
        setHierarchyFields(reversal, tranxTable, userId, mirrorTargetId);

        // Save and update
        dynamicTransactionRepository.insert(tranxTable, reversal);
        dynamicTransactionRepository.updateBalance(amountTable, userId, newBalance);
    }


    private void setHierarchyFields(Object reversal, String tableName, String userId, String mirrorTargetId) {
        String idColumn = getIdColumn(tableName);
        setField(reversal, idColumn, userId);

        if (tableName.startsWith("white_label") || tableName.startsWith("admin")) {
            if (mirrorTargetId != null && !mirrorTargetId.isEmpty()) {
                setField(reversal, "hierarchy_id", mirrorTargetId); // mirror txn
            } else {
                setField(reversal, "hierarchy_id", userId); // self commission
            }
        }
    }


    private String getIdColumn(String tableName) {
        if (tableName.startsWith("retailer")) return "retailer_id";
        if (tableName.startsWith("distributor")) return "distributor_id";
        if (tableName.startsWith("partner")) return "partner_id";
        if (tableName.startsWith("super_distributor")) return "super_distributor_id";
        if (tableName.startsWith("white_label")) return "white_label_id";
        if (tableName.startsWith("enterprises")) return "enterprise_id";
        if (tableName.startsWith("admin")) return "admin_id";
        throw new RuntimeException("Unknown hierarchy table: " + tableName);
    }


    public Object cloneTransaction(Object transaction) {
        try {
            Class<?> clazz = transaction.getClass();
            Object newTxn = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(transaction);
                field.set(newTxn, value);
            }

            return newTxn;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone transaction", e);
        }
    }

    public static String getField(Object target, String fieldName) {
        if (target instanceof Map) {
            Object value = ((Map<?, ?>) target).get(fieldName);
            if (value != null) {
                return String.valueOf(value);
            } else {
                throw new RuntimeException("Field not found in map: " + fieldName);
            }
        } else {
            Field field = ReflectionUtils.findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return String.valueOf(ReflectionUtils.getField(field, target));
            }
            throw new RuntimeException("Field not found: " + fieldName);
        }
    }

    public static void setField(Object target, String fieldName, Object value) {
        if (target instanceof Map) {
            ((Map<String, Object>) target).put(fieldName, value);
        } else {
            Field field = ReflectionUtils.findField(target.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, target, value);
            } else {
                throw new RuntimeException("Field not found: " + fieldName);
            }
        }
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

                    if (!"SETTLEMENT PENDING".equalsIgnoreCase(prepaidTxn.getTranStatus())) {
                        lastResult.append("SKIPPED: ").append(fullTxnId).append(" already processed with status: ").append(prepaidTxn.getTranStatus()).append("\n");
                        continue;
                    }

                    txn = prepaidTxn;

                } else if ("POSTPAID".equals(walletType)) {
                    RetailerPostpaidTransactionDetails postpaidTxn = retailerPostpaidTransactionDetailsRepository
                            .findByTransactionId(fullTxnId)
                            .orElseThrow(() -> new RuntimeException("Postpaid transaction not found"));

                    if (!"SETTLEMENT PENDING".equalsIgnoreCase(postpaidTxn.getTranStatus())) {
                        lastResult.append("SKIPPED: ").append(fullTxnId).append(" already processed with status: ").append(postpaidTxn.getTranStatus()).append("\n");
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

                Operators operator = operatorsRepository.findByOperatorName(operatorName)
                        .orElseThrow(() -> new RuntimeException("Operator not found: " + operatorName));

                SubProducts subProduct = subProductsRepository.findBySubProductTitle(subProductName)
                        .orElseThrow(() -> new RuntimeException("SubProduct not found: " + subProductName));

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

                                DynamicWalletDTO dynamicWallet = dynamicWalletRepository.findWallet(amountTable, idColumnName, hierarchyId);
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
            if (!hasSufficientBalance(retailerId, tranxAmount, grossAmount, walletType)) {
                return "Insufficient balance";
            }

            // Step 2: Assume PENDING - update wallet now
            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId, "PENDING", "DEBIT");

            mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), tranxType, tranxAmount, walletType, "TXN" + baseTransactionId, "Credit", "SUCCESS", dto);
            //mirrorIfRequired(dto, wallet, tranxType, tranxAmount, baseTransactionId, "SUCCESS");
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId, "SUCCESS");
        }

        // Save wallet changes
        saveWallet(walletType, wallet);
        return "Transaction Successful";
    }

    public List<TransactionSearchResponseDTO> getTransactions(
            String hierarchyId, String transactionId, String status, String walletType) {

        return searchTransactions(transactionId, status, hierarchyId, walletType
        );
    }

    public List<TransactionSearchResponseDTO> searchTransactions(
            String transactionId, String status, String hierarchyId, String walletType) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String hierarchyColumnName = commonMethods.getHierarchyColumnName(hierarchyType);

        StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (transactionId != null && !transactionId.isEmpty()) {
            sql.append(" AND transaction_id = ?");
            params.add(transactionId);
        }

        if (status != null && !status.isEmpty()) {
            sql.append(" AND tran_status = ?");
            params.add(status);
        }

        if (hierarchyColumnName != null && !hierarchyColumnName.isEmpty()) {
            sql.append(" AND ").append(hierarchyColumnName).append("= ?");
            params.add(hierarchyId);
        }

        sql.append(" ORDER BY date_of_transaction DESC, time_of_transaction DESC");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            TransactionSearchResponseDTO resp = new TransactionSearchResponseDTO();
            resp.setTransactionId(rs.getString("transaction_id"));
            resp.setHierarchyId(rs.getString(hierarchyColumnName));
            resp.setWalletType(walletType);
            resp.setTranStatus(rs.getString("tran_status"));
            resp.setAmount(rs.getBigDecimal("amount"));
            resp.setOperatorName(rs.getString("operator_name"));
            return resp;
        });
    }


    @Override
    public List<Map<String, Object>> getSettlementTransactionsByFilter(
            LocalDate fromDate,
            LocalDate toDate,
            String subProductName,
            String settlementType,
            String walletType) {

        String tableName = commonMethods.getTransactionTable("RT", walletType);
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

    @Async
    @Transactional
    public CompletableFuture<Map<String, String>> updateStatus(UpdateTransactionStatusRequest request) {
        Map<String, String> result = new HashMap<>();
        try {
            updateTransactionStatus(request);
            result.put("transactionId", String.join(",", request.getTransactionIds()));
            result.put("status", "SUCCESS");
        } catch (Exception e) {
            result.put("transactionId", String.join(",", request.getTransactionIds()));
            result.put("status", "FAILED: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(result);
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



}
