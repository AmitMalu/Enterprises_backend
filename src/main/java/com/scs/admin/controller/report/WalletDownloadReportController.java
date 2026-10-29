package com.scs.admin.controller.report;

import com.scs.admin.response.CommonResponse;
import com.scs.admin.cronJob.WalletHistoryReportScheduler;

import com.scs.admin.response.reports.WalletDownloadReportResponse;
import com.scs.admin.service.reports.WalletDownloadReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDate;

/**
 * Download Report screen APIs for Wallet History Reports.
 *
 * Base URL: /api/reports/wallet-history
 */
@Slf4j
@RestController
@RequestMapping("/api/reports/wallet-history")
@RequiredArgsConstructor
public class WalletDownloadReportController {


    @Autowired
    private WalletHistoryReportScheduler scheduler;;

    private final WalletDownloadReportService walletDownloadReportService;

    /**
     * GET /api/reports/wallet-history/list?page=0&size=10
     * Returns paginated list of all generated reports (latest first).
     * Screen columns: ID, Date, Timestamp, Subject, Download Link
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResponse<Page<WalletDownloadReportResponse>>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: listWalletDownloadReports | page={} size={}", page, size);
        return ResponseEntity.ok(walletDownloadReportService.listReports(page, size));
    }

    /**
     * GET /api/reports/wallet-history/list?date=2026-06-23&page=0&size=10
     * Returns paginated list filtered by a specific date.
     */
    @GetMapping("/list/by-date")
    public ResponseEntity<CommonResponse<Page<WalletDownloadReportResponse>>> listByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: listWalletDownloadReportsByDate | date={} page={} size={}", date, page, size);
        return ResponseEntity.ok(walletDownloadReportService.listReportsByDate(date, page, size));
    }

    /**
     * GET /api/reports/wallet-history/download/{id}
     * Streams the Excel file for the given report ID.
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        log.info("API: downloadWalletReport | id={}", id);
        try {
            walletDownloadReportService.downloadById(id, response);
        } catch (Exception e) {
            log.error("Error downloading wallet report id={}: {}", id, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * POST /api/reports/wallet-history/generate/hourly?date=2026-06-23&hour=3
     * Admin endpoint: manually trigger hourly report generation for a specific hour.
     */
    @PostMapping("/generate/hourly")
    public ResponseEntity<CommonResponse<String>> generateHourly(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam int hour) {

        log.info("API: manual generate hourly wallet report | date={} hour={}", date, hour);
        try {
            walletDownloadReportService.generateHourlyReport(date, date.atTime(hour, 0, 0));
            return ResponseEntity.ok(CommonResponse.success(
                    "Generated", "Hourly wallet history report generated for " + date + " hour=" + hour));
        } catch (Exception e) {
            log.error("Manual hourly generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(CommonResponse.error(500, e.getMessage()));
        }
    }

    /**
     * POST /api/reports/wallet-history/generate/daily?date=2026-06-23
     * Admin endpoint: manually trigger daily report generation.
     */
    @PostMapping("/generate/daily")
    public ResponseEntity<CommonResponse<String>> generateDaily(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("API: manual generate daily wallet report | date={}", date);
        try {
            walletDownloadReportService.generateDailyReport(date);
            return ResponseEntity.ok(CommonResponse.success(
                    "Generated", "Daily wallet history report generated for " + date));
        } catch (Exception e) {
            log.error("Manual daily generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(CommonResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/run-hourly-report")
    public String runHourlyReport() {
        scheduler.generateHourlyReport();
        return "Hourly Report Generated";
    }
}
