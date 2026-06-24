package com.im.admin.cronJob;

import com.im.admin.service.reports.TransactionDownloadReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Scheduler for automatic Transaction Download Report generation (TXN only).
 *
 * Hourly: fires at top of every hour (01:00 – 23:00)
 *   → cumulative TXN report from 00:00 to HH:00
 *
 * Daily: fires at 23:59 every day
 *   → full-day TXN report
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionReportScheduler {

    private final TransactionDownloadReportService transactionDownloadReportService;

    /**
     * Fires at the top of every hour from 01:00 to 23:00.
     */
    @Scheduled(cron = "0 0 1-23 * * *")
    public void generateHourlyReport() {
        LocalDateTime now   = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalDate     today = now.toLocalDate();
        log.info("TransactionReportScheduler: generating hourly report for {} up to {}", today, now);
        try {
            transactionDownloadReportService.generateHourlyReport(today, now);
        } catch (Exception e) {
            log.error("Failed to generate hourly transaction report for {}: {}", now, e.getMessage(), e);
        }
    }

    /**
     * Fires at 23:59:00 every day — full-day TXN report.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("TransactionReportScheduler: generating daily report for {}", today);
        try {
            transactionDownloadReportService.generateDailyReport(today);
        } catch (Exception e) {
            log.error("Failed to generate daily transaction report for {}: {}", today, e.getMessage(), e);
        }
    }
}
