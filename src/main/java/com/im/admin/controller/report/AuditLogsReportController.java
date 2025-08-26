package com.im.admin.controller.report;

import com.im.admin.dto.AuditLogsResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.AuditLogsReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class AuditLogsReportController {

    @Autowired
    private AuditLogsReportService service;

    @PostMapping("/audit-logs-json")
    public ResponseEntity<?> getAuditLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching audit logs JSON from {} to {} | srNo={} | request={} | response={} | ip={} | reqDate={} | resDate={} | page={} | size={}",
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        var data = service.getFilteredLogs(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        String message = data.isEmpty() ? "No records found" : "Success";
        log.info("Audit logs JSON fetched, {} records found", data.size());

        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }

    @PostMapping("/download-excel")
    public ResponseEntity<?> downloadAuditLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Downloading audit logs CSV from {} to {} | srNo={} | request={} | response={} | ip={} | reqDate={} | resDate={} | page={} | size={}",
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        ByteArrayResource resource = service.exportLogsToCSV(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (resource == null) {
            log.warn("No audit log records found to export CSV");
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        log.info("Audit logs CSV generated successfully, size: {} bytes", resource.contentLength());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=AuditLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
