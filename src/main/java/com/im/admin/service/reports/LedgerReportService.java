package com.im.admin.service.reports;

import com.im.admin.dto.LedgerReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface LedgerReportService {

    Page<LedgerReportDto> getLedgerReports(
            String id,
            String transactionId,
            String hierarchyId,
            LocalDate dateOfTransaction,
            LocalDate fromDate,
            LocalDate toDate,
            String timeOfTransaction,
            BigDecimal balBeforeTran,
            BigDecimal amount,
            BigDecimal balAfterTran,
            BigDecimal finalBalance,
            String tranStatus,
            String actionOnBalanceAmount,
            String service,
            String mobileNo,
            String subProductName,
            String remarks,
            String narration,
            String sourceTable,
            Pageable pageable
    );

    List<LedgerReportDto> getLedgerReportsForDownload(LocalDate fromDate, LocalDate toDate);

}
