package com.im.admin.service.reports;

import com.im.admin.dto.CallbackEventLogResponseDTO;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDate;
import java.util.List;

public interface CallbackEventLogReportService {

    List<CallbackEventLogResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate,
                                                      Integer id, String request, String response,
                                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime);

    ByteArrayResource exportLogsToCSV(LocalDate startDate, LocalDate endDate,
                                      Integer id, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime);
}
