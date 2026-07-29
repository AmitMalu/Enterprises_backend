package com.im.admin.service.impl;


import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.entities.FundTransfer;
import com.im.admin.entities.enums.ServiceType;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.repositories.FundTransferRepository;
import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.FundTransferResponseDto;
import com.im.admin.service.FundTransferService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

@Service
@Slf4j
public class FundTransferServiceImpl implements FundTransferService {

    @Autowired
    private FundTransferRepository fundTransferRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SubProductsServiceImpl subProductsServiceImpl;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto) {

        // VALIDATE SUB PRODUCT VISIBILITY
        if (!subProductsServiceImpl.isSubProductVisible(requestDto.getSubProductId()))
        {
            throw new IllegalArgumentException(
                    "Selected sub product is currently not visible."
            );
        }

        log.info("SubProduct is visible and available for wallet transfer. subProductId={}", requestDto.getSubProductId());

        validateRequest(requestDto);

        BigDecimal amount = requestDto.getAmount();
        String fromId = requestDto.getFromHierarchyId();
        String toId = requestDto.getToHierarchyId();
        WalletType fromWalletType = requestDto.getFromWalletType();
        WalletType toWalletType = requestDto.getToWalletType();

        String baseTxnId = commonMethods.generateBaseTransactionId();
        LocalDate date = LocalDate.now();
        LocalTime time = LocalTime.now();

        AbstractWalletAmountTable fromWallet = commonMethods.getWalletByType(fromWalletType.name(), fromId);
        BigDecimal availableBalance = fromWallet.getAvailableBalance();
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance for transfer.");
        }

        AbstractWalletAmountTable toWallet = commonMethods.getWalletByType(toWalletType.name(), toId);

        performWalletUpdate(fromWallet, fromId, fromWalletType, amount, TransactionType.DEBIT, baseTxnId, TransactionType.DEBIT.name(), "Fund Transfer Successfully to " + requestDto.getToHierarchyId() + "/ @" + date + " " + time, date, time);
        performWalletUpdate(toWallet, toId, toWalletType, amount, TransactionType.CREDIT, baseTxnId, TransactionType.CREDIT.name(), "Received from " + fromId, date, time);

        FundTransfer savedTransfer = saveFundTransferRecord(requestDto);
        return buildResponse(savedTransfer);
    }

    private void validateRequest(FundTransferRequestDto dto) {
        if (dto.getAmount() == null || dto.getConfirmAmount() == null) {
            throw new IllegalArgumentException("Amount fields must not be null.");
        }
        if (dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (!dto.getAmount().equals(dto.getConfirmAmount())) {
            throw new IllegalArgumentException("Amount and confirm amount do not match.");
        }
    }

    private <T extends AbstractWalletAmountTable> void performWalletUpdate(
            T wallet, String hierarchyId, WalletType walletType, BigDecimal amount,
            TransactionType type, String baseTxnId, String service, String remarks,
            LocalDate date, LocalTime time) {

        BigDecimal beforeBalance = wallet.getAvailableBalance();
        //BigDecimal afterBalance;

        if (type == TransactionType.CREDIT) {
            wallet.setTotalCash(wallet.getTotalCash().add(amount));
            wallet.setAvailableBalance(beforeBalance.add(amount));
        } else {
            wallet.setTotalCash(wallet.getTotalCash().subtract(amount));
            wallet.setAvailableBalance(beforeBalance.subtract(amount));
        }

        wallet.setLastUpdatedAmount(amount);
        wallet.setLastUpdatedDate(date);
        wallet.setLastUpdatedTime(time);

        String action = (type == TransactionType.CREDIT ? "Add" : "Deduct") + " Funds - " + walletType.name();

        insertTransaction(
                walletType.name(), hierarchyId, Status.SUCCESS.name(), amount,
                beforeBalance, wallet.getAvailableBalance(), action,
                ServiceType.FTR + baseTxnId, service, remarks, date, time, "Fund Transfer"
        );
    }

    private void insertTransaction(String walletType, String hierarchyId, String status, BigDecimal amount,
                                   BigDecimal before, BigDecimal after, String action, String txnId,
                                   String service, String remarks, LocalDate date, LocalTime time, String subproductName) {

        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        String sql = "INSERT INTO " + tableName +
                " (" + columnName + ", service, amount, date_of_transaction, time_of_transaction, " +
                "bal_before_tran, bal_after_tran, action_on_balance_amount, final_balance, transaction_id, tran_status, remarks, sub_product_name) " +
                "VALUES (:hierarchyId, :service, :amount, :date, :time, :before, :after, :action, :final, :txnId, :status, :remarks, :subproductName)";

        entityManager.createNativeQuery(sql)
                .setParameter("subproductName", subproductName)
                .setParameter("hierarchyId", hierarchyId)
                .setParameter("service", service)
                .setParameter("amount", amount)
                .setParameter("date", Date.valueOf(date))
                .setParameter("time", Time.valueOf(time))
                .setParameter("before", before)
                .setParameter("after", after)
                .setParameter("action", action)
                .setParameter("final", after)
                .setParameter("txnId", txnId)
                .setParameter("status", status)
                .setParameter("remarks", remarks)
                .executeUpdate();
    }

    private FundTransfer saveFundTransferRecord(FundTransferRequestDto dto) {
        FundTransfer transfer = new FundTransfer();
        transfer.setFromHierarchyId(dto.getFromHierarchyId());
        transfer.setToHierarchyId(dto.getToHierarchyId());
        transfer.setAmount(dto.getAmount());
        transfer.setFromWalletType(dto.getFromWalletType());
        transfer.setToWalletType(dto.getToWalletType());
        transfer.setNarration(dto.getNarration());
        return fundTransferRepository.save(transfer);
    }

    private FundTransferResponseDto buildResponse(FundTransfer transfer) {
        return new FundTransferResponseDto(
                transfer.getId(),
                transfer.getFromHierarchyId(),
                transfer.getToHierarchyId(),
                transfer.getAmount(),
                transfer.getFromWalletType(),
                transfer.getToWalletType(),
                transfer.getTransferDate(),
                transfer.getNarration()
        );
    }


}
