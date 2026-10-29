package com.scs.admin.service.reports;


import com.scs.admin.entities.HourlyReportEntity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface HourlyReportService {

    Page<HourlyReportEntity> listReportsByDate(LocalDate date, Pageable pageable);


    void streamReportDownload(String id, HttpServletResponse response) throws Exception;

    void generateReportsForDate(LocalDate date) throws Exception; // useful to generate all 24 hours on demand

    void generateReportForHour(LocalDateTime startHour) throws Exception; // generate single hour's reports (transaction+wallet)

}

