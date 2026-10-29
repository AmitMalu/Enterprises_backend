package com.scs.admin.service.reports;

import com.scs.admin.dto.VendorLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;

import java.time.LocalDate;

public interface VendorLogsReportService {

    Page<VendorLogsResponseDTO> getFilteredVendorLogs(LocalDate start, LocalDate end,
                                                      String vendorId, String requestText, String responseText,
                                                      String ip, Integer srNo,
                                                      LocalDate requestDateTime, LocalDate responseDateTime,
                                                      int page, int size);

    ByteArrayResource exportVendorLogsToCSV(LocalDate start, LocalDate end,
                                            String vendorId, String requestText, String responseText,
                                            String ip, Integer srNo,
                                            LocalDate requestDateTime, LocalDate responseDateTime,
                                            int page, int size);
}
