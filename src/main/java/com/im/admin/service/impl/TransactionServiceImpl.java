package com.im.admin.service.impl;

import com.im.admin.dto.AbstractRetailerAmount;
import com.im.admin.dto.CardTypeRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsResponseDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.SlabType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.service.TransactionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.NoResultException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

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
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private WhiteLabelsRepository whiteLabelsRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private RetailerPostpaidAmountRepository retailerPostpaidAmountRepository;

    @Autowired
    private RetailerPrepaidAmountRepository retailerPrepaidAmountRepository;

    @Autowired
    private RetailerPostpaidTransactionDetailsRepository retailerPostpaidTransactionDetailsRepository;

    @Autowired
    private RetailerPrepaidTransactionDetailsRepository retailerPrepaidTransactionDetailsRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getOperatorDetailsByCardBIN(CardTypeRequestDto cardTypeRequestDto) {
        log.info("Fetching operator details for BIN: {}", cardTypeRequestDto.getCardBIN_No());

        return operatorsRepository.findByCardBinNo(cardTypeRequestDto.getCardBIN_No())
                .map(operator -> {
                    log.info("Operator found - Name: {}, Code: {}", operator.getOperatorName(), operator.getOperatorCode());
                    return "Operator Name: " + operator.getOperatorName() + ", Operator Code: " + operator.getOperatorCode();
                })
                .orElseGet(() -> {
                    log.warn("No operator found for BIN No: {}", cardTypeRequestDto.getCardBIN_No());
                    return "No operator found for BIN No: " + cardTypeRequestDto.getCardBIN_No();
                });
    }


    @Override
    public TransactionSchemeDetailsResponseDto getTransactionSchemeDetails(TransactionSchemeDetailsRequestDto request) {
        log.info("Fetching scheme details for hierarchy ID: {}, Operator: {}, Code: {}",
                request.getHierarchyId(), request.getOperatorName(), request.getOperatorCode());

        validateAmount(request);

        List<SchemeAllocation> allocations = schemeAllocationRepository.findByHierarchyId(request.getHierarchyId());
        validateSchemeAllocation(request, allocations);

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
    public String processTransaction(TransactionSchemeDetailsRequestDto dto) {

        String baseTransactionId = generateBaseTransactionId(); // One base ID for full flow

        String vendorResponse = "failure";
        String retailerId = dto.getHierarchyId();
        BigDecimal tranxAmount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(dto.getSchemeRate().getGross());
        String walletType = dto.getWalletType().toUpperCase();
        String taxValue = dto.getGstValue();
        String tranxType = dto.getTransactionType();

        AbstractRetailerAmount wallet;

        if ("PREPAID".equalsIgnoreCase(walletType)) {
            wallet = retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Prepaid wallet not found: " + retailerId));
        } else if ("POSTPAID".equalsIgnoreCase(walletType)) {
            wallet = retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                    .orElseThrow(() -> new RuntimeException("Postpaid wallet not found: " + retailerId));
        } else {
            throw new IllegalArgumentException("Invalid wallet type: " + walletType);
        }

        if (tranxType.equalsIgnoreCase("credit")) {
            // 1. Vendor call before credit
           vendorResponse = "Success"; // Replace with actual call

            if (!vendorResponse.equalsIgnoreCase("Success")) {
                insertTransaction(wallet, "CREDIT_FAILED", tranxAmount,
                        wallet.getAvailableBalance(), wallet.getAvailableBalance(),
                        "Vendor Failure - No Credit", buildTransactionId("FLD", baseTransactionId));
                return "Vendor transaction failed";
            }

            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId);
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId);

        } else {
            // 1. Check balance
            if (!hasSufficientBalance(retailerId, tranxAmount, grossAmount, walletType)) {
                return "Insufficient balance";
            }

            updateWallet(dto, wallet, tranxAmount, tranxType, baseTransactionId);

            // 2. Vendor call
            vendorResponse = "Success"; // Replace with actual call
            if (!vendorResponse.equalsIgnoreCase("Success")) {
                // REVERSAL: Refund amount and insert reversal transaction
                BigDecimal beforeReversal = wallet.getAvailableBalance();
                wallet.setAvailableBalance(beforeReversal.add(tranxAmount));
                BigDecimal afterReversal = wallet.getAvailableBalance();

                insertTransaction(wallet, "REVERSAL", tranxAmount, beforeReversal, afterReversal,
                        "Vendor Failed - Debit Reversed", buildTransactionId("REV", baseTransactionId));
                return "Vendor transaction failed";
            }

            processChargesAndCommission(dto, wallet, grossAmount, taxValue, baseTransactionId);
        }

        // Save wallet changes
        if (wallet instanceof RetailerPrepaidAmount) {
            retailerPrepaidAmountRepository.save((RetailerPrepaidAmount) wallet);
        } else {
            retailerPostpaidAmountRepository.save((RetailerPostpaidAmount) wallet);
        }

        return "Transaction Successful";
    }

    private void processChargesAndCommission(TransactionSchemeDetailsRequestDto dto, AbstractRetailerAmount wallet,
                                             BigDecimal grossAmount, String gstValue, String baseTransactionId) {
        if (dto.getHasCharge()) {
            processCharges(dto, wallet, grossAmount, gstValue, baseTransactionId); // GST only
        }

        if (dto.getIsCommission()) {
            processCommission(dto, baseTransactionId); // TDS only
        }
    }

    private void processCharges(TransactionSchemeDetailsRequestDto dto, AbstractRetailerAmount wallet,
                                BigDecimal grossAmountInput, String gstValue, String baseTransactionId) {

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

        insertTransaction(wallet, "DEBIT", baseAmount, beforeBase, afterBase,
                "Deduct Funds (Gross) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("CHG", baseTransactionId));

        // Step 3: Deduct GST
        BigDecimal beforeGst = wallet.getAvailableBalance();
        wallet.setAvailableBalance(beforeGst.subtract(gstAmount));
        BigDecimal afterGst = wallet.getAvailableBalance();

        insertTransaction(wallet, "DEBIT", gstAmount, beforeGst, afterGst,
                "Deduct Funds (GST) - " + dto.getWalletType().toUpperCase(),
                buildTransactionId("GST", baseTransactionId));

        // Step 4: Update wallet meta
        wallet.setUsedCash(wallet.getUsedCash().add(baseAmount.add(gstAmount)));
        wallet.setLastUpdatedAmount(gstAmount);
        wallet.setLastUpdateDate(LocalDate.now());
        wallet.setLastUpdateTime(LocalTime.now());

        // Step 5: Credit GST to Admin
        creditGstToAdmin(dto.getHierarchyId(), dto.getWalletType(), gstAmount, baseTransactionId);
    }


    private void creditGstToAdmin(String sourceHierarchyId, String walletType, BigDecimal gstAmount, String baseTransactionId) {

        String adminHierarchyId = getTopLevelAdminIdFromHierarchy(sourceHierarchyId); // fetch Admin ID from hierarchy
        String transactionTable = getTransactionTable("AD", walletType);
        String amountTable = getAmountTable("AD", walletType);

        // Step 1: Get Admin's balance
        BigDecimal beforeBalance = getCurrentBalance(adminHierarchyId, walletType);
        BigDecimal afterBalance = beforeBalance.add(gstAmount);

        String gstCreditTranxId = buildTransactionId("GST", baseTransactionId);

        // Step 2: Insert GST credit transaction for Admin
        insertTransactionForAdminHierarchy(
                transactionTable, "CREDIT", gstAmount, "GST collected from: " + sourceHierarchyId,
                sourceHierarchyId, walletType, beforeBalance, afterBalance,
                "Add Funds (GST) - " + walletType.toUpperCase(), gstCreditTranxId
        );

        // Step 3: Update Admin's wallet balance
        updateAmountTable("AD", adminHierarchyId, amountTable, gstAmount);
    }

    private void processCommission(TransactionSchemeDetailsRequestDto dto, String baseTransactionId) {
        BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());

        // Commission to Retailer
        if (dto.getSchemeRate().getRetailer() > 0) {
            BigDecimal retailerCommission = calculateCommissionAmount(
                    dto.getSchemeRate().getRetailer(), BigDecimal.valueOf(dto.getAmount()), dto.getSchemeRate().getRateType().toString()
            );
            distributeCommissionToHierarchy("RT", dto.getHierarchyId(), retailerCommission, dto.getWalletType(), tdsRate, baseTransactionId);
        }

        // Upline hierarchy commission
        Retailer retailer = retailerRepository.findByRetailerId(dto.getHierarchyId())
                .orElseThrow(() -> new RuntimeException("Retailer not found: " + dto.getHierarchyId()));

        distributeCommissionToHierarchies(BigDecimal.valueOf(dto.getAmount()), retailer.getParentAccount(), dto.getSchemeRate(), dto.getWalletType(), tdsRate, baseTransactionId);
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

    private void distributeCommissionToHierarchies(BigDecimal amount, String hierarchyId, SchemeRate schemeRate, String walletType, BigDecimal tdsRate, String baseTransactionId) {
        if (hierarchyId == null || hierarchyId.isEmpty()) {
            return;
        }

        String hierarchyType = extractHierarchyType(hierarchyId);
        double rate = getCommissionRateByType(hierarchyType, schemeRate);
        BigDecimal commissionAmount = calculateCommissionAmount(rate, amount, schemeRate.getRateType().toString());

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("AD".equals(hierarchyType)) {
                distributeCommissionToAdmin(hierarchyId, commissionAmount, walletType, baseTransactionId);
                return;
            } else {
                distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId);
            }
        }

        findParentHierarchy(hierarchyId).ifPresent(parentId ->
                distributeCommissionToHierarchies(amount, parentId, schemeRate, walletType, tdsRate, baseTransactionId)
        );
    }

    private void creditTdsToAdmin(String sourceHierarchyId, String walletType, BigDecimal tdsAmount, String baseTransactionId) {
        String adminHierarchyId = getTopLevelAdminIdFromHierarchy(sourceHierarchyId);
        String transactionTable = getTransactionTable("AD", walletType);
        String amountTable = getAmountTable("AD", walletType);

        // Step 1: Get Admin's current balance
        BigDecimal beforeBalance = getCurrentBalance(adminHierarchyId, walletType);
        BigDecimal afterBalance = beforeBalance.add(tdsAmount);

        String tdsCreditTranxId = buildTransactionId("TDS", baseTransactionId);

        // Step 2: Insert credit transaction for Admin
        insertTransactionForAdminHierarchy(
                transactionTable, "CREDIT", tdsAmount, "TDS collected from: " + sourceHierarchyId,
                sourceHierarchyId, walletType, beforeBalance, afterBalance,"Add Funds (TDS) - " + walletType.toUpperCase(), tdsCreditTranxId
        );

        // Step 3: Update Admin's wallet balance
        updateAmountTable("AD", adminHierarchyId, amountTable, tdsAmount);
    }

    //
    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
                                                 BigDecimal commissionAmount, String walletType,
                                                 BigDecimal tdsRate, String baseTransactionId) {

        log.info("Processing commission for {} | Hierarchy ID: {} | Amount (Incl. TDS): {} | Wallet Type: {} | TDS Rate: {}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        // Step 1: Get current balance
        BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);

        // Step 2: Credit full commission amount
        BigDecimal afterCreditBalance = beforeBalance.add(commissionAmount);
        String comTxnId = buildTransactionId("COM", baseTransactionId);

        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount, "Commission credited",
                hierarchyId, walletType, beforeBalance, afterCreditBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId);

        // Step 3: Calculate TDS (from full commission)
        BigDecimal tdsAmount = commissionAmount.multiply(tdsRate)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        boolean isTdsApplicable = !(hierarchyType.equals("IM") || hierarchyType.equals("AD"));

        if (isTdsApplicable && tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Step 4: Deduct TDS from wallet
            BigDecimal afterTdsBalance = afterCreditBalance.subtract(tdsAmount);
            String tdsTxnId = buildTransactionId("TDS", baseTransactionId);

            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount, "TDS Deduction",
                    hierarchyId, walletType, afterCreditBalance, afterTdsBalance,
                    "Deduct Funds (TDS) - " + walletType.toUpperCase(), tdsTxnId);

            // Step 5: Credit TDS to Admin
            creditTdsToAdmin(hierarchyId, walletType, tdsAmount, baseTransactionId);

            // Step 6: Update amount table with net increase
            updateAmountTable(hierarchyType, hierarchyId, amountTable, afterTdsBalance.subtract(beforeBalance));

        } else {
            // No TDS applicable
            updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount);
        }
    }


    private String getTopLevelAdminIdFromHierarchy(String hierarchyId) {
        String currentId = hierarchyId;

        while (currentId != null && !extractHierarchyType(currentId).equals("AD")) {
            Optional<String> parentOpt = findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;
            currentId = parentOpt.get();
        }

        return currentId; // Will return the Admin ID
    }


    private void distributeCommissionToAdmin(String adminId, BigDecimal commissionAmount, String walletType, String baseTransactionId) {
        log.info("Processing commission for Admin | Admin ID: {} | Amount: {} | Wallet Type: {} (No TDS Deduction)",
                adminId, commissionAmount, walletType);
        BigDecimal beforeTransactionBalance = getCurrentBalance(adminId, walletType);
        BigDecimal afterTransactionBalance = beforeTransactionBalance.add(commissionAmount);
        String amountTable = getAmountTable("AD", walletType);
        String transactionTable = getTransactionTable("AD", walletType);

        String comTxnId = buildTransactionId("COM", baseTransactionId);
        insertTransactionForAdminHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Admin Commission credited", adminId, walletType, beforeTransactionBalance, afterTransactionBalance, "Add Funds (Commission) - "+ walletType.toUpperCase(), comTxnId);
        updateAmountTable("AD", adminId, amountTable, commissionAmount);
    }


    private String extractHierarchyType(String hierarchyId) {
        String[] parts = hierarchyId.split("-");
        return (parts.length >= 2) ? parts[1] : "UNKNOWN";
    }

    private BigDecimal getCommissionAmount(String hierarchyType, SchemeRate schemeRate) {
        return switch (hierarchyType) {
            case "AD" -> BigDecimal.valueOf(schemeRate.getIm());
            case "ET" -> BigDecimal.valueOf(schemeRate.getEnterprise());
            case "WL" -> BigDecimal.valueOf(schemeRate.getWhitelabel());
            case "SD" -> BigDecimal.valueOf(schemeRate.getSuperDistributor());
            case "PT" -> BigDecimal.valueOf(schemeRate.getPartner());
            case "DT" -> BigDecimal.valueOf(schemeRate.getDistributor());
            case "RT" -> BigDecimal.valueOf(schemeRate.getRetailer());
            default -> BigDecimal.ZERO;
        };
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
    private void insertTransactionForAdminHierarchy(String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId, String walletType, BigDecimal beforeBalance,
                                               BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId) {
        //BigDecimal beforeTransactionBalance = getCurrentBalance(hierarchyId, walletType);
        //BigDecimal afterTransactionBalance = service.equals("CREDIT") ? beforeTransactionBalance.add(amount) : beforeTransactionBalance.subtract(amount);

        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeBalance, afterBalance);

        //String columnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "INSERT INTO " + transactionTable +
                " (hierarchy_id,service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId)";

        entityManager.createNativeQuery(sql)
                .setParameter("hierarchyId", hierarchyId)
                .setParameter("service", service)
                .setParameter("amount", amount)
                .setParameter("remarks", remarks)
                .setParameter("dateOfTransaction", new Date())
                .setParameter("timeOfTransaction", new Time(System.currentTimeMillis()))
                .setParameter("beforeTransactionBalance", beforeBalance)
                .setParameter("afterTransactionBalance", afterBalance)
                .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                .setParameter("finalBalance", afterBalance)
                .setParameter("tranxId", tranxId)
                .executeUpdate();
    }

    //This method is for commission credit entry
    private void insertTransactionForHierarchy(String transactionTable, String service, BigDecimal amount, String remarks,
                                               String hierarchyId, String walletType, BigDecimal beforeTransactionBalance,
                                               BigDecimal afterTransactionBalance, String actionOnBalance, String tranxId) {
        //GST entry for ADMIN 1st insertTrasnaction Method
        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {} | Action: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeTransactionBalance, afterTransactionBalance, actionOnBalance);

        String hierarchyColumnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "INSERT INTO " + transactionTable +
                " ("+ hierarchyColumnName +", service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id) " +
                "VALUES (:hierarchyId, :service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalance, :finalBalance, :tranxId)";

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

    private <T extends AbstractRetailerAmount> void updateWallet(TransactionSchemeDetailsRequestDto dto, T wallet, BigDecimal requestedAmount,
                                                                 String tranxType, String baseTransactionId) {
        String actionOnBalance = "";

        // *** Credit Operation ***
        BigDecimal balanceBeforeCredit = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(requestedAmount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(requestedAmount));
            actionOnBalance = "Add Funds - " + dto.getWalletType().toUpperCase();

        } else {
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(requestedAmount));
            wallet.setTotalCash(wallet.getTotalCash().subtract(requestedAmount));
            actionOnBalance = "Deduct Funds - " + dto.getWalletType().toUpperCase();
        }

        wallet.setLastUpdatedAmount(requestedAmount);
        wallet.setLastUpdateDate(LocalDate.now());
        wallet.setLastUpdateTime(LocalTime.now());

        // Insert Credit transaction
        insertTransaction(wallet, tranxType, requestedAmount, balanceBeforeCredit,
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId);


    }

    private void insertTransaction(AbstractRetailerAmount wallet, String transactionType, BigDecimal amount,
                                   BigDecimal balanceBeforeTranx, BigDecimal balanceAfterTranx, String actionOnBalance, String tranxId) {

        if (wallet instanceof RetailerPrepaidAmount) {
            RetailerPrepaidTransactionDetails transaction = new RetailerPrepaidTransactionDetails();
            transaction.setTransactionId(tranxId);
            transaction.setDateOfTransaction(LocalDate.now());
            transaction.setTimeOfTransaction(LocalTime.now());
            transaction.setService("Retailer Wallet");
            transaction.setBalBeforeTran(balanceBeforeTranx);
            transaction.setAmount(amount);
            transaction.setBalAfterTran(balanceAfterTranx);
            transaction.setActionOnBalanceAmount(actionOnBalance);
            transaction.setTranStatus(transactionType);
            transaction.setFinalBalance(balanceAfterTranx);
            transaction.setRetailerId(((RetailerPrepaidAmount) wallet).getRetailerId().getRetailerId());
            retailerPrepaidTransactionDetailsRepository.save(transaction);
        } else if (wallet instanceof RetailerPostpaidAmount) {
            RetailerPostpaidTransactionDetails transaction = new RetailerPostpaidTransactionDetails();
            transaction.setTransactionId(tranxId);
            transaction.setDateOfTransaction(LocalDate.now());
            transaction.setTimeOfTransaction(LocalTime.now());
            transaction.setService("Retailer Wallet");
            transaction.setBalBeforeTran(balanceBeforeTranx);
            transaction.setActionOnBalanceAmount(actionOnBalance);
            transaction.setAmount(amount);
            transaction.setBalAfterTran(balanceAfterTranx);
            transaction.setTranStatus(transactionType);
            transaction.setFinalBalance(balanceAfterTranx);
            transaction.setRetailerId(((RetailerPostpaidAmount) wallet).getRetailerId().getRetailerId());
            retailerPostpaidTransactionDetailsRepository.save(transaction);
        }
    }

    private void validateAmount(TransactionSchemeDetailsRequestDto request) {
        if (!Objects.equals(request.getAmount(), request.getConfirmAmount())) {
            log.error("Amount mismatch! Transaction failed. Amount: {}, ConfirmAmount: {}", request.getAmount(), request.getConfirmAmount());
            throw new RuntimeException("Amount mismatch! Transaction failed.");
        }
    }

    private void validateSchemeAllocation(TransactionSchemeDetailsRequestDto request, List<SchemeAllocation> allocations) {
        if (allocations.isEmpty()) {
            log.warn("No scheme allocations found for hierarchy: {}", request.getHierarchyId());
            throw new RuntimeException("No scheme allocations found for hierarchy: " + request.getHierarchyId());
        }
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
                .filter(rate -> rate.getSlabType() == SlabType.FIXED
                        ? request.getAmount().equals(rate.getFixedSlabValue())
                        : request.getAmount() >= rate.getSlabStart() && request.getAmount() <= rate.getSlabEnd())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No matching Scheme Rate found."));
    }

    private String generateBaseTransactionId() {
        String timestamp = new SimpleDateFormat("yyMMddHHmmss").format(new Date());
        int autoIncrement = getNextTransactionSequence(); // Get from DB or memory (e.g., Redis)
        return timestamp + String.format("%05d", autoIncrement); // padded to ensure sorting
    }

    private String buildTransactionId(String prefix, String baseId) {
        return prefix + baseId;
    }

    private int getNextTransactionSequence() {
        jdbcTemplate.update("INSERT INTO transaction_sequence () VALUES ()");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);
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


}
