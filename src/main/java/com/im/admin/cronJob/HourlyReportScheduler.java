package com.im.admin.cronJob;


import com.im.admin.properties.ReportProperties;
import com.im.admin.service.reports.HourlyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class HourlyReportScheduler {

    @Autowired
    private HourlyReportService hourlyReportService;

    @Autowired
    private ReportProperties reportProperties;

    // Run at 5th minute of every hour (adjust if you prefer)
    @Scheduled(cron = "0 5 * * * *")
    public void runHourly() {
        try {
            // generate just the previous hour (so data is likely complete)
            LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
            LocalDateTime prevHourStart = now.minusHours(1);
            hourlyReportService.generateReportForHour(prevHourStart);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Optional: daily job to ensure whole day was generated (e.g., in case of downtime)
    @Scheduled(cron = "0 10 0 * * *") // daily at 00:10
    public void ensureDailyReports() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        try {
            hourlyReportService.generateReportsForDate(yesterday);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

