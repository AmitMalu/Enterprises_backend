package com.im.admin.service.reports;

import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.WalletDownloadReportResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface WalletDownloadReportService {

    /**
     * Generates and saves an hourly cumulative Wallet History report.
     * Covers 00:00:00 to hourEnd of reportDate.
     */
    void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception;

    /**
     * Generates and saves the full daily Wallet History report (00:00:00 – 23:59:59).
     */
    void generateDailyReport(LocalDate reportDate) throws Exception;

    /**
     * Paginated list of all generated reports, latest first.
     */
    CommonResponse<Page<WalletDownloadReportResponse>> listReports(int page, int size);

    /**
     * Paginated list filtered by date.
     */
    CommonResponse<Page<WalletDownloadReportResponse>> listReportsByDate(LocalDate date, int page, int size);

    /**
     * Stream the Excel file for the given report id to the HTTP response.
     */
    void downloadById(Long id, HttpServletResponse response) throws Exception;
}
