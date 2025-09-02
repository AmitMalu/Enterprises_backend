package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.WalletToWalletTransferService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
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


    @Transactional
    @Override
    public CommonResponse<String> transferAmountWalletToWallet(WalletToWalletTransferDto request) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestJson = null;
        String responseJson = null;

        try {
            // Serialize request
            requestJson = objectMapper.writeValueAsString(request);
            log.info("Initiating wallet-to-wallet transfer: {}", requestJson);

            String hierarchyId = request.getHierarchyId();
            WalletType fromWallet = request.getWalletTypeFrom();
            WalletType toWallet = request.getWalletTypeTo();
            BigDecimal amount = request.getTransferAmount();
            BigDecimal confirmAmount = request.getConfirmAmount();

            if (!Objects.equals(amount, confirmAmount)) {
                log.warn("Amount mismatch: {} != {}", amount, confirmAmount);
                throw new IllegalArgumentException("Amount and confirmation amount do not match.");
            }

            if (fromWallet == toWallet) {
                log.warn("Same wallet type transfer attempted: {}", fromWallet);
                throw new IllegalArgumentException("Cannot transfer between the same wallet types.");
            }

            BigDecimal availableBalance = commonMethods.getCurrentBalance(hierarchyId, String.valueOf(fromWallet));
            log.info("Available balance in {} wallet: {}", fromWallet, availableBalance);

            if (availableBalance.compareTo(amount) < 0) {
                log.warn("Insufficient balance in {} wallet. Required: {}, Available: {}", fromWallet, amount, availableBalance);
                throw new IllegalArgumentException("Insufficient balance in retailer " + fromWallet + " wallet.");
            }

            String baseTransactionId = commonMethods.generateBaseTransactionId();
            log.info("Generated base transaction ID: {}", baseTransactionId);

            AbstractWalletAmountTable fromWalletTable = commonMethods.getWalletByType(fromWallet.name(), hierarchyId);
            updateWallet(fromWalletTable, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT");

            String currentId = hierarchyId;

            while (true) {
                Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
                if (parentOpt.isEmpty()) {
                    log.error("No approver found for hierarchy: {}", hierarchyId);
                    throw new RuntimeException("No approver found for hierarchy: " + hierarchyId);
                }

                String parentId = parentOpt.get();
                String parentType = commonMethods.extractHierarchyType(parentId);
                log.info("Found parent hierarchy: {} with type {}", parentId, parentType);

                if ("WL".equals(parentType)) {
                    whiteLabelRepository.findById(parentId).ifPresent(wl -> {
                        log.info("Processing WL level transfer for WL ID: {}", wl.getWhiteLabelId());
                        AbstractWalletAmountTable from = commonMethods.getWalletByType(fromWallet.name(), wl.getWhiteLabelId());
                        updateWallet(from, hierarchyId, fromWallet, amount, "Credit", baseTransactionId, "SUCCESS", "DEBIT");

                        AbstractWalletAmountTable to = commonMethods.getWalletByType(toWallet.name(), wl.getWhiteLabelId());
                        updateWallet(to, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT");
                    });
                    break;
                }

                if ("IM".equals(parentType) || "AD".equals(parentType)) {
                    adminRepository.findById(parentId).ifPresent(ad -> {
                        log.info("Processing {} level transfer for Admin ID: {}", parentType, ad.getId());
                        AbstractWalletAmountTable from = commonMethods.getWalletByType(fromWallet.name(), ad.getId());
                        updateWallet(from, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT");

                        AbstractWalletAmountTable to = commonMethods.getWalletByType(fromWallet.name(), ad.getId());
                        updateWallet(to, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS", "DEBIT");
                    });
                    break;
                }

                currentId = parentId;
            }

            AbstractWalletAmountTable toWalletTable = commonMethods.getWalletByType(toWallet.name(), hierarchyId);
            updateWallet(toWalletTable, hierarchyId, toWallet, amount, "Credit", baseTransactionId, "SUCCESS", "CREDIT");

            CommonResponse<String> response = new CommonResponse<>(200, "Transfer successful", baseTransactionId);
            responseJson = objectMapper.writeValueAsString(response);

            log.info("Transfer completed successfully. Transaction ID: {}", baseTransactionId);

            return response;

        } catch (Exception ex) {
            log.error("Wallet-to-wallet transfer failed", ex);
            responseJson = "{\"error\": \"" + ex.getMessage() + "\"}";
            throw ex;

        } finally {
            LocalDateTime responseDateTime = LocalDateTime.now();
            // Save logs whether success or failure
            commonFunctions.saveImLogs(
                    requestJson,
                    requestDateTime,
                    null,
                    responseJson,
                    responseDateTime
            );
        }
    }



    private <T extends AbstractWalletAmountTable> void updateWallet(T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
                                                                    String tranxType, String baseTransactionId, String tranStatus, String service) {

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
                wallet.getAvailableBalance(), actionOnBalance, "TXN" + baseTransactionId, service);
    }


    private void insertTransaction(String walletType, String hierarchyId, String tranStatus, BigDecimal amount, BigDecimal beforeBalance,
                                   BigDecimal afterBalance, String actionOnBalanceAmount, String tranxId, String service) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String transactionTableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        log.info("Inserting into {} | Type: {} | Amount: {} | Hierarchy ID: {} | Before Balance: {} | After Balance: {}",
                transactionTableName, service, amount, hierarchyId, beforeBalance, afterBalance);

        String sql = "INSERT INTO " + transactionTableName +
                " (" + columnName + "  ,service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status) " +
                "VALUES (:hierarchyId, :service, :amount, :dateOfTransaction, :timeOfTransaction, " +
                ":beforeTransactionBalance, :afterTransactionBalance, :actionOnBalanceAmount, :finalBalance, :tranxId, :tranStatus)";

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
                .executeUpdate();
    }
}
