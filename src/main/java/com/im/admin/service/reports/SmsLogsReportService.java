package com.im.admin.service.reports;

import com.im.admin.dto.SmsLogsResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface SmsLogsReportService {

    Page<SmsLogsResponseDTO> getFilteredLogs(
            LocalDate startDate, LocalDate endDate,
            Long id, String request, String response,
            String ipAddress, LocalDate requestDateTime,
            LocalDate responseDateTime, int page, int size
    );
}
