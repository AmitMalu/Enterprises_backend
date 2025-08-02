package com.im.admin.service.impl.reports;

import com.im.admin.dto.ImLogsResponseDTO;
import com.im.admin.repositories.ImLogsRepository;
import com.im.admin.service.reports.ImLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ImLogsReportServiceImpl implements ImLogsReportService {

    @Autowired
    private ImLogsRepository imLogsRepository;

    @Override
    public Page<ImLogsResponseDTO> getFilteredLogs(LocalDate startDate, LocalDate endDate,
                                                   Integer srNo, String request, String response,
                                                   String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                                   int page, int size) {

        List<Object[]> rows = imLogsRepository.getLogsBetweenDates(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        List<ImLogsResponseDTO> filtered = rows.stream().map(row -> {
                    ImLogsResponseDTO dto = new ImLogsResponseDTO();
                    dto.setSrNo((Integer) row[0]);
                    dto.setRequest((String) row[1]);
                    dto.setResponse((String) row[2]);
                    dto.setIpAddress((String) row[3]);

                    Timestamp reqTs = (Timestamp) row[4];
                    if (reqTs != null) dto.setRequestDateTime(reqTs.toLocalDateTime());

                    Timestamp resTs = (Timestamp) row[5];
                    if (resTs != null) dto.setResponseDateTime(resTs.toLocalDateTime());

                    return dto;
                })
                .filter(r -> srNo == null || r.getSrNo().equals(srNo))
                .filter(r -> request == null || (r.getRequest() != null && r.getRequest().toLowerCase().contains(request.toLowerCase())))
                .filter(r -> response == null || (r.getResponse() != null && r.getResponse().toLowerCase().contains(response.toLowerCase())))
                .filter(r -> ipAddress == null || (r.getIpAddress() != null && r.getIpAddress().contains(ipAddress)))
                .filter(r -> requestDateTime == null || (r.getRequestDateTime() != null && r.getRequestDateTime().toLocalDate().equals(requestDateTime)))
                .filter(r -> responseDateTime == null || (r.getResponseDateTime() != null && r.getResponseDateTime().toLocalDate().equals(responseDateTime)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<ImLogsResponseDTO> paginatedList = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), total);
    }

    @Override
    public ByteArrayResource exportLogsToCSV(LocalDate startDate, LocalDate endDate,
                                             Integer srNo, String request, String response,
                                             String ipAddress, LocalDate requestDateTime, LocalDate responseDateTime,
                                             int page, int size) {

        Page<ImLogsResponseDTO> logs = getFilteredLogs(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (logs.isEmpty()) return null;

        StringBuilder csv = new StringBuilder();
        csv.append("SrNo,Request,Response,IP Address,Request DateTime,Response DateTime\n");

        for (ImLogsResponseDTO log : logs) {
            csv.append(log.getSrNo()).append(",")
                    .append("\"").append(log.getRequest().replace("\"", "\"\"")).append("\",")
                    .append("\"").append(log.getResponse().replace("\"", "\"\"")).append("\",")
                    .append(log.getIpAddress()).append(",")
                    .append(log.getRequestDateTime()).append(",")
                    .append(log.getResponseDateTime()).append("\n");
        }

        return new ByteArrayResource(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
}
