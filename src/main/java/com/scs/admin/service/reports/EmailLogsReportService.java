package com.scs.admin.service.reports;

import com.scs.admin.dto.EmailLogsResponseDTO;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface EmailLogsReportService {
    Page<EmailLogsResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate, String request, String response, String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime, int page, int size);
}
