package com.im.admin.controller.report;

import com.im.admin.entities.enums.WalletReportType;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.WalletDownloadReportResponse;
import com.im.admin.service.reports.WalletDownloadReportService;
import com.im.admin.service.reports.WalletPreviewDownloadReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Download Report screen APIs for Wallet Preview Reports.
 *
 * Base URL: /api/reports/wallet-preview
 *
 * Reuses the same download_report table (differentiated by reportType = WALLET_PREVIEW).
 * Reuses the same download endpoint from WalletDownloadReportController.
 */
@Slf4j
@RestController
@RequestMapping("/api/reports/wallet-preview")
@RequiredArgsConstructor
public class WalletPreviewDownloadReportController {

    private final WalletPreviewDownloadReportService walletPreviewDownloadReportService;

    // WalletDownloadReportService is reused for list + download
    // (it already supports filtering by reportType)
    private final WalletDownloadReportService walletDownloadReportService;

    // -------------------------------------------------------------------------
    // List APIs
    // -------------------------------------------------------------------------

    /**
     * GET /api/reports/wallet-preview/list?page=0&size=10
     * Returns paginated list of all WALLET_PREVIEW reports, latest first.
     */
    @GetMapping("/list")
    public ResponseEntity<CommonResponse<Page<WalletDownloadReportResponse>>> listReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: listWalletPreviewReports | page={} size={}", page, size);
        return ResponseEntity.ok(
                walletDownloadReportService.listReportsByType(WalletReportType.WALLET_PREVIEW, page, size));
    }

    /**
     * GET /api/reports/wallet-preview/list/by-date?date=2026-06-23&page=0&size=10
     * Returns paginated list of WALLET_PREVIEW reports filtered by date.
     */
    @GetMapping("/list/by-date")
    public ResponseEntity<CommonResponse<Page<WalletDownloadReportResponse>>> listByDate(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("API: listWalletPreviewReportsByDate | date={} page={} size={}", date, page, size);
        return ResponseEntity.ok(
                walletDownloadReportService.listReportsByTypeAndDate(WalletReportType.WALLET_PREVIEW, date, page, size));
    }

    // -------------------------------------------------------------------------
    // Download — reuses the same download endpoint logic
    // -------------------------------------------------------------------------

    /**
     * GET /api/reports/wallet-preview/download/{id}
     * Streams the Excel file for the given report ID.
     * (same logic as /api/reports/wallet-history/download/{id})
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        log.info("API: downloadWalletPreviewReport | id={}", id);
        try {
            walletDownloadReportService.downloadById(id, response);
        } catch (Exception e) {
            log.error("Error downloading wallet preview report id={}: {}", id, e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // -------------------------------------------------------------------------
    // Manual trigger endpoints (admin use)
    // -------------------------------------------------------------------------

    /**
     * POST /api/reports/wallet-preview/generate/hourly?date=2026-06-23&hour=3
     */
    @PostMapping("/generate/hourly")
    public ResponseEntity<CommonResponse<String>> generateHourly(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam int hour) {

        log.info("API: manual generate hourly wallet preview report | date={} hour={}", date, hour);
        try {
            walletPreviewDownloadReportService.generateHourlyReport(date, date.atTime(hour, 0, 0));
            return ResponseEntity.ok(CommonResponse.success(
                    "Generated",
                    "Hourly wallet preview report generated for " + date + " hour=" + hour));
        } catch (Exception e) {
            log.error("Manual hourly wallet preview generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(CommonResponse.error(500, e.getMessage()));
        }
    }

    /**
     * POST /api/reports/wallet-preview/generate/daily?date=2026-06-23
     */
    @PostMapping("/generate/daily")
    public ResponseEntity<CommonResponse<String>> generateDaily(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        log.info("API: manual generate daily wallet preview report | date={}", date);
        try {
            walletPreviewDownloadReportService.generateDailyReport(date);
            return ResponseEntity.ok(CommonResponse.success(
                    "Generated",
                    "Daily wallet preview report generated for " + date));
        } catch (Exception e) {
            log.error("Manual daily wallet preview generation failed: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(CommonResponse.error(500, e.getMessage()));
        }
    }
}
