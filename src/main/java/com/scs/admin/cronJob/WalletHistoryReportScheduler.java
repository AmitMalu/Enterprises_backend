package com.scs.admin.cronJob;

import com.scs.admin.service.reports.WalletDownloadReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduler for automatic Wallet History Report generation.
 *
 * Hourly: at minute 0 of every hour (e.g. 01:00, 02:00, ..., 23:00)
 *   → cumulative report 00:00 to HH:00
 *
 * Daily:  at 23:59 every day
 *   → full-day report 00:00 to 23:59:59
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletHistoryReportScheduler {

    private final WalletDownloadReportService walletDownloadReportService;

    /**
     * Fires at the top of every hour (minute=0, second=0).
     * Generates a cumulative wallet history report from 00:00:00 to current hour.
     *
     * Cron: "0 0 1-23 * * *"  → triggers at 01:00, 02:00, ..., 23:00
     */
    @Scheduled(cron = "0 0 1-23 * * *")
    public void generateHourlyReport() {
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDate today = now.toLocalDate();
        log.info("WalletHistoryReportScheduler: generating hourly report for {} up to {}", today, now);
        try {
            walletDownloadReportService.generateHourlyReport(today, now);
        } catch (Exception e) {
            log.error("Failed to generate hourly wallet history report for {}: {}", now, e.getMessage(), e);
        }
    }

    /**
     * Fires at 23:59:00 every day.
     * Generates the full-day wallet history report.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("WalletHistoryReportScheduler: generating daily report for {}", today);
        try {
            walletDownloadReportService.generateDailyReport(today);
        } catch (Exception e) {
            log.error("Failed to generate daily wallet history report for {}: {}", today, e.getMessage(), e);
        }
    }
}
