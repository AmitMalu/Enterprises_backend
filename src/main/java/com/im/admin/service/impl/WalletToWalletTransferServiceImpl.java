package com.im.admin.service.impl;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import com.im.admin.dto.WalletToWalletTransferDto;
import com.im.admin.entities.enums.WalletType;
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
import java.time.LocalTime;
import java.util.Date;

@Service
@Slf4j
public class WalletToWalletTransferServiceImpl implements WalletToWalletTransferService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;


    @Transactional
    @Override
    public void transferAmountWalletToWallet(WalletToWalletTransferDto request) {

        String hierarchyId = request.getHierarchyId();
        WalletType fromWallet = request.getWalletTypeFrom();
        WalletType toWallet = request.getWalletTypeTo();
        BigDecimal amount = request.getTransferAmount();

        if (fromWallet == toWallet) {
            throw new IllegalArgumentException("Cannot transfer between the same wallet types");
        }

        // 1. Check balance
        BigDecimal availableBalance = commonMethods.getCurrentBalance(hierarchyId, String.valueOf(fromWallet));
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in retailer" + fromWallet + " wallet");
        }

        String baseTransactionId = commonMethods.generateBaseTransactionId();

        AbstractWalletAmountTable fromWalletTable = commonMethods.getWalletByType(fromWallet.name(), hierarchyId);
        updateWallet(fromWalletTable, hierarchyId, fromWallet, amount, "Debit", baseTransactionId, "SUCCESS","DEBIT");

        AbstractWalletAmountTable tomWalletTable = commonMethods.getWalletByType(toWallet.name(), hierarchyId);
        updateWallet(tomWalletTable, hierarchyId, toWallet, amount, "Credit", baseTransactionId, "SUCCESS","CREDIT");

    }

    private <T extends AbstractWalletAmountTable> void updateWallet(T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
                                                                 String tranxType, String baseTransactionId, String tranStatus, String service) {

        String actionOnBalance;

        // *** Credit or Debit Operation ***
        BigDecimal balanceBefore = wallet.getAvailableBalance();

        if (tranxType.equalsIgnoreCase("credit")) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().add(amount));
            actionOnBalance = "Add Funds - " + walletType.name();
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
            actionOnBalance = "Deduct Funds - " +  walletType.name();
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(LocalDate.now());
        wallet.setLastUpdatedTime(LocalTime.now());

        // Insert transaction
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
