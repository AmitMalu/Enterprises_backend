package com.scs.admin.controller.report;

import com.scs.admin.dto.HierarchyLogsResponseDTO;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.reports.HierarchyLogsReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/report")
public class HierarchyLogsReportController {

    @Autowired
    private HierarchyLogsReportService hierarchyLogsReportService;

    @PostMapping("/hierarchy-logs-json")
    public ResponseEntity<?> getHierarchyLogsJson(
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
        log.info("Received request to fetch hierarchy logs: startDate={}, endDate={}, srNo={}, ipAddress={}",
                startDate, endDate, srNo, ipAddress);

        Page<HierarchyLogsResponseDTO> data = hierarchyLogsReportService.getFilteredLogs(
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size
        );

        log.info("Hierarchy logs fetched | Records count: {}", data.getTotalElements());

        String message = data.isEmpty() ? "No records found" : "Success";
        return ResponseEntity.ok(new CommonResponse<>(200, message, data));
    }

    @PostMapping("/hie-excel")
    public ResponseEntity<?> downloadHierarchyLogsCSV(
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
        log.info("Received request to download hierarchy logs CSV: startDate={}, endDate={}, srNo={}, ipAddress={}",
                startDate, endDate, srNo, ipAddress);

        ByteArrayResource resource = hierarchyLogsReportService.exportLogsToCSV(
                startDate, endDate, srNo, request, response, ipAddress, requestDateTime, responseDateTime, page, size
        );

        if (resource == null) {
            log.info("No hierarchy log records found for CSV export");
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        log.info("Hierarchy logs CSV generated successfully, size: {} bytes", resource.contentLength());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=HierarchyLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}


