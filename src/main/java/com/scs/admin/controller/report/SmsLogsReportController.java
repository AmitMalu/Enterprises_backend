package com.scs.admin.controller.report;

import com.scs.admin.dto.SmsLogsResponseDTO;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.reports.SmsLogsReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class SmsLogsReportController {

    @Autowired
    private SmsLogsReportService smsLogsReportService;

    @PostMapping("/sms-logs-json")
    public ResponseEntity<?> getSmsLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "id", required = false) Long id,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Received request to fetch SMS logs: startDate={}, endDate={}", startDate, endDate);

        Page<SmsLogsResponseDTO> data = smsLogsReportService.getFilteredLogs(
                startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size
        );

        String message = data.isEmpty() ? "No records found" : "Success";
        log.info("Fetched {} SMS log records", data.getTotalElements());

        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }
}
