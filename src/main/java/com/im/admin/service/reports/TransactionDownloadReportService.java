package com.im.admin.service.reports;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface TransactionDownloadReportService {

    /** Generates cumulative hourly TXN report from 00:00 to hourEnd */
    void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception;

    /** Generates full-day TXN report 00:00 to 23:59:59 */
    void generateDailyReport(LocalDate reportDate) throws Exception;
}
