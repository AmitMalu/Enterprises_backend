package com.im.admin.service.reports;

import com.im.admin.dto.VendorLogsResponseDTO;
import org.springframework.core.io.ByteArrayResource;

import java.time.LocalDate;
import java.util.List;

public interface VendorLogsReportService {
    List<VendorLogsResponseDTO> getFilteredVendorLogs(LocalDate start, LocalDate end,
                                                      String vendorId, String requestText, String responseText,
                                                      String ip, LocalDate requestDateTime, LocalDate responseDateTime);

    ByteArrayResource exportVendorLogsToCSV(LocalDate start, LocalDate end,
                                            String vendorId, String requestText, String responseText,
                                            String ip, LocalDate requestDateTime, LocalDate responseDateTime);
}
