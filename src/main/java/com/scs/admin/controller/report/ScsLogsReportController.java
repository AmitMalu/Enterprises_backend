package com.scs.admin.controller.report;

import com.scs.admin.dto.ScsLogsResponseDTO;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.reports.ScsLogsReportService;
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
public class ScsLogsReportController {

    @Autowired
    private ScsLogsReportService scsLogsReportService;

    @PostMapping("/scs-logs-json")
    public ResponseEntity<?> getLogsJson(
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
        log.info("Received request to fetch SCS logs: startDate={}, endDate={}, srNo={}, ipAddress={}",
                startDate, endDate, srNo, ipAddress);

        var data = scsLogsReportService.getFilteredLogs(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        log.info("SCS logs fetched | Records count: {}");

        String message = data.isEmpty() ? "No records found" : "Success";
        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }

    @PostMapping("/excel")
    public ResponseEntity<?> downloadLogsCSV(
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
        log.info("Received request to download SCS logs CSV: startDate={}, endDate={}, srNo={}, ipAddress={}",
                startDate, endDate, srNo, ipAddress);

        ByteArrayResource resource = scsLogsReportService.exportLogsToCSV(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (resource == null) {
            log.info("No SCS log records found for CSV export");
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        log.info("SCS logs CSV generated successfully, size: {} bytes", resource.contentLength());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ScsLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

}
