package com.scs.admin.controller.report;


import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.request.reports.NetworkTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.NetworkTransactionReportResponse;
import com.scs.admin.service.reports.NetworkTransactionReportService;
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
@RequestMapping("/api/network-transaction")
@Slf4j
public class NetworkTransactionReportController {

    @Autowired
    private NetworkTransactionReportService networkTransactionReportService;

    @PostMapping("/view")
    public ResponseEntity<CommonResponse<Page<NetworkTransactionReportResponse>>> viewNetworkTransactions(
            @RequestBody NetworkTransactionReportRequest request) {

        log.info("Received request to view NetworkTransaction report: {}", request);

        CommonResponse<Page<NetworkTransactionReportResponse>> response =
                networkTransactionReportService.viewNetworkTransactions(request);

        log.info("Fetched NetworkTransaction report | Total records: {}",
                response.getData() != null ? response.getData().getTotalElements() : 0);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping(value = "/download-report", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    })
    public ResponseEntity<Resource> downloadMyTransactionReport(
            @RequestBody NetworkTransactionReportRequest request,
            @RequestParam("format") String format) throws IOException {

        log.info("Received request to download NetworkTransaction report | format={}", format);

        ByteArrayInputStream fileStream;
        String filename;
        MediaType mediaType;
        String successMessage = "Transaction report generated successfully";

        if ("excel".equalsIgnoreCase(format)) {
            fileStream = networkTransactionReportService.downloadExcel(request);
            filename = "network_transaction_report.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("csv".equalsIgnoreCase(format)) {
            fileStream = networkTransactionReportService.downloadCsv(request);
            filename = "network_transaction_report.csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            log.warn("Invalid format provided for NetworkTransaction report download: {}", format);
            return ResponseEntity.badRequest()
                    .header("X-Status-Message", "Invalid format provided (use 'excel' or 'csv')")
                    .body(null);
        }

        InputStreamResource resource = new InputStreamResource(fileStream);

        log.info("NetworkTransaction report generated successfully | format={}, filename={}", format, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header("X-Status-Message", successMessage)
                .contentType(mediaType)
                .body(resource);
    }
}
