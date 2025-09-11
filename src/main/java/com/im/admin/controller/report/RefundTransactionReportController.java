package com.im.admin.controller.report;

import com.im.admin.dto.RefundTransactionDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.RefundTransactionReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;
import java.util.List;


@RestController
@RequestMapping("/api/refund")
public class RefundTransactionReportController {

    @Autowired
    private RefundTransactionReportService refundTransactionReportService;


    @GetMapping("/search")
    public ResponseEntity<CommonResponse<Object>> searchRefundTransactions(
            @RequestParam String hierarchyType,
            @RequestParam(required = false) String walletType,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date fromDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date toDate,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal balAfterTran,
            @RequestParam(required = false) BigDecimal balBeforeTran,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date dateOfTransaction,
            @RequestParam(required = false) BigDecimal finalBalance,
            @RequestParam(required = false) Integer id,
            @RequestParam(required = false) Time timeOfTransaction,
            @RequestParam(required = false) Date updatedDateOfTransaction,
            @RequestParam(required = false) Time updatedTimeOfTransaction,
            @RequestParam(required = false) String actionOnBalanceAmount,
            @RequestParam(required = false) String bankRefId,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String schemeHierarchyName,
            @RequestParam(required = false) String schemeName,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String subOperatorName,
            @RequestParam(required = false) String subProductName,
            @RequestParam(required = false) String tranStatus,
            @RequestParam(required = false) String transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<RefundTransactionDto> dataPage = refundTransactionReportService.getRefundTransactions(
                hierarchyType, walletType, fromDate, toDate, transactionId, amount, balAfterTran,
                balBeforeTran, dateOfTransaction, finalBalance, id, timeOfTransaction, updatedDateOfTransaction,
                updatedTimeOfTransaction, actionOnBalanceAmount, bankRefId, operatorName, userId,
                schemeHierarchyName, schemeName, service, subOperatorName, subProductName,
                tranStatus, transactionType, pageable);

        return ResponseEntity.ok(new CommonResponse<>(200, "Refund transactions fetched successfully", dataPage));
    }

}


