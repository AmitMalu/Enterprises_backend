package com.im.admin.service.reports;

import java.time.LocalDate;

public interface ChargeCommissionReportService {

    /** Generates full-day Charge & Commission report 00:00 to 23:59:59 */
    void generateDailyReport(LocalDate reportDate) throws Exception;
}
