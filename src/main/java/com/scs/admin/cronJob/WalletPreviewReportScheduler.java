package com.scs.admin.cronJob;

import com.scs.admin.service.reports.WalletPreviewDownloadReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduler for automatic Wallet Preview Report generation.
 *
 * Hourly: at top of every hour (01:00 – 23:00)
 *   → cumulative wallet preview report from 00:00 to HH:00
 *
 * Daily: at 23:59 every day
 *   → full-day wallet preview report
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletPreviewReportScheduler {

    private final WalletPreviewDownloadReportService walletPreviewDownloadReportService;

    /**
     * Fires at the top of every hour from 01:00 to 23:00.
     */
    @Scheduled(cron = "0 0 1-23 * * *")
    public void generateHourlyReport() {
        LocalDateTime now   = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDate     today = now.toLocalDate();
        log.info("WalletPreviewReportScheduler: generating hourly report for {} up to {}", today, now);
        try {
            walletPreviewDownloadReportService.generateHourlyReport(today, now);
        } catch (Exception e) {
            log.error("Failed to generate hourly wallet preview report for {}: {}", now, e.getMessage(), e);
        }
    }

    /**
     * Fires at 23:59:00 every day — generates full-day wallet preview report.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("WalletPreviewReportScheduler: generating daily report for {}", today);
        try {
            walletPreviewDownloadReportService.generateDailyReport(today);
        } catch (Exception e) {
            log.error("Failed to generate daily wallet preview report for {}: {}", today, e.getMessage(), e);
        }
    }
}
