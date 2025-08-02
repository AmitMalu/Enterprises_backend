package com.im.admin.controller.report;

import com.im.admin.dto.CallbackEventLogResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.CallbackEventLogReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/callback-logs-report")
public class CallbackEventLogReportController {

    @Autowired
    private CallbackEventLogReportService service;

    @PostMapping("/json")
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
        Page<CallbackEventLogResponseDTO> data = service.getFilteredLogs(startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);
        String message = data.isEmpty() ? "No records found" : "Success";
        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }

    @PostMapping("/excel")
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
        ByteArrayResource resource = service.exportLogsToCSV(startDate, endDate, id, request, response, ipAddress, requestDateTime, responseDateTime, page, size);
        if (resource == null) {
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=CallbackEventLogReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
