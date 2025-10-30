package com.im.admin.controller.report;



import com.im.admin.dto.LedgerReportDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.LedgerReportService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/ledger-report")
@RequiredArgsConstructor
public class LedgerReportController {

    @Autowired
    private LedgerReportService ledgerReportService;

    @GetMapping("/view")
    public ResponseEntity<CommonResponse<Page<LedgerReportDto>>> getLedgerReports(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String transactionId,
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String dateOfTransaction,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String timeOfTransaction,
            @RequestParam(required = false) BigDecimal balBeforeTran,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) BigDecimal balAfterTran,
            @RequestParam(required = false) BigDecimal finalBalance,
            @RequestParam(required = false) String tranStatus,
            @RequestParam(required = false) String actionOnBalanceAmount,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String mobileNo,
            @RequestParam(required = false) String subProductName,
            @RequestParam(required = false) String remarks,
            @RequestParam(required = false) String narration,
            @RequestParam(required = false) String sourceTable,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        Page<LedgerReportDto> reports = ledgerReportService.getLedgerReports(
                id, transactionId, hierarchyId, parseDate(dateOfTransaction), fromDate, toDate, timeOfTransaction,
                balBeforeTran, amount, balAfterTran, finalBalance,
                tranStatus, actionOnBalanceAmount, service, mobileNo,
                subProductName, remarks, narration, sourceTable, pageable
        );

        return ResponseEntity.ok(CommonResponse.success(reports, "Ledger reports fetched successfully"));
    }


    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadLedgerReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) throws IOException {

        List<LedgerReportDto> reports = ledgerReportService.getLedgerReportsForDownload(fromDate, toDate);

        // Create Excel
        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Ledger Report");

        int rowIdx = 0;
        Row headerRow = sheet.createRow(rowIdx++);
        String[] headers = {"Transaction ID", "Hierarchy ID", "Date", "Time",
                "Balance Before", "Amount", "Balance After", "Final Balance",
                "Status", "Action", "Service", "Mobile No", "SubProduct",
                "Remarks", "Narration", "Source Table"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        for (LedgerReportDto dto : reports) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(dto.getTransactionId());
            row.createCell(1).setCellValue(dto.getHierarchyId());
            row.createCell(2).setCellValue(dto.getDateOfTransaction().toString());
            row.createCell(3).setCellValue(dto.getTimeOfTransaction().toString());
            row.createCell(4).setCellValue(dto.getBalBeforeTran().toString());
            row.createCell(5).setCellValue(dto.getAmount().toString());
            row.createCell(6).setCellValue(dto.getBalAfterTran().toString());
            row.createCell(7).setCellValue(dto.getFinalBalance().toString());
            row.createCell(8).setCellValue(dto.getTranStatus());
            row.createCell(9).setCellValue(dto.getActionOnBalanceAmount());
            row.createCell(10).setCellValue(dto.getService());
            row.createCell(11).setCellValue(dto.getMobileNo());
            row.createCell(12).setCellValue(dto.getSubProductName());
            row.createCell(13).setCellValue(dto.getRemarks());
            row.createCell(14).setCellValue(dto.getNarration());
            row.createCell(15).setCellValue(dto.getSourceTable());
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        HttpHeaders headersHttp = new HttpHeaders();
        headersHttp.setContentDispositionFormData("attachment", "Ledger_Report_" + fromDate + "-" + toDate + ".xlsx");
        headersHttp.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return ResponseEntity.ok()
                .headers(headersHttp)
                .body(bos.toByteArray());
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isEmpty()) return null;
        try {
            return LocalDate.parse(date, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        } catch (Exception e) {
            try {
                return LocalDate.parse(date);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid date format. Use dd-MM-yyyy or yyyy-MM-dd");
            }
        }


    }
}
