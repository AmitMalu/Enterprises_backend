package com.im.admin.controller.report;

import com.im.admin.dto.VendorLogsResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.VendorLogsReportService;
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
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Received request for vendor logs | startDate={}, endDate={}, vendorId={}, page={}, size={}",
                startDate, endDate, vendorId, page, size);

        var result = vendorLogsReportService.getFilteredVendorLogs(
                startDate, endDate, vendorId, request, response, ip, srNo, requestDateTime, responseDateTime, page, size);

        String message = result.isEmpty() ? "No records found" : "Success";
        log.info("Vendor logs fetched | totalRecords={}");

        return ResponseEntity.ok(new CommonResponse<>(200, message, result));
    }

    @PostMapping("/vendor-excel")
    public ResponseEntity<?> downloadVendorLogsCSV(
            @RequestParam("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate endDate,
            @RequestParam(value = "vendorId", required = false) String vendorId,
            @RequestParam(value = "request", required = false) String request,
            @RequestParam(value = "response", required = false) String response,
            @RequestParam(value = "ipAddress", required = false) String ip,
            @RequestParam(value = "srNo", required = false) Integer srNo,
            @RequestParam(value = "requestDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate requestDateTime,
            @RequestParam(value = "responseDateTime", required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate responseDateTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Received request to download vendor logs CSV | startDate={}, endDate={}, vendorId={}, page={}, size={}",
                startDate, endDate, vendorId, page, size);

        ByteArrayResource resource = vendorLogsReportService.exportVendorLogsToCSV(
                startDate, endDate, vendorId, request, response, ip, srNo, requestDateTime, responseDateTime, page, size
        );

        if (resource == null) {
            log.info("No vendor logs found for download");
            return ResponseEntity.ok(new CommonResponse<>(200, "No records found", null));
        }

        log.info("Vendor logs CSV generated successfully");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=VendorLogsReport.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
