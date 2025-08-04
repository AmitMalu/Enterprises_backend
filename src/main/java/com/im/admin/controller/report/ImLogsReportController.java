package com.im.admin.controller.report;

import com.im.admin.dto.ImLogsResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.ImLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/report")
public class ImLogsReportController {

    @Autowired
    private ImLogsReportService imLogsReportService;

    @PostMapping("/im-logs-json")
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
        var data = imLogsReportService.getFilteredLogs(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);
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
        ByteArrayResource resource = imLogsReportService.exportLogsToCSV(startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size);
        if (resource == null) {
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ImLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
