package com.scs.admin.controller.report;

import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.MyTransactionReportResponse;
import com.scs.admin.service.reports.TransactionReportService;
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

/**
 * Transaction Report Controller — TXN transactions only.
 *
 * Mirrors MyTransactionReportController but filters to
 * transaction_id LIKE 'TXN%' only.
 *
 * Base URL: /api/transaction-report
 */
@RestController
@RequestMapping("/api/transaction-report")
@Slf4j
public class TransactionReportController {

    @Autowired
    private TransactionReportService transactionReportService;

    /**
     * POST /api/transaction-report/view
     *
     * Same request body as POST /api/my-transaction/view
     * but returns only TXN% transactions with pagination.
     */
    @PostMapping("/view")
    public ResponseEntity<CommonResponse<Page<MyTransactionReportResponse>>> getReport(
            @RequestBody MyTransactionReportRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("TransactionReportController.view | page={} size={} request={}", page, size, request);

        CommonResponse<Page<MyTransactionReportResponse>> response =
                transactionReportService.getReport(request, page, size);

        log.info("Transaction report fetched | TXN records: {}",
                response.getData() != null ? response.getData().getTotalElements() : 0);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    /**
     * POST /api/transaction-report/download-report?format=excel
     *
     * Downloads all TXN transactions as Excel.
     * Same format as /api/my-transaction/download-report.
     */
    @PostMapping(value = "/download-report",
            produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<Resource> downloadReport(
            @RequestBody MyTransactionReportRequest request,
            @RequestParam(defaultValue = "excel") String format) throws IOException {

        log.info("TransactionReportController.downloadReport | format={}", format);

        if (!"excel".equalsIgnoreCase(format)) {
            return ResponseEntity.badRequest()
                    .header("X-Status-Message", "Only 'excel' format is supported")
                    .body(null);
        }

        ByteArrayInputStream fileStream = transactionReportService.downloadExcel(request);
        InputStreamResource resource = new InputStreamResource(fileStream);

        log.info("Transaction report Excel generated successfully");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=transaction_report.xlsx")
                .header("X-Status-Message", "Transaction report generated successfully")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(fileStream.available())
                .body(resource);
    }
}
