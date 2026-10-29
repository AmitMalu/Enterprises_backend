package com.scs.admin.service.reports;

import com.scs.admin.dto.ScsLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface ScsLogsReportService {

    Page<ScsLogsResponseDTO> getFilteredLogs(
            LocalDate startDate, LocalDate endDate,
            Integer srNo, String request, String response,
            String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
            int page, int size
    );

    ByteArrayResource exportLogsToCSV(
            LocalDate startDate, LocalDate endDate,
            Integer srNo, String request, String response,
            String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
            int page, int size
    );
}
