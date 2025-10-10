package com.im.admin.util;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.BbpsTranactionRequestDto;
import com.im.admin.dto.BillRechargeResponseDTO;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.Date;


@Component
@Slf4j
public class BBPSRelatedTransactions {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;

    public void processCharges(BbpsTranactionRequestDto dto, AbstractWalletAmountTable wallet) {

        BigDecimal grossAmount;

        if ("PERCENTAGE".equalsIgnoreCase(dto.getSchemeRate().getRateType().toString())) {
            // gross is percentage of transaction amount
            BigDecimal percentage = BigDecimal.valueOf(dto.getSchemeRate().getGross());
            grossAmount = BigDecimal.valueOf(dto.getTxnAmount())
                    .multiply(percentage)
                    .setScale(4, RoundingMode.HALF_UP);
        } else {
            grossAmount = BigDecimal.valueOf(dto.getTxnAmount());
        }

        // Step 1: Calculate GST and base
        BigDecimal gstRate = new BigDecimal(dto.getGst())
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(gstRate);
        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);
        BigDecimal gstAmount = grossAmount.subtract(baseAmount).setScale(4, RoundingMode.HALF_UP);

        // Check balance before deducting base amount
        if (wallet.getAvailableBalance().compareTo(baseAmount) < 0) {
            throw new RuntimeException("Insufficient balance for deduction.");
        }

        // Step 2: Deduct base
        BigDecimal beforeBase = wallet.getAvailableBalance();
        wallet.setAvailableBalance(beforeBase.subtract(baseAmount));
        BigDecimal afterBase = wallet.getAvailableBalance();

        insertTransaction(wallet, dto.getVendorResponse(), baseAmount, beforeBase, afterBase,
                "Deduct Funds (Charge) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("CHG", dto.getTransactionId()), "DEBIT", dto);

        if (wallet.getAvailableBalance().compareTo(gstAmount) < 0) {
            throw new RuntimeException("Insufficient balance for GST deduction. Required: "
                    + gstAmount + ", Available: " + wallet.getAvailableBalance());
        }

        //Credit the Gross/Charge amount to admin
        mirrorTransactionToAdminOrWhitelabel(dto.getHierarchyId(), "DEBIT", baseAmount, dto.getWalletType(), "CHG" + dto.getTransactionId(), "(Charge)", dto.getVendorResponse(), dto);

        // Step 3: Deduct GST
        BigDecimal beforeGst = wallet.getAvailableBalance();
        wallet.setAvailableBalance(beforeGst.subtract(gstAmount));
        BigDecimal afterGst = wallet.getAvailableBalance();

        insertTransaction(wallet, dto.getVendorResponse(), gstAmount, beforeGst, afterGst,
                "Deduct Funds (GST) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("GST", dto.getTransactionId()), "DEBIT", dto);

        // Step 4: Update wallet meta
        wallet.setUsedCash(wallet.getUsedCash().add(baseAmount.add(gstAmount)));
        wallet.setLastUpdatedAmount(gstAmount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Step 5: Credit GST to Admin or White label admin.
        creditGstToAdminOrWhitelabel(dto.getHierarchyId(), dto.getWalletType(), gstAmount, dto.getTransactionId(), dto);

    }

    private void creditGstToAdminOrWhitelabel(String sourceHierarchyId, String walletType, BigDecimal gstAmount, String baseTransactionId, BbpsTranactionRequestDto request) {

        String retailerId = sourceHierarchyId;
        String currentId = sourceHierarchyId;

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent() && Boolean.TRUE.equals(wlOpt.get().getIsAdmin())) {
                    // Mirror GST to Whitelabel
                    creditGstToAdmin(parentId, retailerId, walletType, gstAmount, baseTransactionId, request.getVendorResponse(), request);
                    break;
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                creditGstToAdmin(parentId, retailerId, walletType, gstAmount, baseTransactionId, request.getVendorResponse(), request);
                break;
            }

            currentId = parentId;
        }
    }

