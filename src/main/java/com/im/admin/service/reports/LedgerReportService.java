package com.im.admin.service.reports;

import com.im.admin.dto.LedgerReportDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface LedgerReportService {

    Page<LedgerReportDto> getLedgerReports(LocalDate fromDate, LocalDate toDate, Pageable pageable);

    List<LedgerReportDto> getLedgerReportsForDownload(LocalDate fromDate, LocalDate toDate);

}
