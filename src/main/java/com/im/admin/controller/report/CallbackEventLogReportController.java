package com.im.admin.controller.report;

import com.im.admin.dto.CallbackEventLogResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.CallbackEventLogReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class CallbackEventLogReportController {

    @Autowired
    private CallbackEventLogReportService service;

    @PostMapping("/callback-logs-json")
    public ResponseEntity<?> getCallbackLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching callback logs JSON from {} to {} | id={} | request={} | response={} | ip={} | reqDate={} | resDate={} | page={} | size={}",
                startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        Page<CallbackEventLogResponseDTO> data = service.getFilteredLogs(
                startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        String message = data.isEmpty() ? "No records found" : "Success";
        log.info("Callback logs JSON fetched, {} records found", data.getTotalElements());

        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }

    @PostMapping("/call-excel")
    public ResponseEntity<?> downloadCallbackLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "id", required = false) Integer id,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Downloading callback logs CSV from {} to {} | id={} | request={} | response={} | ip={} | reqDate={} | resDate={} | page={} | size={}",
                startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        ByteArrayResource resource = service.exportLogsToCSV(
                startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);

        if (resource == null) {
            log.warn("No callback log records found to export CSV");
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        log.info("Callback logs CSV generated successfully, size: {} bytes", resource.contentLength());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=CallbackEventLogReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }

}
