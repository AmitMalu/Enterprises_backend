package com.im.admin.service.impl;

import com.im.admin.dto.AbstractRetailerAmount;
import com.im.admin.dto.CardTypeRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.TransactionSchemeDetailsResponseDto;
import com.im.admin.entities.*;
import com.im.admin.entities.enums.HierarchyType;
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
        String vendorResponse = "failure";
        String retailerId = dto.getHierarchyId();
        BigDecimal tranxAmount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(dto.getSchemeRate().getGross());
        String walletType = dto.getWalletType().toUpperCase();
        String taxValue = dto.getGstValue();
        String tranxType = dto.getTransactionType();

        Date transactionDate = new Date();
        Time transactionTime = new Time(System.currentTimeMillis());

        String baseTransactionId = generateBaseTransactionId(); // One base ID for full flow

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
                return "Vendor transaction failed";
            }

            updateWallet(dto, wallet, tranxAmount, grossAmount, taxValue, tranxType, baseTransactionId);
            processChargesAndCommission(dto, wallet, grossAmount, taxValue, transactionDate, transactionTime, baseTransactionId);

        } else {
            // 1. Check balance
            if (!hasSufficientBalance(retailerId, tranxAmount, walletType)) {
                return "Insufficient balance";
            }

            updateWallet(dto, wallet, tranxAmount, grossAmount, taxValue, tranxType, baseTransactionId);

            // 2. Vendor call
            vendorResponse = "Success"; // Replace with actual call
            if (!vendorResponse.equalsIgnoreCase("Success")) {
                return "Vendor transaction failed"; // optionally reverse wallet here
            }

            processChargesAndCommission(dto, wallet, grossAmount, taxValue, transactionDate, transactionTime, baseTransactionId);
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
                                             BigDecimal grossAmount, String gstValue, Date transactionDate, Time transactionTime, String baseTransactionId) {
        if (dto.getHasCharge()) {
            processCharges(dto, wallet, grossAmount, gstValue, transactionDate, transactionTime, baseTransactionId); // GST only
        }

        if (dto.getIsCommission()) {
            processCommission(dto, baseTransactionId); // TDS only
        }
    }

    private void processCharges(TransactionSchemeDetailsRequestDto dto, AbstractRetailerAmount wallet,
                                BigDecimal grossAmount, String gstValue, Date transactionDate, Time transactionTime, String baseTransactionId) {

        // Deduct Gross Amount (amount + GST inclusive)
        BigDecimal balanceBeforeGross = wallet.getAvailableBalance();
        wallet.setAvailableBalance(balanceBeforeGross.subtract(grossAmount));
        BigDecimal balanceAfterGross = wallet.getAvailableBalance();

        String chgId = buildTransactionId("CHG", baseTransactionId);
        insertTransaction(wallet, "DEBIT", grossAmount, balanceBeforeGross, balanceAfterGross,
                transactionDate, transactionTime, "Deduct Funds (Gross) - " + dto.getWalletType().toUpperCase(), chgId);

        // Extract GST amount from gross
        BigDecimal gstRate = new BigDecimal(gstValue).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.ONE.add(gstRate);
        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);
        BigDecimal gstAmount = baseAmount.multiply(gstRate).setScale(4, RoundingMode.HALF_UP);

        // Track GST separately if needed for reporting
        BigDecimal balanceBeforeGst = wallet.getAvailableBalance();
        wallet.setAvailableBalance(balanceBeforeGst.subtract(gstAmount));
        BigDecimal balanceAfterGst = wallet.getAvailableBalance();

        wallet.setUsedCash(wallet.getUsedCash().add(grossAmount.add(gstAmount)));
        wallet.setLastUpdatedAmount(gstAmount);
        wallet.setLastUpdateDate(transactionDate);
        wallet.setLastUpdateTime(transactionTime);

        String gstId = buildTransactionId("GST", baseTransactionId);
        insertTransaction(wallet, "DEBIT", gstAmount, balanceBeforeGst, balanceAfterGst,
                transactionDate, transactionTime, "Deduct Funds (GST) - " + dto.getWalletType().toUpperCase(), gstId);
    }

    private void processCommission(TransactionSchemeDetailsRequestDto dto, String baseTransactionId) {
        BigDecimal tdsRate = new BigDecimal(dto.getTdsRate());

        // Commission to Retailer
        if (dto.getSchemeRate().getRetailer() > 0) {
            BigDecimal retailerCommission = BigDecimal.valueOf(dto.getSchemeRate().getRetailer());
            distributeCommissionToHierarchy("RT", dto.getHierarchyId(), retailerCommission, dto.getWalletType(), tdsRate, baseTransactionId);
        }

        // Upline hierarchy commission
        Retailer retailer = retailerRepository.findByRetailerId(dto.getHierarchyId())
                .orElseThrow(() -> new RuntimeException("Retailer not found: " + dto.getHierarchyId()));

        distributeCommissionToHierarchies(retailer.getParentAccount(), dto.getSchemeRate(), dto.getWalletType(), tdsRate, baseTransactionId);
    }

    public boolean hasSufficientBalance(String retailerId, BigDecimal requestedAmount, String walletType) {
        String amountTable = getAmountTable("RT", walletType);

        String sql = "SELECT available_balance FROM " + amountTable + " WHERE retailer_id = :retailerId";

        BigDecimal availableBalance = (BigDecimal) entityManager.createNativeQuery(sql)
                .setParameter("retailerId", retailerId)
                .getSingleResult();

        log.info("Retailer ID: {} | Available Balance: {} | Requested Amount: {}", retailerId, availableBalance, requestedAmount);

        return availableBalance != null && availableBalance.compareTo(requestedAmount) >= 0;
    }

    private void distributeCommissionToHierarchies(String hierarchyId, SchemeRate schemeRate, String walletType, BigDecimal tdsRate, String baseTransactionId) {
        if (hierarchyId == null || hierarchyId.isEmpty()) {
            return;
        }

        String hierarchyType = extractHierarchyType(hierarchyId);
        BigDecimal commissionAmount = getCommissionAmount(hierarchyType, schemeRate);

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("AD".equals(hierarchyType)) {
                distributeCommissionToAdmin(hierarchyId, commissionAmount, walletType, baseTransactionId);
                return;
            } else {
                distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate, baseTransactionId);
            }
        }

        findParentHierarchy(hierarchyId).ifPresent(parentId ->
                distributeCommissionToHierarchies(parentId, schemeRate, walletType, tdsRate, baseTransactionId)
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
        insertTransactionForHierarchy(
                transactionTable, "CREDIT", tdsAmount, "TDS collected from: " + sourceHierarchyId,
                adminHierarchyId, walletType, beforeBalance, afterBalance,"Add Funds (TDS) - " + walletType.toUpperCase(), tdsCreditTranxId
        );

        // Step 3: Update Admin's wallet balance
        updateAmountTable("AD", adminHierarchyId, amountTable, tdsAmount);
    }

    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId,
                                                 BigDecimal commissionAmount, String walletType, BigDecimal tdsRate, String baseTransactionId) {
        log.info("Processing commission for {} | Hierarchy ID: {} | Amount: {} | Wallet Type: {} | TDS Rate: {}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        // Step 1: Get Current Balance
        BigDecimal beforeBalance = getCurrentBalance(hierarchyId, walletType);

        // Step 2: Credit Commission
        BigDecimal afterCreditBalance = beforeBalance.add(commissionAmount);
        String comTxnId = buildTransactionId("COM", baseTransactionId);
        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount, "Commission credited",
                hierarchyId, walletType, beforeBalance, afterCreditBalance,
                "Add Funds (Commission) - " + walletType.toUpperCase(), comTxnId);

        // Step 3: Calculate TDS (if applicable)
        BigDecimal tdsAmount = BigDecimal.ZERO;
        boolean isTdsApplicable = !(hierarchyType.equals("IM") || hierarchyType.equals("AD"));
        if (isTdsApplicable) {
            tdsAmount = commissionAmount.multiply(tdsRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Step 4: Deduct TDS (if applicable)
        BigDecimal afterTdsBalance = afterCreditBalance.subtract(tdsAmount);
        if (isTdsApplicable && tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            String tdsTxnId = buildTransactionId("TDS", baseTransactionId);

            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount, "TDS Deduction",
                    hierarchyId, walletType, afterCreditBalance, afterTdsBalance,
                    "Deduct Funds (TDS) - " + walletType.toUpperCase(), tdsTxnId);

            // Step 5: Credit TDS to Admin (Transaction only, no balance update)
            creditTdsToAdmin(hierarchyId, walletType, tdsAmount, baseTransactionId);
        }

        // Step 6: Update hierarchy's own wallet balance
        updateAmountTable(hierarchyType, hierarchyId, amountTable, afterTdsBalance.subtract(beforeBalance));
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

        String amountTable = getAmountTable("AD", walletType);
        String transactionTable = getTransactionTable("AD", walletType);

        String comTxnId = buildTransactionId("COM", baseTransactionId);
        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount,
                "Admin Commission credited", adminId, walletType, "Add Funds (Commission) - "+ walletType.toUpperCase(), comTxnId);
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

    private void insertTransactionForHierarchy(String transactionTable, String service, BigDecimal amount, String remarks, String hierarchyId, String walletType, String actionOnBalanceAmount, String tranxId) {
        BigDecimal beforeTransactionBalance = getCurrentBalance(hierarchyId, walletType);
        BigDecimal afterTransactionBalance = service.equals("CREDIT") ? beforeTransactionBalance.add(amount) : beforeTransactionBalance.subtract(amount);

        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeTransactionBalance, afterTransactionBalance);

        String columnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "INSERT INTO " + transactionTable +
                " (" + columnName + ",service, amount, remarks, date_of_transaction, time_of_transaction, " +
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
                .setParameter("beforeTransactionBalance", beforeTransactionBalance)
                .setParameter("afterTransactionBalance", afterTransactionBalance)
                .setParameter("actionOnBalanceAmount", actionOnBalanceAmount)
                .setParameter("finalBalance", afterTransactionBalance)
                .setParameter("tranxId", tranxId)
                .executeUpdate();
    }

    //This method is for commission credit entry
    private void insertTransactionForHierarchy(String transactionTable, String service, BigDecimal amount, String remarks,
                                               String hierarchyId, String walletType, BigDecimal beforeTransactionBalance,
                                               BigDecimal afterTransactionBalance, String actionOnBalance, String tranxId) {

        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {} | Action: {}",
                transactionTable, service, amount, remarks, hierarchyId, beforeTransactionBalance, afterTransactionBalance, actionOnBalance);

        String hierarchyColumnName = getHierarchyColumnName(extractHierarchyType(hierarchyId));

        String sql = "INSERT INTO " + transactionTable +
                " (" + hierarchyColumnName + ", service, amount, remarks, date_of_transaction, time_of_transaction, " +
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

    private void processPrepaidTransaction(TransactionSchemeDetailsRequestDto dto, String retailerId, BigDecimal requestedAmount, BigDecimal grossAmount, String taxValue, String productCode, String tranxType, String baseTransactionId) {
        RetailerPrepaidAmount prepaidAmount = retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .orElseThrow(() -> new RuntimeException("Prepaid account not found for Retailer ID: " + retailerId));

        updateWallet(dto, prepaidAmount, requestedAmount, grossAmount, taxValue, tranxType, baseTransactionId);
        retailerPrepaidAmountRepository.save(prepaidAmount);

        log.info("Prepaid Transaction Completed: Available Balance = {}", prepaidAmount.getAvailableBalance());
    }

    private void processPostpaidTransaction(TransactionSchemeDetailsRequestDto dto, String retailerId, BigDecimal requestedAmount, BigDecimal grossAmount, String taxValue, String productCode, String tranxType, String baseTransactionId) {
        RetailerPostpaidAmount postpaidAmount = retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .orElseThrow(() -> new RuntimeException("Postpaid account not found for Retailer ID: " + retailerId));

        updateWallet(dto, postpaidAmount, requestedAmount, grossAmount, taxValue, tranxType, baseTransactionId);
        retailerPostpaidAmountRepository.save(postpaidAmount);

        log.info("Postpaid Transaction Completed: Available Balance = {}", postpaidAmount.getAvailableBalance());
    }

    private <T extends AbstractRetailerAmount> void updateWallet(TransactionSchemeDetailsRequestDto dto, T wallet, BigDecimal requestedAmount,
                                                                 BigDecimal grossAmount, String taxValue, String tranxType, String baseTransactionId) {
        String actionOnBalance = "";

        Date transactionDate = new Date();
        Time transactionTime = new Time(System.currentTimeMillis());

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
        wallet.setLastUpdateDate(transactionDate);
        wallet.setLastUpdateTime(transactionTime);

        // Insert Credit transaction
        insertTransaction(wallet, tranxType, requestedAmount, balanceBeforeCredit,
                wallet.getAvailableBalance(), transactionDate, transactionTime, actionOnBalance, baseTransactionId);


    }

    private void insertTransaction(AbstractRetailerAmount wallet, String transactionType, BigDecimal amount,
                                   BigDecimal balanceBeforeTranx, BigDecimal balanceAfterTranx, Date transactionDate, Time transactionTime, String actionOnBalance, String tranxId) {

        if (wallet instanceof RetailerPrepaidAmount) {
            RetailerPrepaidTransactionDetails transaction = new RetailerPrepaidTransactionDetails();
            transaction.setTransactionId(tranxId);
            transaction.setDateOfTransaction(transactionDate);
            transaction.setTimeOfTransaction(transactionTime);
            transaction.setService("Retailer Wallet");
            transaction.setBalBeforeTran(balanceBeforeTranx);
            transaction.setRequestAmount(amount);
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
            transaction.setDateOfTransaction(transactionDate);
            transaction.setTimeOfTransaction(transactionTime);
            transaction.setService("Retailer Wallet");
            transaction.setBalBeforeTran(balanceBeforeTranx);
            transaction.setActionOnBalanceAmount(actionOnBalance);
            transaction.setRequestAmount(amount);
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

}
