package com.im.admin.controller.report;


import com.im.admin.request.reports.CommissionTransactionReportRequest;
import com.im.admin.response.reports.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.CommissionChargeTransactionReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/api/commission-charge")
@Slf4j
public class CommissionChargeTransactionReportController {

    @Autowired
    private CommissionChargeTransactionReportService commissionChargeTransactionReportService;

    @PostMapping("/view")
    public ResponseEntity<CommonResponse<Page<CommissionTransactionReportResponse>>> getReport(
            @RequestBody CommissionTransactionReportRequest request) {

        log.info("Received request to fetch commission transaction report: {}", request);

        CommonResponse<Page<CommissionTransactionReportResponse>> response =
                commissionChargeTransactionReportService.getReport(request);

        log.info("Commission report fetched with status: {} | Records count: {}",
                response.getStatusCode(),
                response.getData() != null ? response.getData().getTotalElements() : 0);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping(value = "/download-report", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    })
    public ResponseEntity<Resource> downloadReport(
            @RequestBody CommissionTransactionReportRequest request,
            @RequestParam("format") String format) throws IOException {

        log.info("Received request to download commission report: {} | format: {}", request, format);

        ByteArrayInputStream fileStream;
        String filename;
        MediaType mediaType;
        String successMessage = "Report generated successfully";

        if ("excel".equalsIgnoreCase(format)) {
            fileStream = commissionChargeTransactionReportService.downloadExcel(request);
            filename = "commission_report.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            log.info("Excel report generated successfully: {}", filename);
        } else if ("csv".equalsIgnoreCase(format)) {
            fileStream = commissionChargeTransactionReportService.downloadCsv(request);
            filename = "commission_report.csv";
            mediaType = MediaType.parseMediaType("text/csv");
            log.info("CSV report generated successfully: {}", filename);
        } else {
            log.warn("Invalid format requested for commission report: {}", format);
            return ResponseEntity.badRequest()
                    .header("X-Status-Message", "Invalid format provided (use 'excel' or 'csv')")
                    .body(null);
        }

        InputStreamResource resource = new InputStreamResource(fileStream);

        log.info("Returning commission report response: {}", filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header("X-Status-Message", successMessage) // custom message header
                .contentType(mediaType)
                .body(resource);
    }

}

