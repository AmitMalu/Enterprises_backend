package com.scs.admin.service.reports;

import com.scs.admin.dto.RefundTransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;
import java.util.List;

public interface RefundTransactionReportService {
    Page<RefundTransactionDto> getRefundTransactions(String hierarchyType, String walletType, Date fromDate, Date toDate,
                                                     String transactionId, BigDecimal amount, BigDecimal balAfterTran,
                                                     BigDecimal balBeforeTran, Date dateOfTransaction,
                                                     BigDecimal finalBalance, Integer id, Time timeOfTransaction,
                                                     Date updatedDateOfTransaction, Time updatedTimeOfTransaction,
                                                     String actionOnBalanceAmount, String bankRefId,
                                                     String operatorName, String userId,
                                                     String schemeHierarchyName, String schemeName,
                                                     String service, String subOperatorName,
                                                     String subProductName, String tranStatus,
                                                     String transactionType, Pageable pageable);
}

