package com.im.admin.service.reports;

import com.im.admin.dto.ImLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ImLogsReportService {

    List<ImLogsResponseDTO> getFilteredLogs(
            LocalDate startDate, LocalDate endDate,
            Integer srNo, String request, String response,
            String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime
    );

    ByteArrayResource exportLogsToCSV(
            LocalDate startDate, LocalDate endDate,
            Integer srNo, String request, String response,
            String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime
    );
}
