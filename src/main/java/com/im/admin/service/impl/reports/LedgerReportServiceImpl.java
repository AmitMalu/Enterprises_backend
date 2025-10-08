package com.im.admin.service.impl.reports;



import com.im.admin.dto.LedgerReportDto;
import com.im.admin.entities.LedgerReport;
import com.im.admin.repositories.LedgerReportRepository;
import com.im.admin.service.reports.LedgerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LedgerReportServiceImpl implements LedgerReportService {

    @Autowired
    private LedgerReportRepository repo;

    @Override
    public Page<LedgerReportDto> getLedgerReports(
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
    ) {
        Page<LedgerReport> page = repo.findLedgerReportsWithFilters(
                id, transactionId, hierarchyId, dateOfTransaction, fromDate, toDate, timeOfTransaction,
                balBeforeTran, amount, balAfterTran, finalBalance,
                tranStatus, actionOnBalanceAmount, service, mobileNo,
                subProductName, remarks, narration, sourceTable,
                pageable
        );

        return page.map(this::toDto);
    }


    private LedgerReportDto toDto(LedgerReport e) {
        return LedgerReportDto.builder()
                .id(e.getId())
                .transactionId(e.getTransactionId())
                .hierarchyId(e.getHierarchyId())
                .dateOfTransaction(e.getDateOfTransaction())
                .timeOfTransaction(e.getTimeOfTransaction())
                .balBeforeTran(e.getBalBeforeTran())
                .amount(e.getAmount())
                .balAfterTran(e.getBalAfterTran())
                .finalBalance(e.getFinalBalance())
                .tranStatus(e.getTranStatus())
                .actionOnBalanceAmount(e.getActionOnBalanceAmount())
                .service(e.getService())
                .mobileNo(e.getMobileNo())
                .subProductName(e.getSubProductName())
                .remarks(e.getRemarks())
                .narration(e.getNarration())
                .sourceTable(e.getSourceTable())
                .build();
    }

    @Override
    public List<LedgerReportDto> getLedgerReportsForDownload(LocalDate fromDate, LocalDate toDate) {
        List<LedgerReport> entities = repo.findLedgerReportsForDownload(fromDate, toDate);
        return entities.stream().map(this::toDto).toList();
    }

}

