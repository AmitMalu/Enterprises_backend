package com.scs.admin.cronJob;

import com.scs.admin.service.reports.ChargeCommissionReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Scheduler for automatic Charge & Commission Report generation.
 *
 * Daily: fires at 23:59 every day
 *   → full-day Charge & Commission report (CHG + COM transactions)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChargeCommissionReportScheduler {

    private final ChargeCommissionReportService chargeCommissionReportService;

    /**
     * Fires at 23:59:00 every day.
     * Generates the full-day Charge & Commission report.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        log.info("ChargeCommissionReportScheduler: generating daily report for {}", today);
        try {
            chargeCommissionReportService.generateDailyReport(today);
        } catch (Exception e) {
            log.error("Failed to generate daily charge & commission report for {}: {}", today, e.getMessage(), e);
        }
    }
}
