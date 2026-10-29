package com.scs.admin.service.impl.reports;

import com.scs.admin.request.reports.MyTransactionReportRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.reports.MyTransactionReportResponse;
import com.scs.admin.service.reports.MyTransactionReportService;
import com.scs.admin.service.reports.TransactionReportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Transaction Report Service — returns only TXN% transactions.
 *
 * Delegates to MyTransactionReportService for the actual SP call,
 * then filters results to keep only records where tranxId starts with "TXN".
 */
@Slf4j
@Service
public class TransactionReportServiceImpl implements TransactionReportService {

    @Autowired
    private MyTransactionReportService myTransactionReportService;

    // -------------------------------------------------------------------------
    // View (paginated) — TXN only
    // -------------------------------------------------------------------------

    @Override
    public CommonResponse<Page<MyTransactionReportResponse>> getReport(
            MyTransactionReportRequest request, int page, int size) {

        log.info("TransactionReportService.getReport | page={} size={} request={}", page, size, request);

        // Fetch a larger slice from the underlying service so we have enough
        // TXN records to fill the requested page after filtering.
        // Strategy: fetch size * 10 rows, filter, then manually paginate.
        // This is acceptable because the SP already filters by date/hierarchy.
        int fetchSize = Math.max(size * 10, 200);

        CommonResponse<Page<MyTransactionReportResponse>> raw =
                myTransactionReportService.getReport(request, 0, fetchSize);

        if (raw.getData() == null) {
            return new CommonResponse<>(200, "No data", Page.empty());
        }

        // ★ Filter: only TXN transactions
        List<MyTransactionReportResponse> txnOnly = raw.getData()
                .getContent()
                .stream()
//                .filter(r -> r.getTranxId() != null && r.getTranxId().startsWith("TXN"))
                .filter(r -> r.getTranxId() != null &&
                        (r.getTranxId().startsWith("TXN") ||
                                r.getTranxId().startsWith("REVTXN")))
                .toList();

        log.info("TransactionReportService: total={} txnOnly={}",
                raw.getData().getTotalElements(), txnOnly.size());

        // Manual pagination on the filtered list
        int start = page * size;
        int end   = Math.min(start + size, txnOnly.size());

        List<MyTransactionReportResponse> paged =
                (start >= txnOnly.size()) ? List.of() : txnOnly.subList(start, end);

        Page<MyTransactionReportResponse> resultPage =
                new PageImpl<>(paged, PageRequest.of(page, size), txnOnly.size());

        return new CommonResponse<>(200, "Transaction report fetched successfully", resultPage);
    }

    // -------------------------------------------------------------------------
    // Download Excel — TXN only
    // -------------------------------------------------------------------------

    @Override
    public ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException {

        log.info("TransactionReportService.downloadExcel");

        // Fetch all records (no pagination for download)
        request.setSize(Integer.MAX_VALUE);
        CommonResponse<Page<MyTransactionReportResponse>> raw =
                myTransactionReportService.getReport(request, 0, Integer.MAX_VALUE);

        List<MyTransactionReportResponse> txnOnly = List.of();
        if (raw.getData() != null) {
            txnOnly = raw.getData()
                    .getContent()
                    .stream()
                    .filter(r -> r.getTranxId() != null && r.getTranxId().startsWith("TXN"))
                    .toList();
        }

        log.info("TransactionReportService.downloadExcel: txnOnly records = {}", txnOnly.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transaction Report");

            Row header = sheet.createRow(0);
            String[] columns = {
                "Dated", "Transaction ID", "Hierarchy ID",
                "Balance Before", "Amount", "Balance After",
                "Action Type", "Status", "SubProduct", "Product",
                "Scheme", "Narration", "Wallet Type",
                "Vendor Name", "Vendor Txn ID"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (MyTransactionReportResponse tx : txnOnly) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(
                        tx.getTransactionDate() + " " + tx.getTransactionTime());
                row.createCell(1).setCellValue(nullSafe(tx.getTranxId()));
                row.createCell(2).setCellValue(nullSafe(tx.getHierarchyId()));
                row.createCell(3).setCellValue(
                        tx.getBalBefore() != null ? tx.getBalBefore().toPlainString() : "");
                row.createCell(4).setCellValue(
                        tx.getAmount() != null ? tx.getAmount().toPlainString() : "");
                row.createCell(5).setCellValue(
                        tx.getBalAfter() != null ? tx.getBalAfter().toPlainString() : "");
                row.createCell(6).setCellValue(nullSafe(tx.getActionOnBalanceAmount()));
                row.createCell(7).setCellValue(
                        tx.getStatus() != null ? tx.getStatus().name() : "");
                row.createCell(8).setCellValue(nullSafe(tx.getSubProductName()));
                row.createCell(9).setCellValue(nullSafe(tx.getProductName()));
                row.createCell(10).setCellValue(nullSafe(tx.getSchemeName()));
                row.createCell(11).setCellValue(nullSafe(tx.getNarration()));
                row.createCell(12).setCellValue(
                        tx.getWalletType() != null ? tx.getWalletType().name() : "");
                row.createCell(13).setCellValue(nullSafe(tx.getVendorName()));
                row.createCell(14).setCellValue(nullSafe(tx.getVendorTransactionId()));
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    private String nullSafe(String s) {
        return s != null ? s : "";
    }
}
