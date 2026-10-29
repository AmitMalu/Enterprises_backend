package com.scs.admin.service.reports;

import com.scs.admin.entities.enums.WalletReportType;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.WalletDownloadReportResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface WalletDownloadReportService {

    void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception;

    void generateDailyReport(LocalDate reportDate) throws Exception;

    CommonResponse<Page<WalletDownloadReportResponse>> listReports(int page, int size);

    CommonResponse<Page<WalletDownloadReportResponse>> listReportsByType(WalletReportType reportType, int page, int size);

    CommonResponse<Page<WalletDownloadReportResponse>> listReportsByDate(LocalDate date, int page, int size);

    CommonResponse<Page<WalletDownloadReportResponse>> listReportsByTypeAndDate(WalletReportType reportType, LocalDate date, int page, int size);

    void downloadById(Long id, HttpServletResponse response) throws Exception;
}
