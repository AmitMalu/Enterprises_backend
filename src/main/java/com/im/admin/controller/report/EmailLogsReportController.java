package com.im.admin.controller.report;

import com.im.admin.dto.EmailLogsResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.EmailLogsReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class EmailLogsReportController {

    @Autowired
    private EmailLogsReportService emailLogsReportService;

    @PostMapping("/email-logs-json")
    public ResponseEntity<?> getEmailLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching Email Logs | startDate={}, endDate={}, ip={}", startDate, endDate, ipAddress);

        Page<EmailLogsResponseDTO> data = emailLogsReportService.getFilteredLogs(
                startDate, endDate, request, response, ipAddress, requestDateTime, responseDateTime, page, size
        );

        String message = data.isEmpty() ? "No records found" : "Success";
        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }
}