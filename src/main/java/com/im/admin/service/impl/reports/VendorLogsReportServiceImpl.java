package com.im.admin.service.impl.reports;

import com.im.admin.dto.VendorLogsResponseDTO;
import com.im.admin.repositories.VendorReqResRepository;
import com.im.admin.service.reports.VendorLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VendorLogsReportServiceImpl implements VendorLogsReportService {

    @Autowired
    private VendorReqResRepository vendorLogsRepo;

    @Override
    public List<VendorLogsResponseDTO> getFilteredVendorLogs(LocalDate start, LocalDate end,
                                                             String vendorId, String requestText, String responseText,
                                                             String ip, LocalDate requestDateTime, LocalDate responseDateTime) {

        List<Object[]> rows = vendorLogsRepo.getVendorLogsBetweenDates(
                start.atStartOfDay(), end.atTime(23, 59, 59)
        );

        return rows.stream().map(row -> {
                    VendorLogsResponseDTO dto = new VendorLogsResponseDTO();
                    dto.setSrNo((Integer) row[0]);
                    dto.setImVendorId((String) row[1]);
                    dto.setRequest((String) row[2]);
                    dto.setResponse((String) row[3]);
                    dto.setRequestIp((String) row[4]);

                    Timestamp reqTime = (Timestamp) row[5];
                    if (reqTime != null) dto.setRequestDateTime(reqTime.toLocalDateTime());

                    Timestamp resTime = (Timestamp) row[6];
                    if (resTime != null) dto.setResponseDateTime(resTime.toLocalDateTime());

                    return dto;
                })
                .filter(r -> vendorId == null || r.getImVendorId().equalsIgnoreCase(vendorId))
                .filter(r -> requestText == null || (r.getRequest() != null && r.getRequest().toLowerCase().contains(requestText.toLowerCase())))
                .filter(r -> responseText == null || (r.getResponse() != null && r.getResponse().toLowerCase().contains(responseText.toLowerCase())))
                .filter(r -> ip == null || (r.getRequestIp() != null && r.getRequestIp().contains(ip)))
                .filter(r -> requestDateTime == null || (r.getRequestDateTime() != null && r.getRequestDateTime().toLocalDate().equals(requestDateTime)))
                .filter(r -> responseDateTime == null || (r.getResponseDateTime() != null && r.getResponseDateTime().toLocalDate().equals(responseDateTime)))
                .collect(Collectors.toList());
    }

    @Override
    public ByteArrayResource exportVendorLogsToCSV(LocalDate start, LocalDate end,
                                                   String vendorId, String requestText, String responseText,
                                                   String ip, LocalDate requestDateTime, LocalDate responseDateTime) {

        List<VendorLogsResponseDTO> logs = getFilteredVendorLogs(start, end, vendorId, requestText, responseText, ip, requestDateTime, responseDateTime);

        if (logs.isEmpty()) return null;

        StringBuilder csv = new StringBuilder("SrNo,VendorId,Request,Response,IP,RequestDateTime,ResponseDateTime\n");
        for (VendorLogsResponseDTO log : logs) {
            csv.append(log.getSrNo()).append(",")
                    .append(log.getImVendorId()).append(",")
                    .append("\"").append(log.getRequest().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(log.getResponse().replace("\"", "\"\"")).append("\",")
                    .append(log.getRequestIp()).append(",")
                    .append(log.getRequestDateTime()).append(",")
                    .append(log.getResponseDateTime()).append("\n");
        }

        return new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
