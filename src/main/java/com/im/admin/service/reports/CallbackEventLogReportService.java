package com.im.admin.service.reports;

import com.im.admin.dto.CallbackEventLogResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface CallbackEventLogReportService {

    Page<CallbackEventLogResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate,
                                                      Integer id, String request, String response,
                                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                                      int page, int size);

    ByteArrayResource exportLogsToCSV(LocalDate startDate, LocalDate endDate,
                                      Integer id, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                      int page, int size);
}
