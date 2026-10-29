package com.scs.admin.controller.report;

import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.MyTransactionReportResponse;
import com.scs.admin.service.reports.MyTransactionReportService;
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
@RequestMapping("/api/my-transaction")
@Slf4j
public class MyTransactionReportController {

    @Autowired
    private MyTransactionReportService myTransactionReportService;

    @PostMapping("/view")
    public ResponseEntity<CommonResponse<Page<MyTransactionReportResponse>>> getReport(
            @RequestBody MyTransactionReportRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        log.info("Received request to fetch MyTransaction report: {}", request);

        CommonResponse<Page<MyTransactionReportResponse>> response = myTransactionReportService.getReport(request, page, size);

        log.info("MyTransaction report fetched | Records count: {}",
                response.getData() != null ? response.getData().getTotalElements() : 0);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


    @PostMapping(value = "/download-report", produces = {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/csv"
    })
    public ResponseEntity<Resource> downloadMyTransactionReport(
            @RequestBody MyTransactionReportRequest request,
            @RequestParam("format") String format) throws IOException {

        log.info("Received request to download MyTransaction report | format={}", format);

        ByteArrayInputStream fileStream;
        String filename;
        MediaType mediaType;
        String successMessage = "Transaction report generated successfully";

        if ("excel".equalsIgnoreCase(format)) {
            fileStream = myTransactionReportService.downloadExcel(request);
            filename = "my_transaction_report.xlsx";
            mediaType = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        } else if ("csv".equalsIgnoreCase(format)) {
            fileStream = myTransactionReportService.downloadCsv(request);
            filename = "my_transaction_report.csv";
            mediaType = MediaType.parseMediaType("text/csv");
        } else {
            log.warn("Invalid format provided for MyTransaction report download: {}", format);
            return ResponseEntity.badRequest()
                    .header("X-Status-Message", "Invalid format provided (use 'excel' or 'csv')")
                    .body(null);
        }

        InputStreamResource resource = new InputStreamResource(fileStream);

        log.info("MyTransaction report generated successfully, format={}, filename={}", format, filename);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .header("X-Status-Message", successMessage)
                .contentType(mediaType)
                .contentLength(fileStream.available())
                .body(resource);
    }


}