    public void creditGstToAdmin(String receiverHierarchyId, String retailerId, String walletType, BigDecimal gstAmount, String baseTransactionId, String tranStatus, BbpsTranactionRequestDto request) {

        String hierarchyType = commonMethods.extractHierarchyType(receiverHierarchyId);
        String transactionTable = commonMethods.getTransactionTable(hierarchyType, walletType);
        String amountTable = commonMethods.getAmountTable(hierarchyType, walletType);

        // Step 1: Get current balance
        BigDecimal beforeBalance = commonMethods.getCurrentBalance(receiverHierarchyId, walletType);
        BigDecimal afterBalance = beforeBalance.add(gstAmount);

        String gstCreditTranxId = buildTransactionId("GST", baseTransactionId);

        // Step 2: Insert GST credit transaction
        insertTransactionForAdminHierarchy(
                transactionTable,
                "CREDIT",
                gstAmount,
                "GST collected",
                receiverHierarchyId,
                beforeBalance,
                afterBalance,
                "Add Funds (GST) - " + walletType.toUpperCase(),
                gstCreditTranxId, request
        );

        // Step 3: Update balance table
        updateAmountTable(hierarchyType, receiverHierarchyId, amountTable, gstAmount);
    }

    //This method is for admin commission credit only
    private void insertTransactionForAdminHierarchy(String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId, BigDecimal beforeBalance,
                                                    BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId, BbpsTranactionRequestDto request) {

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
                    .setParameter("tranStatus", request.getVendorResponse())
                    .setParameter("operatorName", operatorName)
                    .executeUpdate();
        } catch (Exception e) {
            log.error(" Failed to insert transaction for admin: {}", e.getMessage(), e);
            throw e; // or handle more gracefully
        }

    }


    public void insertTransaction(AbstractWalletAmountTable wallet, String tranStatus, BigDecimal amount,
                                  BigDecimal balanceBeforeTranx, BigDecimal balanceAfterTranx, String actionOnBalance, String tranxId, String service, BbpsTranactionRequestDto dto) {

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
            transaction.setSubProductName(dto.getSubProductName());
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
            transaction.setSubProductName(dto.getSubProductName());
            retailerPostpaidTransactionDetailsRepository.save(transaction);
        }
    }

    public void mirrorTransactionToAdminOrWhitelabel(String hierarchyId, String txnType, BigDecimal amount, String walletType, String txnId, String action, String tranStatus, BbpsTranactionRequestDto request) {
        String currentId = hierarchyId;

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

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

    @Transactional
    public void mirrorTransactionToHierarchy(String targetHierarchyId, String sourceHierarchyId, String originalTxnType,
                                             BigDecimal amount, String walletType, String txnId, String action, String tranStatus, BbpsTranactionRequestDto request) {

        String targetType = commonMethods.extractHierarchyType(targetHierarchyId);
        String mirrorTxnType = originalTxnType.equalsIgnoreCase("CREDIT") ? "DEBIT" : "CREDIT";

        BigDecimal currentBalance = commonMethods.getCurrentBalance(targetHierarchyId, walletType);

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
                commonMethods.getTransactionTable(targetType, walletType),
                mirrorTxnType,
                amount,
                "Mirror Transaction for " + originalTxnType,
                sourceHierarchyId,
                beforeBalance,
                afterBalance,
                actionOnBalanceAmount,
                txnId, request
        );

        // Update target's balance table
        updateAmountTable(targetType, targetHierarchyId, commonMethods.getAmountTable(targetType, walletType), afterBalance.subtract(beforeBalance));
    }

    public void updateAmountTable(String hierarchyType, String hierarchyId, String amountTable, BigDecimal amount) {
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

    public String getHierarchyColumnName(String hierarchyType) {
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

    public String buildTransactionId(String prefix, String baseId) {
        return prefix.trim() + baseId.trim();
    }

    public void processCommission(BbpsTranactionRequestDto bbpsTranactionRequestDto) {

        BbpsTranactionRequestDto request = new BbpsTranactionRequestDto();
        request.setVendorResponse("SUCCESS");
        // 1. Commission to Retailer
        if (bbpsTranactionRequestDto.getSchemeRate().getRetailer() > 0) {
            BigDecimal retailerCommission = commonMethods.calculateCommissionAmount(
                    bbpsTranactionRequestDto.getSchemeRate().getRetailer(),
                    BigDecimal.valueOf(bbpsTranactionRequestDto.getTxnAmount()),
                    bbpsTranactionRequestDto.getSchemeRate().getRateType().toString()
            );
            distributeCommissionToHierarchy("RT", bbpsTranactionRequestDto.getHierarchyId(), retailerCommission, bbpsTranactionRequestDto);
        }

        // 2. Traverse and distribute to parent hierarchies
        Retailer retailer = retailerRepository.findByRetailerId(bbpsTranactionRequestDto.getHierarchyId())
                .orElseThrow(() -> new RuntimeException("Retailer not found: " + bbpsTranactionRequestDto.getHierarchyId()));

        distributeCommissionToHierarchies(
                bbpsTranactionRequestDto,
                retailer.getParentAccount(),
                new HashSet<>()
        );

        // 3. Admin-level commission (dynamically fetched)
        if (bbpsTranactionRequestDto.getSchemeRate().getIm() > 0) {
            BigDecimal adminCommission = commonMethods.calculateCommissionAmount(
                    bbpsTranactionRequestDto.getSchemeRate().getIm(),
                    BigDecimal.valueOf(bbpsTranactionRequestDto.getTxnAmount()),
                    bbpsTranactionRequestDto.getSchemeRate().getRateType().toString()
            );

            commonMethods.getAdminHierarchyId(bbpsTranactionRequestDto.getHierarchyId()).ifPresent(adminId -> {
                distributeCommissionToHierarchy("AD", adminId, adminCommission, bbpsTranactionRequestDto);
            });
        }
    }

    private void distributeCommissionToHierarchies(BbpsTranactionRequestDto request, String hierarchyId, Set<String> processedMirrorSet) {

        if (hierarchyId == null || hierarchyId.isEmpty()) return;

        if (!processedMirrorSet.add(hierarchyId)) {
            log.info("Skipping duplicate mirror/commission for hierarchy: {}", hierarchyId);
            return; // already processed
        }

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        double rate = commonMethods.getCommissionRateByType(hierarchyType, request.getSchemeRate());
        BigDecimal commissionAmount = commonMethods.calculateCommissionAmount(rate, BigDecimal.valueOf(request.getTxnAmount()), request.getSchemeRate().getRateType().toString());

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("AD".equals(hierarchyType)) {
                log.info("Distributing commission directly to Admin: {}", hierarchyId);
                distributeCommissionToAdmin(hierarchyId, commissionAmount, request);
                return; // Admin reached, stop here
            }

            if ("WL".equals(hierarchyType)) {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Whitelabel not found: " + hierarchyId));

                if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                    log.info("Whitelabel {} isAdmin = true, full commission credited and stopping traversal", hierarchyId);
                    distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, request);
                    return; // Admin WL reached, stop here
                }
            }

            // All other hierarchy levels
            distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, request);
        }

        // Fix: Always continue traversal unless Admin is reached or Admin WL
        Optional<String> parentIdOpt = commonMethods.findParentHierarchy(hierarchyId);
        if (parentIdOpt.isPresent()) {
            String parentId = parentIdOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            // Only skip if Admin was already handled above
            if (!"AD".equals(parentType)) {
                distributeCommissionToHierarchies(request, parentId, processedMirrorSet);
            } else {
                // Admin found → still process it
                distributeCommissionToHierarchies(request, parentId, processedMirrorSet);
            }
        } else {
            log.warn("No parent found for hierarchyId: {}", hierarchyId);
        }
    }

    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
                                                 BigDecimal commissionAmount, BbpsTranactionRequestDto request) {

        log.info("Processing commission for {} | Hierarchy ID: {} | Amount (Incl. TDS): {} | Wallet Type: {} | TDS Rate: {}",
                hierarchyType, hierarchyId, commissionAmount, request.getWalletType(), request.getTds());

        String walletType = request.getWalletType();
        BigDecimal tdsRate = new BigDecimal(request.getTds());

        // === CASE 1: IM or AD — Credit only, no TDS or Mirror ===
        if ("IM".equals(hierarchyType) || "AD".equals(hierarchyType)) {
            log.info("{} hierarchy detected. Crediting full commission without TDS or mirror.", hierarchyType);

            String transactionTable = commonMethods.getTransactionTable(hierarchyType, walletType);
            String amountTable = commonMethods.getAmountTable(hierarchyType, walletType);

            BigDecimal beforeBalance = commonMethods.getCurrentBalance(hierarchyId, walletType);
            BigDecimal afterBalance = beforeBalance.add(commissionAmount);
            String txnId = buildTransactionId("COM", request.getTransactionId());

            insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                    "Commission credited (Direct IM/AD)", hierarchyId, walletType,
                    beforeBalance, afterBalance,
                    "Add Funds (Commission) - " + request.getWalletType().toUpperCase(), txnId, "SUCCESS", request);

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

                String transactionTable = commonMethods.getTransactionTable(hierarchyType, walletType);
                String amountTable = commonMethods.getAmountTable(hierarchyType, walletType);

                BigDecimal beforeBalance = commonMethods.getCurrentBalance(hierarchyId, walletType);
                BigDecimal afterBalance = beforeBalance.add(commissionAmount);
                String txnId = buildTransactionId("COM", request.getTransactionId());

                insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                        "Commission credited (Admin WL)", hierarchyId, walletType,
                        beforeBalance, afterBalance,
                        "Add Funds (Commission) - " + walletType.toUpperCase(), txnId, request.getVendorResponse(), request);

                updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

                return; // Stop further processing (no TDS or mirror)
            }
        }

        // === CASE 3: All other hierarchies — Credit, Mirror, TDS (if applicable) ===
        String transactionTable = commonMethods.getTransactionTable(hierarchyType, walletType);
        String amountTable = commonMethods.getAmountTable(hierarchyType, walletType);

        BigDecimal beforeBalance = commonMethods.getCurrentBalance(hierarchyId, walletType);
        BigDecimal afterCreditBalance = beforeBalance.add(commissionAmount);
        String comTxnId = buildTransactionId("COM", request.getTransactionId());

        // Credit commission
        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Commission credited", hierarchyId, walletType,
                beforeBalance, afterCreditBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, request.getVendorResponse(), request);

        updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);

        // Mirror commission (DEBIT from Admin/WL)
        if (!"pending".equalsIgnoreCase(request.getVendorResponse())) {
            mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "DEBIT", commissionAmount, walletType,
                    comTxnId, "Deduct Funds (Commission) - " + walletType.toUpperCase(), request.getVendorResponse(), request);
        }

        // TDS deduction (if applicable)
        BigDecimal tdsAmount = commissionAmount.multiply(tdsRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afterTdsBalance = afterCreditBalance.subtract(tdsAmount);
            String tdsTxnId = buildTransactionId("TDS", request.getTransactionId());

            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount,
                    "TDS Deduction", hierarchyId, walletType,
                    afterCreditBalance, afterTdsBalance,
                    "Deduct Funds (TDS) - " + walletType.toUpperCase(), tdsTxnId, request.getVendorResponse(), request);

            // Mirror TDS (CREDIT to Admin/WL)
            if (!"pending".equalsIgnoreCase(request.getVendorResponse())) {
                mirrorTransactionToWhitelabelOrAdmin(hierarchyId, hierarchyType, "CREDIT", tdsAmount, walletType,
                        tdsTxnId, "Add Funds (TDS) - " + walletType.toUpperCase(), request.getVendorResponse(), request);
            }
            updateAmountTable(hierarchyType, hierarchyId, amountTable, tdsAmount.negate());
        }
    }

    private void distributeCommissionToAdmin(String hierarchyId, BigDecimal commissionAmount, BbpsTranactionRequestDto request) {
        String currentId = hierarchyId;

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) return;

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            if ("WL".equals(parentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(parentId);
                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        // This is final Admin, credit full commission without TDS
                        creditCommissionToFinalAdmin(parentId, commissionAmount, request);
                        return;
                    }
                }
            }

            if ("IM".equals(parentType) || "AD".equals(parentType)) {
                // Final Admin - direct commission (not mirror) should also come here
                creditCommissionToFinalAdmin(parentId, commissionAmount, request);
                return;
            }

            currentId = parentId;
        }
    }

    private void creditCommissionToFinalAdmin(String adminId, BigDecimal commissionAmount, BbpsTranactionRequestDto request) {
        String walletType = request.getWalletType();

        log.info("Processing commission for Admin | Admin ID: {} | Amount: {} | Wallet Type: {} (No TDS Deduction)",
                adminId, commissionAmount, walletType);

        String tranxId = request.getTransactionId();
        BigDecimal beforeTransactionBalance = commonMethods.getCurrentBalance(adminId, walletType);
        BigDecimal afterTransactionBalance = beforeTransactionBalance.add(commissionAmount);

        String amountTable = commonMethods.getAmountTable("AD", walletType);
        String transactionTable = commonMethods.getTransactionTable("AD", walletType);
        String comTxnId = buildTransactionId("COM", tranxId);

        insertTransactionForAdminHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Admin Commission credited", adminId,
                beforeTransactionBalance, afterTransactionBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId, request);

        updateAmountTable("AD", adminId, amountTable, commissionAmount);
    }

    //This method is for commission credit entry
    private void insertTransactionForHierarchy(String transactionTable, String service, BigDecimal amount, String remarks,
                                               String hierarchyId, String walletType, BigDecimal beforeTransactionBalance,
                                               BigDecimal afterTransactionBalance, String actionOnBalance, String tranxId, String tranStatus, BbpsTranactionRequestDto request) {
        //GST entry for ADMIN 1st insertTrasnaction Method
        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {} | Action: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeTransactionBalance, afterTransactionBalance, actionOnBalance);

        String hierarchyColumnName = getHierarchyColumnName(commonMethods.extractHierarchyType(hierarchyId));
        String subProductName = request.getSubProductName();

        String sql = "INSERT INTO " + transactionTable +
                " (" + hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, sub_product_name) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalance, :finalBalance, :tranxId, :tranStatus, :subProductName)";

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
                .executeUpdate();
    }

    private void mirrorTransactionToWhitelabelOrAdmin(
            String sourceHierarchyId, String sourceHierarchyType, String txnType, BigDecimal amount, String walletType,
            String mirrorTxnId, String actionOnBalance, String tranStatus, BbpsTranactionRequestDto request) {
        String currentId = sourceHierarchyId;

        while (true) {
            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.warn("No parent found for: {}", currentId);
                return;
            }

            String parentId = parentOpt.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            // Defensive check
            if (parentType.equals("UNKNOWN")) {
                log.error("Invalid hierarchy type for ID: {}", parentId);
                return;
            }

            // Fetch current balance
            BigDecimal beforeBalance = commonMethods.getCurrentBalance(parentId, walletType);
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
                                commonMethods.getTransactionTable("WL", walletType),
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

                        updateAmountTable("WL", parentId, commonMethods.getAmountTable("WL", walletType),
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
                        commonMethods.getTransactionTable(parentType, walletType),
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

                updateAmountTable(parentType, parentId, commonMethods.getAmountTable(parentType, walletType),
                        afterBalance.subtract(beforeBalance));
                return;
            }

            // Continue upward
            currentId = parentId;
        }
    }

    public List<Map<String, Object>> getTransactions(String hierarchyId, String walletType, String baseTransactionId) {
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);

        String sql = "SELECT * FROM " + tableName +
                " WHERE transaction_id LIKE CONCAT('%', :baseTransactionId) " +
                " ORDER BY time_of_transaction ASC";

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("baseTransactionId", baseTransactionId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        // Get column names from ResultSet metadata instead of SessionFactory (simpler)
        List<Map<String, Object>> transactions = new ArrayList<>();
        List<String> columnNames = getColumnNames(tableName); // You'll need a helper

        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            for (int i = 0; i < columnNames.size(); i++) {
                map.put(columnNames.get(i), row[i]);
            }
            transactions.add(map);
        }
        return transactions;
    }

    private List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();

        String query = "SELECT * FROM " + tableName + " LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error fetching column names for table: " + tableName, e);
        }

        return columnNames;
    }


    @Transactional
    public void updateTransactionStatus(String retailerId, String walletType, String baseTransactionId) {
        // Step 1: Update retailer transactions
        updateHierarchyTransactionsStatus(retailerId, walletType, baseTransactionId);

        // Step 2: Find topmost parent and reverse their transactions
        Optional<String> currentParent = commonMethods.findParentHierarchy(retailerId);

        while (currentParent.isPresent()) {
            String parentId = currentParent.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            // Check for Admin
            if ("AD".equalsIgnoreCase(parentType)) {
                updateHierarchyTransactionsStatus(parentId, walletType, baseTransactionId);
                break;
            }

            // Check for WhiteLabel with isAdmin = true
            if ("WL".equalsIgnoreCase(parentType)) {
                boolean isAdminWL = whiteLabelsRepository.findById(parentId)
                        .map(wl -> wl.getIsAdmin() != null && wl.getIsAdmin())
                        .orElse(false);

                if (isAdminWL) {
                    updateHierarchyTransactionsStatus(parentId, walletType, baseTransactionId);
                    break;
                }
            }

            // Reverse parent transactions
            updateHierarchyTransactionsStatus(parentId, walletType, baseTransactionId);

            // Move one level up
            currentParent = commonMethods.findParentHierarchy(parentId);
        }
    }

    private void updateHierarchyTransactionsStatus(String hierarchyId, String walletType, String baseTransactionId) {
        // Step 1: Determine transaction table
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);

        // Step 2: Fetch transactions for this hierarchy and baseTransactionId
        List<Map<String, Object>> transactions = getTransactions(hierarchyId, walletType, baseTransactionId);

        for (Map<String, Object> txn : transactions) {
            String status = txn.get("tran_status") != null ? txn.get("tran_status").toString() : "";

            if ("PENDING".equalsIgnoreCase(status)) {
                String transactionId = txn.get("transaction_id").toString();

                // Step 3: Update status to SUCCESS
                String updateSql = "UPDATE " + tableName +
                        " SET tran_status = :txnStatus " +
                        "WHERE transaction_id = :transactionId";

                entityManager.createNativeQuery(updateSql)
                        .setParameter("txnStatus", "SUCCESS")
                        .setParameter("transactionId", transactionId)
                        .executeUpdate();
            }
        }
    }

    @Transactional
    public void reverseTransactionsAndUpdateWallet(String retailerId, String walletType, String baseTransactionId) {
        // Step 1: Reverse retailer transactions
        reverseSingleHierarchyTransactions(retailerId, walletType, baseTransactionId);

        // Step 2: Find topmost parent and reverse their transactions
        Optional<String> currentParent = commonMethods.findParentHierarchy(retailerId);

        while (currentParent.isPresent()) {
            String parentId = currentParent.get();
            String parentType = commonMethods.extractHierarchyType(parentId);

            // Check for Admin
            if ("AD".equalsIgnoreCase(parentType)) {
                reverseAdminOrWLAdminTransactions(parentId, walletType, baseTransactionId);
                break;
            }

            // Check for WhiteLabel with isAdmin = true
            if ("WL".equalsIgnoreCase(parentType)) {
                boolean isAdminWL = whiteLabelsRepository.findById(parentId)
                        .map(wl -> wl.getIsAdmin() != null && wl.getIsAdmin())
                        .orElse(false);

                if (isAdminWL) {
                    reverseAdminOrWLAdminTransactions(parentId, walletType, baseTransactionId);
                    break;
                }
            }

            // Reverse parent transactions
            reverseSingleHierarchyTransactions(parentId, walletType, baseTransactionId);

            // Move one level up
            currentParent = commonMethods.findParentHierarchy(parentId);
        }
    }


    private void reverseSingleHierarchyTransactions(String hierarchyId, String walletType, String baseTransactionId) {
        // Step 1: Determine transaction table
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);

        // Step 2: Fetch transactions
        List<Map<String, Object>> transactions = getTransactions(hierarchyId, walletType, baseTransactionId);

        for (Map<String, Object> txn : transactions) {
            String originalTxnId = txn.get("transaction_id").toString();
            BigDecimal amount = new BigDecimal(txn.get("amount").toString());

            String service = txn.get("service") != null ? txn.get("service").toString() : "";
            String remarks = txn.get("remarks") != null ? txn.get("remarks").toString() : "";
            String operatorName = txn.get("operator_name") != null ? txn.get("operator_name").toString() : "";
            String subProductName = txn.get("sub_product_name") != null ? txn.get("sub_product_name").toString() : "";
            String actionOnBalanceAmount = txn.get("action_on_balance_amount") != null
                    ? txn.get("action_on_balance_amount").toString()
                    : "";

            // Step 3: Reverse type
            String reversedTxnId = "REV" + originalTxnId;
            String reversedType = service.equalsIgnoreCase("Credit") ? "Debit" : "Credit";

            // Step 4: Reverse "action_on_balance_amount"
            String reversedActionOnBalance;
            if (actionOnBalanceAmount.toLowerCase().contains("deduct")) {
                reversedActionOnBalance = actionOnBalanceAmount.replaceFirst("(?i)Deduct", "Add");
            } else if (actionOnBalanceAmount.toLowerCase().contains("add")) {
                reversedActionOnBalance = actionOnBalanceAmount.replaceFirst("(?i)Add", "Deduct");
            } else {
                reversedActionOnBalance = actionOnBalanceAmount;
            }

            // Step 5: Wallet update
            AbstractWalletAmountTable wallet = commonMethods.getWalletByType(walletType, hierarchyId);
            BigDecimal walletBalBefore = wallet.getAvailableBalance();
            BigDecimal walletBalAfter;

            if (reversedType.equalsIgnoreCase("Credit")) {
                walletBalAfter = walletBalBefore.add(amount);
                wallet.setAvailableBalance(walletBalAfter);
            } else {
                walletBalAfter = walletBalBefore.subtract(amount);
                wallet.setAvailableBalance(walletBalAfter);
            }

            String hierarchyColumnName = getHierarchyColumnName(hierarchyType);

            // Step 6: Insert reversal
            String insertSql = "INSERT INTO " + tableName + " (" +
                    hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                    "bal_before_tran, bal_after_tran, final_balance, transaction_type, transaction_id, tran_status, " +
                    "operator_name, sub_product_name, action_on_balance_amount) " +
                    "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                    ":balBefore, :balAfter, :balAfter, :txnType, :txnId, :txnStatus, :operatorName, :subProductName, :actionOnBalance)";

            entityManager.createNativeQuery(insertSql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("remarks", "Reversal of " + originalTxnId + " | " + remarks)
                    .setParameter("dateOfTransaction", new java.sql.Date(System.currentTimeMillis()))
                    .setParameter("timeOfTransaction", new java.sql.Time(System.currentTimeMillis()))
                    .setParameter("balBefore", walletBalBefore)
                    .setParameter("balAfter", walletBalAfter)
                    .setParameter("txnType", reversedType)
                    .setParameter("txnId", reversedTxnId)
                    .setParameter("txnStatus", "FAILURE")
                    .setParameter("operatorName", operatorName)
                    .setParameter("subProductName", subProductName)
                    .setParameter("actionOnBalance", reversedActionOnBalance)
                    .executeUpdate();

            // Step 7: Save wallet changes
            entityManager.merge(wallet);
        }
    }


    private void reverseAdminOrWLAdminTransactions(String hierarchyId, String walletType, String baseTransactionId) {
        // Step 1: Determine transaction table
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);

        // Step 2: Fetch transactions
        List<Map<String, Object>> transactions = getTransactions(hierarchyId, walletType, baseTransactionId);

        for (Map<String, Object> txn : transactions) {
            String originalTxnId = txn.get("transaction_id").toString();
            BigDecimal amount = new BigDecimal(txn.get("amount").toString());

            String service = txn.get("service") != null ? txn.get("service").toString() : "";
            String remarks = txn.get("remarks") != null ? txn.get("remarks").toString() : "";
            String operatorName = txn.get("operator_name") != null ? txn.get("operator_name").toString() : "";
            String subProductName = txn.get("sub_product_name") != null ? txn.get("sub_product_name").toString() : "";
            String actionOnBalanceAmount = txn.get("action_on_balance_amount") != null
                    ? txn.get("action_on_balance_amount").toString()
                    : "";

            // Step 3: Reverse type
            String reversedTxnId = "REV" + originalTxnId;
            String reversedType = service.equalsIgnoreCase("Credit") ? "Debit" : "Credit";

            // Step 4: Reverse "action_on_balance_amount"
            String reversedActionOnBalance;
            if (actionOnBalanceAmount.toLowerCase().contains("deduct")) {
                reversedActionOnBalance = actionOnBalanceAmount.replaceFirst("(?i)Deduct", "Add");
            } else if (actionOnBalanceAmount.toLowerCase().contains("add")) {
                reversedActionOnBalance = actionOnBalanceAmount.replaceFirst("(?i)Add", "Deduct");
            } else {
                reversedActionOnBalance = actionOnBalanceAmount;
            }

            // Step 5: Wallet update
            AbstractWalletAmountTable wallet = commonMethods.getWalletByType(walletType, hierarchyId);
            BigDecimal walletBalBefore = wallet.getAvailableBalance();
            BigDecimal walletBalAfter;

            if (reversedType.equalsIgnoreCase("Credit")) {
                walletBalAfter = walletBalBefore.add(amount);
                wallet.setAvailableBalance(walletBalAfter);
            } else {
                walletBalAfter = walletBalBefore.subtract(amount);
                wallet.setAvailableBalance(walletBalAfter);
            }

            String hierarchyColumnName = getHierarchyColumnName(hierarchyType);

            // Step 6: Insert reversal
            String insertSql = "INSERT INTO " + tableName + " (" +
                    hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                    "bal_before_tran, bal_after_tran, final_balance, transaction_id, tran_status, " +
                    "operator_name, sub_product_name, action_on_balance_amount) " +
                    "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                    ":balBefore, :balAfter, :balAfter, :txnId, :txnStatus, :operatorName, :subProductName, :actionOnBalance)";

            entityManager.createNativeQuery(insertSql)
                    .setParameter("hierarchyId", hierarchyId)
                    .setParameter("service", service)
                    .setParameter("amount", amount)
                    .setParameter("remarks", "Reversal of " + originalTxnId + " | " + remarks)
                    .setParameter("dateOfTransaction", new java.sql.Date(System.currentTimeMillis()))
                    .setParameter("timeOfTransaction", new java.sql.Time(System.currentTimeMillis()))
                    .setParameter("balBefore", walletBalBefore)
                    .setParameter("balAfter", walletBalAfter)
                    .setParameter("txnId", reversedTxnId)
                    .setParameter("txnStatus", "FAILURE")
                    .setParameter("operatorName", operatorName)
                    .setParameter("subProductName", subProductName)
                    .setParameter("actionOnBalance", reversedActionOnBalance)
                    .executeUpdate();

            // Step 7: Save wallet changes
            entityManager.merge(wallet);
        }
    }




}
