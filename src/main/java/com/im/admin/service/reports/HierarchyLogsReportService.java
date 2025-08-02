package com.im.admin.service.reports;

import com.im.admin.dto.HierarchyLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface HierarchyLogsReportService {

    Page<HierarchyLogsResponseDTO> getFilteredLogs(LocalDate start, LocalDate end,
                                                   Integer srNo, String request, String response,
                                                   String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                                   int page, int size);

    ByteArrayResource exportLogsToCSV(LocalDate start, LocalDate end,
                                      Integer srNo, String request, String response,
                                      String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                      int page, int size);
}


