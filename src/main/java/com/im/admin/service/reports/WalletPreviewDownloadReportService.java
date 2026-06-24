package com.im.admin.service.reports;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface WalletPreviewDownloadReportService {

    void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception;

    void generateDailyReport(LocalDate reportDate) throws Exception;
}
