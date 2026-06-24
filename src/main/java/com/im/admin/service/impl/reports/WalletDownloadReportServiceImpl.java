package com.im.admin.service.impl.reports;

import com.im.admin.entities.WalletDownloadReport;
import com.im.admin.entities.enums.WalletReportFrequency;
import com.im.admin.entities.enums.WalletReportType;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.properties.ReportProperties;
import com.im.admin.repositories.WalletDownloadReportRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.MyTransactionReportResponse;
import com.im.admin.response.reports.WalletDownloadReportResponse;
import com.im.admin.service.reports.WalletDownloadReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.sql.Date;
import java.sql.Time;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletDownloadReportServiceImpl implements WalletDownloadReportService {

    @Autowired
    private ReportProperties reportProperties;

    @Autowired
    private WalletDownloadReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    private static final DateTimeFormatter DATE_FMT   = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FMT   = DateTimeFormatter.ofPattern("yyyyMMdd_HH");
    private static final String            SUB_FOLDER = "wallet-history";

    // -------------------------------------------------------------------------
    // Generate hourly cumulative report
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping hourly wallet history report generation");
            return;
        }
        String fromLabel = "12 AM";
        String toLabel   = formatHourLabel(hourEnd.getHour());
        String subject   = "Hourly Wallet History Report " + fromLabel + " - " + toLabel;

        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.WALLET_HISTORY, reportDate, WalletReportFrequency.HOURLY, subject).isPresent()) {
            log.info("Hourly wallet report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating hourly wallet history report: {}", subject);

        // Fetch: from start-of-day to hourEnd
        LocalDateTime from = reportDate.atStartOfDay();
        List<MyTransactionReportResponse> data = fetchAllWalletData(
                Date.valueOf(reportDate),
                Date.valueOf(reportDate),
                null  // no time filter — hourEnd handled by toDate
        );

        // Filter to records up to hourEnd
        data = data.stream()
                .filter(r -> {
                    if (r.getTransactionDate() == null || r.getTransactionTime() == null) return true;
                    LocalDateTime txDt = LocalDateTime.of(r.getTransactionDate(), r.getTransactionTime());
                    return !txDt.isAfter(hourEnd);
                })
                .toList();

        // Save to file
        String fname = "wallet_history_" + hourEnd.format(HOUR_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File file = ensureFile(relativePath);
        writeExcel(data, file);

        // Save metadata
        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(hourEnd);
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.HOURLY);
        entity.setReportType(WalletReportType.WALLET_HISTORY);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Hourly wallet report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // Generate daily report
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateDailyReport(LocalDate reportDate) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping daily wallet history report generation");
            return;
        }

        String subject = "Daily Wallet History Report " + reportDate;
        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.WALLET_HISTORY, reportDate, WalletReportFrequency.DAILY, subject).isPresent()) {
            log.info("Daily wallet report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating daily wallet history report: {}", subject);

        List<MyTransactionReportResponse> data = fetchAllWalletData(
                Date.valueOf(reportDate),
                Date.valueOf(reportDate),
                null
        );

        String fname = "wallet_history_daily_" + reportDate.format(DATE_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File file = ensureFile(relativePath);
        writeExcel(data, file);

        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(reportDate.atTime(23, 59, 59));
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.DAILY);
        entity.setReportType(WalletReportType.WALLET_HISTORY);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Daily wallet report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // List reports
    // -------------------------------------------------------------------------

    @Override
    public CommonResponse<Page<WalletDownloadReportResponse>> listReports(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletDownloadReport> entities =
                reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        return CommonResponse.success(entities.map(this::toResponse), "Reports fetched successfully");
    }

    @Override
    public CommonResponse<Page<WalletDownloadReportResponse>> listReportsByType(WalletReportType reportType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletDownloadReport> entities =
                reportRepository.findByReportTypeOrderByCreatedAtDesc(reportType, pageable);
        return CommonResponse.success(entities.map(this::toResponse), "Reports fetched successfully");
    }

    @Override
    public CommonResponse<Page<WalletDownloadReportResponse>> listReportsByDate(LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletDownloadReport> entities =
                reportRepository.findByReportDateOrderByCreatedAtDesc(date, pageable);
        return CommonResponse.success(entities.map(this::toResponse), "Reports fetched successfully");
    }

    @Override
    public CommonResponse<Page<WalletDownloadReportResponse>> listReportsByTypeAndDate(WalletReportType reportType, LocalDate date, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletDownloadReport> entities =
                reportRepository.findByReportTypeAndReportDateOrderByCreatedAtDesc(reportType, date, pageable);
        return CommonResponse.success(entities.map(this::toResponse), "Reports fetched successfully");
    }

    // -------------------------------------------------------------------------
    // Download
    // -------------------------------------------------------------------------

    @Override
    public void downloadById(Long id, HttpServletResponse response) throws Exception {
        WalletDownloadReport entity = reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Wallet download report not found: " + id));

        File file = new File(reportProperties.getBaseDir(), entity.getFilePath());
        if (!file.exists()) {
            throw new RuntimeException("Report file not found on disk: " + file.getAbsolutePath());
        }

        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + entity.getFileName() + "\"");
        Files.copy(file.toPath(), response.getOutputStream());
        response.flushBuffer();

        log.info("Wallet report downloaded: id={} file={}", id, entity.getFileName());
    }

    // -------------------------------------------------------------------------
    // Internal: fetch data via stored procedure (same SP as MyTransactionReport)
    // -------------------------------------------------------------------------

    private List<MyTransactionReportResponse> fetchAllWalletData(
            Date fromDate, Date toDate, Time transactionTime) {

        List<MyTransactionReportResponse> all = new ArrayList<>();

        for (WalletType walletType : List.of(WalletType.PREPAID, WalletType.POSTPAID)) {

            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("my_transaction_report");

            query.registerStoredProcedureParameter("p_table_name",              String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hierarchy_type",          String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hierarchy_id",            String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_from_date",               Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_to_date",                 Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_sub_products",            String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_product_name",            String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_status",                  String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_column_name",             String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_id",                      Integer.class,    ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_date",        Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_time",        Time.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_before",              BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_amount",                  BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_after",               BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_action_on_balance_amount",String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_tranx_id",                String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_narration",               String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_offset",                  Integer.class,    ParameterMode.IN);
            query.registerStoredProcedureParameter("p_page_size",               Integer.class,    ParameterMode.IN);

            // AD = admin scope → fetch all hierarchies
            query.setParameter("p_table_name",               walletType.name());
            query.setParameter("p_hierarchy_type",           "AD");
            query.setParameter("p_hierarchy_id",             null);
            query.setParameter("p_from_date",                fromDate);
            query.setParameter("p_to_date",                  toDate);
            query.setParameter("p_sub_products",             "ALL");
            query.setParameter("p_product_name",             "ALL");
            query.setParameter("p_status",                   "ALL");
            query.setParameter("p_column_name",              "admin_id");
            query.setParameter("p_id",                       null);
            query.setParameter("p_transaction_date",         null);
            query.setParameter("p_transaction_time",         transactionTime);
            query.setParameter("p_bal_before",               null);
            query.setParameter("p_amount",                   null);
            query.setParameter("p_bal_after",                null);
            query.setParameter("p_action_on_balance_amount", null);
            query.setParameter("p_tranx_id",                 null);
            query.setParameter("p_narration",                null);
            query.setParameter("p_offset",                   0);
            query.setParameter("p_page_size",                Integer.MAX_VALUE); // fetch all for report

            query.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            log.info("SP my_transaction_report returned {} rows for walletType={}", rows.size(), walletType);

            for (Object[] row : rows) {
                MyTransactionReportResponse dto = new MyTransactionReportResponse();
                dto.setId(row[0] != null ? ((Number) row[0]).intValue() : null);
                dto.setTransactionDate(row[1] != null ? ((Date) row[1]).toLocalDate() : null);
                dto.setTransactionTime(row[2] != null ? ((Time) row[2]).toLocalTime() : null);
                dto.setBalBefore((BigDecimal) row[3]);
                dto.setAmount((BigDecimal) row[4]);
                dto.setBalAfter((BigDecimal) row[5]);
                dto.setSubProductName((String) row[6]);
                dto.setProductName((String) row[7]);
                dto.setSchemeName((String) row[8]);
                dto.setActionOnBalanceAmount((String) row[9]);
                if (row[10] != null) {
                    try {
                        dto.setStatus(com.im.admin.entities.enums.Status.valueOf(
                                ((String) row[10]).trim().replace(" ", "_").toUpperCase()));
                    } catch (Exception ignored) {}
                }
                dto.setTranxId((String) row[11]);
                dto.setNarration((String) row[12]);
                dto.setHierarchyId((String) row[13]);
                dto.setVendorName((String) row[14]);
                dto.setVendorTransactionId((String) row[15]);
                dto.setLatitude(row[16] != null ? ((Number) row[16]).doubleValue() : null);
                dto.setLongitude(row[17] != null ? ((Number) row[17]).doubleValue() : null);
                dto.setWalletType(walletType);
                all.add(dto);
            }
        }

        // Sort by date+time desc
        all.sort(Comparator
                .comparing(MyTransactionReportResponse::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MyTransactionReportResponse::getTransactionTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        log.info("fetchAllWalletData: total rows fetched from SP = {}", all.size());
        return all;
    }

    // -------------------------------------------------------------------------
    // Excel generation
    // -------------------------------------------------------------------------

    private void writeExcel(List<MyTransactionReportResponse> data, File file) throws Exception {

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Wallet History");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {
                "ID", "Date", "Time", "Wallet Type", "Hierarchy ID",
                "Sub Product", "Product", "Scheme",
                "Balance Before", "Amount", "Balance After",
                "Action Type", "Status", "Transaction ID", "Narration",
                "Vendor Name", "Vendor TxID", "Latitude", "Longitude"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (MyTransactionReportResponse r : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId() != null ? r.getId() : 0);
                row.createCell(1).setCellValue(r.getTransactionDate() != null ? r.getTransactionDate().toString() : "");
                row.createCell(2).setCellValue(r.getTransactionTime() != null ? r.getTransactionTime().toString() : "");
                row.createCell(3).setCellValue(r.getWalletType() != null ? r.getWalletType().name() : "");
                row.createCell(4).setCellValue(r.getHierarchyId() != null ? r.getHierarchyId() : "");
                row.createCell(5).setCellValue(r.getSubProductName() != null ? r.getSubProductName() : "");
                row.createCell(6).setCellValue(r.getProductName() != null ? r.getProductName() : "");
                row.createCell(7).setCellValue(r.getSchemeName() != null ? r.getSchemeName() : "");
                row.createCell(8).setCellValue(r.getBalBefore() != null ? r.getBalBefore().toPlainString() : "");
                row.createCell(9).setCellValue(r.getAmount() != null ? r.getAmount().toPlainString() : "");
                row.createCell(10).setCellValue(r.getBalAfter() != null ? r.getBalAfter().toPlainString() : "");
                row.createCell(11).setCellValue(r.getActionOnBalanceAmount() != null ? r.getActionOnBalanceAmount() : "");
                row.createCell(12).setCellValue(r.getStatus() != null ? r.getStatus().name() : "");
                row.createCell(13).setCellValue(r.getTranxId() != null ? r.getTranxId() : "");
                row.createCell(14).setCellValue(r.getNarration() != null ? r.getNarration() : "");
                row.createCell(15).setCellValue(r.getVendorName() != null ? r.getVendorName() : "");
                row.createCell(16).setCellValue(r.getVendorTransactionId() != null ? r.getVendorTransactionId() : "");
                row.createCell(17).setCellValue(r.getLatitude() != null ? r.getLatitude() : 0.0);
                row.createCell(18).setCellValue(r.getLongitude() != null ? r.getLongitude() : 0.0);
            }

            workbook.write(fos);
        }
    }

    // -------------------------------------------------------------------------
    // File helper
    // -------------------------------------------------------------------------

    private File ensureFile(String relativePath) {
        File file = new File(reportProperties.getBaseDir(), relativePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private WalletDownloadReportResponse toResponse(WalletDownloadReport e) {
        return new WalletDownloadReportResponse(
                e.getId(),
                e.getReportDate(),
                e.getReportTimestamp(),
                e.getSubject(),
                e.getFrequency(),
                e.getReportType(),
                e.getFileName(),
                e.getRecordCount(),
                e.getCreatedAt()
        );
    }

    /**
     * Converts a 24h hour integer to a 12h AM/PM label.
     * 0 → "12 AM", 1 → "1 AM", 12 → "12 PM", 13 → "1 PM"
     */
    private String formatHourLabel(int hour) {
        if (hour == 0)  return "12 AM";
        if (hour < 12)  return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }
}
