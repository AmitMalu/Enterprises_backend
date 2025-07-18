package com.im.admin.controller.report;

import com.im.admin.dto.VendorLogsResponseDTO;
import com.im.admin.service.reports.VendorLogsReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class VendorLogsReportController {

    @Autowired
    private VendorLogsReportService vendorLogsReportService;

    @PostMapping("/vendor-logs-json")
    public ResponseEntity<?> getVendorLogsJson(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "vendorId", required = false) String vendorId,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ip,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime
    ) {
        List<VendorLogsResponseDTO> data = vendorLogsReportService.getFilteredVendorLogs(
                startDate, endDate, vendorId, request, response, ip,srNo, requestDateTime, responseDateTime
        );

        if (data.isEmpty()) {
            Map<String, Object> res = new HashMap<>();
            res.put("statusCode", 200);
            res.put("message", "No records found");
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.ok(data);
    }

    @PostMapping("/vendor-logs-excel")
    public ResponseEntity<?> downloadVendorLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "vendorId", required = false) String vendorId,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ip,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime
    ) {
        ByteArrayResource resource = vendorLogsReportService.exportVendorLogsToCSV(
                startDate, endDate, vendorId, request, response, ip,srNo, requestDateTime, responseDateTime
        );

        if (resource == null) {
            Map<String, Object> res = new HashMap<>();
            res.put("statusCode", 200);
            res.put("message", "No records found");
            return ResponseEntity.ok(res);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VendorLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
