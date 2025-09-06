package com.im.admin.service.reports;


import com.im.admin.entities.HourlyReportEntity;
import jakarta.servlet.http.HttpServletResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface HourlyReportService {

    List<HourlyReportEntity> listReportsByDate(LocalDate date);

    void streamReportDownload(String id, HttpServletResponse response) throws Exception;

    void generateReportsForDate(LocalDate date) throws Exception; // useful to generate all 24 hours on demand

    void generateReportForHour(LocalDateTime startHour) throws Exception; // generate single hour's reports (transaction+wallet)

}

