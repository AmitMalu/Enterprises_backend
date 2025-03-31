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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Time;
import java.util.*;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private SchemeRepository schemeRepository;

    @Autowired
    private SchemeRateRepository schemeRateRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private AdminRepository adminRepository;

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

        String walletType = getWalletType(request);
        SchemeRate matchedSchemeRate = findMatchingSchemeRate(request);

        return new TransactionSchemeDetailsResponseDto(matchedSchemeRate, walletType, gstValue.toString());
    }

    @Override
    @Transactional
    public String processRetailerTransaction(TransactionSchemeDetailsRequestDto transactionSchemeDetailsRequestDto) {

        //TODO call vendor API and get response (if SUCCESS then only below implementation is applicable for FAILURE implementation is not done yet)
        // callVendor API here

        // Extract request details
        String retailerId = transactionSchemeDetailsRequestDto.getHierarchyId();
        BigDecimal requestedAmount = BigDecimal.valueOf(transactionSchemeDetailsRequestDto.getAmount());
        BigDecimal grossAmount = BigDecimal.valueOf(transactionSchemeDetailsRequestDto.getSchemeRate().getGross());
        String walletType = transactionSchemeDetailsRequestDto.getWalletType().toUpperCase(); // Convert to uppercase for consistency
        SchemeRate schemeRate = transactionSchemeDetailsRequestDto.getSchemeRate();
        String taxValue = transactionSchemeDetailsRequestDto.getTaxValue();
        BigDecimal tdsRate = BigDecimal.valueOf(Long.parseLong(transactionSchemeDetailsRequestDto.getTdsRate()));

        log.info("Processing transaction for Retailer ID: {} | Wallet Type: {} | Requested Amount: {} | Gross Amount: {}",
                retailerId, walletType, requestedAmount, grossAmount);

        if ("PREPAID".equalsIgnoreCase(walletType)) {
            processPrepaidTransaction(retailerId, requestedAmount, grossAmount, taxValue);
        } else if ("POSTPAID".equalsIgnoreCase(walletType)) {
            processPostpaidTransaction(retailerId, requestedAmount, grossAmount, taxValue);
        } else {
            throw new IllegalArgumentException("Invalid wallet type: " + walletType);
        }

        Retailer retailer = retailerRepository.findByRetailerId(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found with ID: " + retailerId));

        distributeCommissionToHierarchies(retailer.getParentAccount(), schemeRate, walletType, tdsRate);

        return "Transaction Successful";
    }

    private void distributeCommissionToHierarchies(String hierarchyId, SchemeRate schemeRate, String walletType, BigDecimal tdsRate) {
        if (hierarchyId == null || hierarchyId.isEmpty()) {
            return;
        }

        String hierarchyType = extractHierarchyType(hierarchyId);
        BigDecimal commissionAmount = getCommissionAmount(hierarchyType, schemeRate);

        if (commissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            if ("AD".equals(hierarchyType)) {
                distributeCommissionToAdmin(hierarchyId, commissionAmount, walletType);
                return; // Stop further processing
            } else {
                distributeCommissionToHierarchy(hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);
            }
        }

        findParentHierarchy(hierarchyId).ifPresent(parentId ->
                distributeCommissionToHierarchies(parentId, schemeRate, walletType, tdsRate)
        );
    }

    private void distributeCommissionToHierarchy(String hierarchyType, String hierarchyId, BigDecimal commissionAmount, String walletType, BigDecimal tdsRate) {
        log.info("Processing commission for {} | Hierarchy ID: {} | Amount: {} | Wallet Type: {} | TDS Rate: {}",
                hierarchyType, hierarchyId, commissionAmount, walletType, tdsRate);

        BigDecimal tdsAmount = commissionAmount.multiply(tdsRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        String transactionTable = getTransactionTable(hierarchyType, walletType);
        String amountTable = getAmountTable(hierarchyType, walletType);

        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount, "Commission credited", hierarchyId, walletType);

        if (tdsAmount.compareTo(BigDecimal.ZERO) > 0) {
            insertTransactionForHierarchy(transactionTable, "DEBIT", tdsAmount, "TDS Deduction", hierarchyId, walletType);
        }

        updateAmountTable(hierarchyType, hierarchyId, amountTable, commissionAmount.subtract(tdsAmount)); // Update final amount after TDS deduction
    }

    private void distributeCommissionToAdmin(String adminId, BigDecimal commissionAmount, String walletType) {
        log.info("Processing commission for Admin | Admin ID: {} | Amount: {} | Wallet Type: {} (No TDS Deduction)",
                adminId, commissionAmount, walletType);

        String amountTable = getAmountTable("AD", walletType);
        String transactionTable = getTransactionTable("AD", walletType);

        insertTransactionForHierarchy(transactionTable, "CREDIT", commissionAmount, "Admin Commission credited", adminId, walletType);
        updateAmountTable("AD", adminId, amountTable, commissionAmount);
    }


    private String extractHierarchyType(String hierarchyId) {
        String[] parts = hierarchyId.split("-");
        return (parts.length >= 2) ? parts[1] : "UNKNOWN";
    }

    private BigDecimal getCommissionAmount(String hierarchyType, SchemeRate schemeRate) {
        switch (hierarchyType) {
            case "AD": return BigDecimal.valueOf(schemeRate.getIm());
            case "ET": return BigDecimal.valueOf(schemeRate.getEnterprise());
            case "WL": return BigDecimal.valueOf(schemeRate.getWhitelabel());
            case "SD": return BigDecimal.valueOf(schemeRate.getSuperDistributor());
            case "PT": return BigDecimal.valueOf(schemeRate.getPartner());
            case "DT": return BigDecimal.valueOf(schemeRate.getDistributor());
            case "RT": return BigDecimal.valueOf(schemeRate.getRetailer());
            default: return BigDecimal.ZERO;
        }
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
        return entityName + (walletType.equals("PREPAID") ? "_prepaid_transaction_details" : "_postpaid_transaction_details");
    }

    private String getAmountTable(String hierarchyType, String walletType) {
        String entityName = getEntityName(hierarchyType);
        return entityName + (walletType.equals("PREPAID") ? "_prepaid_amount" : "_postpaid_amount");
    }

    /**
     * Converts hierarchyType (like "DT", "SD") to the correct entity name
     * that matches table naming convention.
     */
    private String getEntityName(String hierarchyType) {
        switch (hierarchyType) {
            case "AD": return "admin";
            case "ET": return "enterprises";
            case "WL": return "white_label";
            case "SD": return "super_distributor";
            case "PT": return "partner";
            case "DT": return "distributor";
            case "RT": return "retailer";
            default: throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        }
    }

    private String formatTableName(String hierarchyType, String walletType, String tableType) {
        return convertToSnakeCase(hierarchyType) + "_" + walletType.toLowerCase() + "_" + tableType;
    }

    private String convertToSnakeCase(String input) {
        return input.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    private void insertTransactionForHierarchy(String transactionTable,
                                               String service, BigDecimal amount, String remarks, String hierarchyId, String walletType) {
        log.info("Inserting into {} | Type: {} | Amount: {} | Remarks: {} | Hierarchy ID: {}",
                transactionTable, service, amount, remarks, hierarchyId);

        BigDecimal currentBalance = getCurrentBalance(hierarchyId, walletType);

        BigDecimal beforeTransactionBalance = currentBalance;
        BigDecimal afterTransactionBalance = service.equals("CREDIT") ? beforeTransactionBalance.add(amount) : beforeTransactionBalance.subtract(amount);

        String sql = "INSERT INTO " + transactionTable +
                " (service, amount, remarks, date_of_transaction, time_of_transaction, " +
                "ch_bal_before_tran, ch_bal_after_tran) " +
                "VALUES (:service, :amount, :remarks, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance)";

        entityManager.createNativeQuery(sql)
                .setParameter("service", service)
                .setParameter("amount", amount)
                .setParameter("remarks", remarks)
                .setParameter("dateOfTransaction", new Date())
                .setParameter("timeOfTransaction", new Time(System.currentTimeMillis()))
                .setParameter("beforeTransactionBalance", beforeTransactionBalance)
                .setParameter("afterTransactionBalance", afterTransactionBalance)
                .executeUpdate();
    }

    private void updateAmountTable(String hierarchyType, String hierarchyId, String amountTable, BigDecimal amount) {
        log.info("Updating {} | Hierarchy Type: {} | Hierarchy ID: {} | Amount: {}", amountTable, hierarchyType, hierarchyId, amount);

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

    /**
     * Returns the correct column name based on hierarchy type.
     */
    private String getHierarchyColumnName(String hierarchyType) {
        switch (hierarchyType) {
            case "ET":
                return "enterprises_id";
            case "PT":
                return "partner_id";
            case "RT":
                return "retailer_id";
            case "DT":
                return "distributor_id";
            case "SD":
                return "super_distributor_id";
            case "WL":
                return "white_label_id";
            case "AD":
                return "admin_id";
            default:
                throw new IllegalArgumentException("Invalid hierarchy type: " + hierarchyType);
        }
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


    private void processPrepaidTransaction(String retailerId, BigDecimal requestedAmount, BigDecimal grossAmount, String taxValue) {
        RetailerPrepaidAmount prepaidAmount = retailerPrepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .orElseThrow(() -> new RuntimeException("Prepaid account not found for Retailer ID: " + retailerId));

        updateWallet(prepaidAmount, requestedAmount, grossAmount, taxValue);
        retailerPrepaidAmountRepository.save(prepaidAmount);

        log.info("Prepaid Transaction Completed: Available Balance = {}", prepaidAmount.getAvailableBalance());
    }

    private void processPostpaidTransaction(String retailerId, BigDecimal requestedAmount, BigDecimal grossAmount, String taxValue) {
        RetailerPostpaidAmount postpaidAmount = retailerPostpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .orElseThrow(() -> new RuntimeException("Postpaid account not found for Retailer ID: " + retailerId));

        updateWallet(postpaidAmount, requestedAmount, grossAmount, taxValue);
        retailerPostpaidAmountRepository.save(postpaidAmount);

        log.info("Postpaid Transaction Completed: Available Balance = {}", postpaidAmount.getAvailableBalance());
    }

    private <T extends AbstractRetailerAmount> void updateWallet(T wallet, BigDecimal requestedAmount, BigDecimal grossAmount, String taxValue) {

        Date transactionDate = new Date();
        Time transactionTime = new Time(System.currentTimeMillis());

        // *** Credit Operation ***
        BigDecimal balanceBeforeCredit = wallet.getAvailableBalance();
        wallet.setTotalCash(wallet.getTotalCash().add(requestedAmount));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(requestedAmount));
        wallet.setLastUpdatedAmount(requestedAmount);
        wallet.setLastUpdateDate(transactionDate);
        wallet.setLastUpdateTime(transactionTime);

        // Insert Credit transaction
        insertTransaction(wallet, "CREDIT", requestedAmount, balanceBeforeCredit,
                wallet.getAvailableBalance(), transactionDate, transactionTime);

        // Convert taxValue to BigDecimal
        BigDecimal taxRate = new BigDecimal(taxValue).divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);

        // Compute (1 + taxRate) with high precision
        BigDecimal divisor = BigDecimal.ONE.add(taxRate);

        // Calculate base amount (grossAmount / (1 + taxRate)) with high precision
        BigDecimal baseAmount = grossAmount.divide(divisor, 10, RoundingMode.HALF_UP);

        // Calculate tax amount (baseAmount * taxRate) with high precision
        BigDecimal taxAmount = baseAmount.multiply(taxRate).setScale(10, RoundingMode.HALF_UP);

        // Round both values to 4 decimal places
        baseAmount = baseAmount.setScale(4, RoundingMode.HALF_UP);
        taxAmount = taxAmount.setScale(4, RoundingMode.HALF_UP);

        // *** Combined Debit (Base Amount + Tax Amount) ***
        BigDecimal totalDebit = baseAmount.add(taxAmount);

        BigDecimal balanceBeforeDebit = wallet.getAvailableBalance();
        wallet.setUsedCash(wallet.getUsedCash().add(totalDebit));
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(totalDebit));
        wallet.setLastUpdatedAmount(totalDebit);
        wallet.setLastUpdateDate(transactionDate);
        wallet.setLastUpdateTime(transactionTime);

        // ** Insert transaction records separately for tracking **
        insertTransaction(wallet, "DEBIT", baseAmount, balanceBeforeDebit,
                balanceBeforeDebit.subtract(baseAmount), transactionDate, transactionTime);

        insertTransaction(wallet, "DEBIT", taxAmount,
                balanceBeforeDebit.subtract(baseAmount), wallet.getAvailableBalance(), transactionDate, transactionTime);
    }

    private void insertTransaction(AbstractRetailerAmount wallet, String transactionType, BigDecimal amount,
                                   BigDecimal balanceBefore, BigDecimal balanceAfter, Date transactionDate, Time transactionTime) {

        if (wallet instanceof RetailerPrepaidAmount) {
            RetailerPrepaidTransactionDetails transaction = new RetailerPrepaidTransactionDetails();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setDateOfTransaction(transactionDate);
            transaction.setTimeOfTransaction(transactionTime);
            transaction.setService("Retailer Wallet");
            transaction.setChBalBeforTran(balanceBefore);
            transaction.setRequestAmount(amount);
            transaction.setAmount(amount);
            transaction.setChBalAfterTran(balanceAfter);
            transaction.setTranStatus(transactionType);
            transaction.setChFinalBalance(balanceAfter);
            transaction.setRetailerId(((RetailerPrepaidAmount) wallet).getRetailerId().getRetailerId());
            retailerPrepaidTransactionDetailsRepository.save(transaction);
        } else if (wallet instanceof RetailerPostpaidAmount) {
            RetailerPostpaidTransactionDetails transaction = new RetailerPostpaidTransactionDetails();
            transaction.setTransactionId(UUID.randomUUID().toString());
            transaction.setDateOfTransaction(transactionDate);
            transaction.setTimeOfTransaction(transactionTime);
            transaction.setService("Retailer Wallet");
            transaction.setChBalBeforTran(balanceBefore);
            transaction.setRequestAmount(amount);
            transaction.setAmount(amount);
            transaction.setChBalAfterTran(balanceAfter);
            transaction.setTranStatus(transactionType);
            transaction.setChFinalBalance(balanceAfter);
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

}
