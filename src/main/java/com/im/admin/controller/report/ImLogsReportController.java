package com.im.admin.controller.report;

import com.im.admin.dto.ImLogsResponseDTO;
import com.im.admin.service.reports.ImLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/report")
public class ImLogsReportController {

    @Autowired
    private ImLogsReportService imLogsReportService;


    @PostMapping("/logs-json")
    public ResponseEntity<?> getLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime
    ) {
        List<ImLogsResponseDTO> data = imLogsReportService.getFilteredLogs(
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime
        );

        if (data.isEmpty()) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", 200);
            responseMap.put("message", "No records found");
            return ResponseEntity.ok(responseMap);
        }

        return ResponseEntity.ok(data);
    }

    @PostMapping("/logs-excel")
    public ResponseEntity<?> downloadLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ipAddress,
            @RequestParam(value = "requestDateTime", required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false)
            @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime
    ) {
        ByteArrayResource resource = imLogsReportService.exportLogsToCSV(
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime
        );

        if (resource == null) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("statusCode", 200);
            responseMap.put("message", "No records found");
            return ResponseEntity.ok(responseMap);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ImLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
