package com.im.admin.service.impl.reports;

import com.im.admin.dto.EmailLogsResponseDTO;
import com.im.admin.repositories.EmailLogRepository;
import com.im.admin.service.reports.EmailLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailLogsReportServiceImpl implements EmailLogsReportService {

    @Autowired
    private EmailLogRepository emailLogRepository;


    @Override
    public Page<EmailLogsResponseDTO> getFilteredLogs(
            LocalDate start, LocalDate end,
            String request, String response,
            String ipAddress,
            LocalDate requestDateTime,
            LocalDate responseDateTime,
            int page, int size
    ) {
        List<Object[]> rows = emailLogRepository.getEmailLogsBetweenDates(start.atStartOfDay(), end.atTime(23, 59, 59));

        List<EmailLogsResponseDTO> filtered = rows.stream().map(row -> {
                    EmailLogsResponseDTO dto = new EmailLogsResponseDTO();
                    dto.setId(((Number) row[0]).longValue());
                    dto.setRequest((String) row[1]);
                    dto.setResponse((String) row[2]);
                    dto.setIpAddress((String) row[3]);

                    Timestamp reqTs = (Timestamp) row[4];
                    if (reqTs != null) dto.setRequestDateTime(reqTs.toLocalDateTime());

                    Timestamp resTs = (Timestamp) row[5];
                    if (resTs != null) dto.setResponseDateTime(resTs.toLocalDateTime());

                    return dto;
                })
                .filter(r -> request == null || (r.getRequest() != null && r.getRequest().toLowerCase().contains(request.toLowerCase())))
                .filter(r -> response == null || (r.getResponse() != null && r.getResponse().toLowerCase().contains(response.toLowerCase())))
                .filter(r -> ipAddress == null || (r.getIpAddress() != null && r.getIpAddress().contains(ipAddress)))
                .filter(r -> requestDateTime == null || (r.getRequestDateTime() != null && r.getRequestDateTime().toLocalDate().equals(requestDateTime)))
                .filter(r -> responseDateTime == null || (r.getResponseDateTime() != null && r.getResponseDateTime().toLocalDate().equals(responseDateTime)))
                .collect(Collectors.toList());

        int total = filtered.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);

        List<EmailLogsResponseDTO> paginatedList = filtered.subList(fromIndex, toIndex);

        return new PageImpl<>(paginatedList, PageRequest.of(page, size), total);
    }
}
