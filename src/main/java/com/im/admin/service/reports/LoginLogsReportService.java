package com.im.admin.service.reports;

import com.im.admin.dto.LoginLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface LoginLogsReportService {

    Page<LoginLogsResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate,
                                               Integer srNo, String request, String response,
                                               String ipAddress, LocalDate requestDateTime,
                                               LocalDate responseDateTime, int page, int size);


    ByteArrayResource exportLogsToCSV(LocalDate startDate, LocalDate endDate,
                                      Integer srNo, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                      int page, int size);
}
