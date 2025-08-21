package com.im.admin.service.impl;

import com.im.admin.dto.AbstractWalletAmountTable;
import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.entities.enums.TransactionType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.service.FundingService;
import com.im.admin.util.CommonMethods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class FundingServiceImpl  implements FundingService {

    @Autowired
    private CommonMethods commonMethods;


    @Transactional
    public String fundsCreditDebit(FundCreditDebitDto dto) {
        // Generate base transaction ID
        String baseTxnId = commonMethods.generateBaseTransactionId();

        // Extract values
        String userId = dto.getUserAccountId();
        WalletType walletType = WalletType.fromString(dto.getWalletType());
        TransactionType transactionType = TransactionType.fromString(dto.getFundingType());

        BigDecimal amount = BigDecimal.valueOf(dto.getAmount());
        BigDecimal confirmAmount = BigDecimal.valueOf(dto.getConfirmAmount());

        // Validate amount match
        if (amount.compareTo(confirmAmount) != 0) {
            throw new IllegalArgumentException("Amount and Confirm Amount do not match");
        }

        // Resolve wallet
        AbstractWalletAmountTable walletAmountTable = commonMethods.getWalletByType(walletType.name(), userId);

        // Generate transaction ID prefix based on type
        String txnIdPrefix = transactionType == TransactionType.CREDIT ? "FCR" : "FDR";
        String txnId = txnIdPrefix + baseTxnId;

        commonMethods.performWalletUpdate(
                walletAmountTable,
                userId,
                walletType,
                amount,
                transactionType,
                txnId,
                transactionType.name(),
                dto.getRemarks()
        );

        return String.format("Transaction %s successful", transactionType.name());
    }

}
