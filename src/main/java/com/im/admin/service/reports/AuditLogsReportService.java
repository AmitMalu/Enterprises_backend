package com.im.admin.service.reports;

import com.im.admin.dto.AuditLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface AuditLogsReportService {

    Page<AuditLogsResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate,
                                               Integer srNo, String request, String response,
                                               String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                               int page, int size);

    ByteArrayResource exportLogsToCSV(LocalDate startDate, LocalDate endDate,
                                      Integer srNo, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                      int page, int size);
}
