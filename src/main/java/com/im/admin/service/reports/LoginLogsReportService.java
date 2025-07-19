package com.im.admin.service.reports;

import com.im.admin.dto.LoginLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDate;
import java.util.List;

public interface LoginLogsReportService {

    List<LoginLogsResponseDTO> getFilteredLogs(LocalDate start, LocalDate end,
                                               Integer srNo, String request, String response,
                                               String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime);

    ByteArrayResource exportLogsToCSV(LocalDate start, LocalDate end,
                                      Integer srNo, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime);
}
