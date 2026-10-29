package com.scs.admin.service.impl.reports;

import com.scs.admin.entities.WalletDownloadReport;
import com.scs.admin.entities.enums.WalletReportFrequency;
import com.scs.admin.entities.enums.WalletReportType;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.properties.ReportProperties;
import com.scs.admin.repositories.*;
import com.scs.admin.response.reports.MyTransactionReportResponse;
import com.scs.admin.service.reports.TransactionDownloadReportService;
import com.scs.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Implementation of TransactionDownloadReportService.
 *
 * Generates Excel reports containing ONLY TXN transactions
 * (tranxId starts with "TXN"). Excludes CHG, GST, COM.
 *
 * Files saved at: reports.base-dir/transaction-report/{date}/
 * Metadata saved in: wallet_download_reports (reportType = TRANSACTION_REPORT)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionDownloadReportServiceImpl implements TransactionDownloadReportService {

    @Autowired
    private ReportProperties reportProperties;

    @Autowired
    private WalletDownloadReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private AdminRepository adminRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HH");
    private static final String            SUB_FOLDER = "transaction-report";

    // -------------------------------------------------------------------------
    // Hourly cumulative report  (00:00 → hourEnd)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping hourly transaction report generation");
            return;
        }

        String fromLabel = "12 AM";
        String toLabel   = formatHourLabel(hourEnd.getHour());
        String subject   = "Hourly Transaction Report " + fromLabel + " - " + toLabel;

        // Skip if already generated for this slot
        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.TRANSACTION_REPORT, reportDate,
                WalletReportFrequency.HOURLY, subject).isPresent()) {
            log.info("Hourly transaction report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating hourly transaction report: {}", subject);

        // Fetch all TXN records for today, then trim to hourEnd
        List<MyTransactionReportResponse> data = fetchTxnData(
                Date.valueOf(reportDate), Date.valueOf(reportDate));

        data = data.stream()
                .filter(r -> {
                    if (r.getTransactionDate() == null || r.getTransactionTime() == null) return true;
                    LocalDateTime txDt = LocalDateTime.of(r.getTransactionDate(), r.getTransactionTime());
                    return !txDt.isAfter(hourEnd);
                })
                .toList();

        String fname        = "txn_report_" + hourEnd.format(HOUR_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File   file         = ensureFile(relativePath);
        writeExcel(data, file);

        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(hourEnd);
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.HOURLY);
        entity.setReportType(WalletReportType.TRANSACTION_REPORT);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Hourly transaction report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // Daily report  (00:00 → 23:59:59)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateDailyReport(LocalDate reportDate) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping daily transaction report generation");
            return;
        }

        String subject = "Daily Transaction Report " + reportDate;

        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.TRANSACTION_REPORT, reportDate,
                WalletReportFrequency.DAILY, subject).isPresent()) {
            log.info("Daily transaction report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating daily transaction report: {}", subject);

        List<MyTransactionReportResponse> data = fetchTxnData(
                Date.valueOf(reportDate), Date.valueOf(reportDate));

        String fname        = "txn_report_daily_" + reportDate.format(DATE_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File   file         = ensureFile(relativePath);
        writeExcel(data, file);

        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(reportDate.atTime(23, 59, 59));
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.DAILY);
        entity.setReportType(WalletReportType.TRANSACTION_REPORT);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Daily transaction report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // Fetch all data via SP, then filter to TXN% in Java
    // -------------------------------------------------------------------------

    private List<MyTransactionReportResponse> fetchTxnData(Date fromDate, Date toDate) {

        List<MyTransactionReportResponse> all = new ArrayList<>();

        for (WalletType walletType : List.of(WalletType.PREPAID, WalletType.POSTPAID)) {

            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("my_transaction_report");

            query.registerStoredProcedureParameter("p_table_name",               String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hierarchy_type",           String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hierarchy_id",             String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_from_date",                Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_to_date",                  Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_sub_products",             String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_product_name",             String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_status",                   String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_column_name",              String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_id",                       Integer.class,    ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_date",         Date.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_time",         Time.class,       ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_before",               BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_amount",                   BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_after",                BigDecimal.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_action_on_balance_amount", String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_tranx_id",                 String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_narration",                String.class,     ParameterMode.IN);
            query.registerStoredProcedureParameter("p_offset",                   Integer.class,    ParameterMode.IN);
            query.registerStoredProcedureParameter("p_page_size",                Integer.class,    ParameterMode.IN);

            query.setParameter("p_table_name",               walletType.name());
            query.setParameter("p_hierarchy_type",           "AD");  // admin scope = all hierarchies
            query.setParameter("p_hierarchy_id",             null);
            query.setParameter("p_from_date",                fromDate);
            query.setParameter("p_to_date",                  toDate);
            query.setParameter("p_sub_products",             "ALL");
            query.setParameter("p_product_name",             "ALL");
            query.setParameter("p_status",                   "ALL");
            query.setParameter("p_column_name",              "admin_id");
            query.setParameter("p_id",                       null);
            query.setParameter("p_transaction_date",         null);
            query.setParameter("p_transaction_time",         null);
            query.setParameter("p_bal_before",               null);
            query.setParameter("p_amount",                   null);
            query.setParameter("p_bal_after",                null);
            query.setParameter("p_action_on_balance_amount", null);
            query.setParameter("p_tranx_id",                 null); // TXN filter applied in Java below
            query.setParameter("p_narration",                null);
            query.setParameter("p_offset",                   0);
            query.setParameter("p_page_size",                Integer.MAX_VALUE);

            query.execute();

            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();
            log.info("SP my_transaction_report returned {} rows for walletType={}", rows.size(), walletType);

            for (Object[] row : rows) {

                String tranxId = (String) row[11];

                // ★ KEY FILTER — include only TXN transactions, skip CHG / GST / COM
                if (tranxId == null || !tranxId.startsWith("TXN")) {
                    continue;
                }

                MyTransactionReportResponse dto = new MyTransactionReportResponse();
                dto.setId(row[0] != null ? ((Number) row[0]).intValue() : null);
                dto.setTransactionDate(row[1] != null ? ((Date) row[1]).toLocalDate() : null);
                dto.setTransactionTime(row[2] != null ? ((Time) row[2]).toLocalTime() : null);
                dto.setBalBefore((BigDecimal) row[3]);
                dto.setAmount((BigDecimal) row[4]);
                dto.setBalAfter((BigDecimal) row[5]);

                // Look up subProductTitle from sub_products table using the sub_product_name value
                String subProductKey = (String) row[6];
                String subProductTitle = subProductsRepository
                        .findBySubProductTitle(subProductKey)
                        .map(sp -> sp.getSubProductTitle())
                        .orElseGet(() ->
                                subProductsRepository.findBySubProductCode(subProductKey)
                                        .map(sp -> sp.getSubProductTitle())
                                        .orElse(subProductKey)
                        );

//                dto.setSubProductName((String) row[6]);
                dto.setSubProductName(subProductTitle);
                dto.setProductName((String) row[7]);
                dto.setSchemeName((String) row[8]);
                dto.setActionOnBalanceAmount((String) row[9]);
                if (row[10] != null) {
                    try {
                        dto.setStatus(com.scs.admin.entities.enums.Status.valueOf(
                                ((String) row[10]).trim().replace(" ", "_").toUpperCase()));
                    } catch (Exception ignored) {}
                }
                dto.setTranxId(tranxId);
                dto.setNarration((String) row[12]);
                dto.setHierarchyId((String) row[13]);
                dto.setVendorName((String) row[14]);
                dto.setVendorTransactionId((String) row[15]);
                dto.setLatitude(row[16] != null ? ((Number) row[16]).doubleValue() : null);
                dto.setLongitude(row[17] != null ? ((Number) row[17]).doubleValue() : null);

                // New columns from SP (indices 18–26)
                dto.setRemarks((String) row[18]);
                dto.setTransactionType((String) row[19]);
                dto.setClientIp((String) row[20]);
                dto.setMedium((String) row[21]);
                dto.setRequestDomainName((String) row[22]);
                dto.setMobileNo((String) row[23]);
                dto.setUpdatedDateOfTransaction(row[24] != null ? ((Date) row[24]).toLocalDate() : null);
                dto.setUpdatedTimeOfTransaction(row[25] != null ? ((Time) row[25]).toLocalTime() : null);
                dto.setOperatorName((String) row[26]);

                // Resolve outletName and createdBy from hierarchy table
                String hierarchyId = (String) row[13];
                if (hierarchyId != null) {
                    String[] outletAndCreatedBy = resolveOutletAndCreatedBy(hierarchyId);
                    dto.setOutletName(outletAndCreatedBy[0]);
                    dto.setCreatedBy(outletAndCreatedBy[1]);
                }

                dto.setWalletType(walletType);
                all.add(dto);
            }
        }

        // Sort by date+time descending
        all.sort(Comparator
                .comparing(MyTransactionReportResponse::getTransactionDate,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(MyTransactionReportResponse::getTransactionTime,
                        Comparator.nullsLast(Comparator.reverseOrder())));

        log.info("fetchTxnData: TXN records after filter = {}", all.size());
        return all;
    }

    // -------------------------------------------------------------------------
    // Resolve outletName and createdBy from hierarchy table
    // -------------------------------------------------------------------------

    /**
     * Returns [outletName, createdBy] for the given hierarchyId.
     * Looks up the correct entity table based on the hierarchy type prefix.
     */
    private String[] resolveOutletAndCreatedBy(String hierarchyId) {
        String type = commonMethods.extractHierarchyType(hierarchyId);
        try {
            return switch (type.toUpperCase()) {
                case "RT" -> retailerRepository.findByRetailerId(hierarchyId)
                        .map(r -> new String[]{
                                r.getOutletAddress() != null ? r.getOutletAddress().getCompanyName() : null,
                                r.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "DT" -> distributorRepository.findByDistributorId(hierarchyId)
                        .map(d -> new String[]{
                                d.getOutletAddress() != null ? d.getOutletAddress().getCompanyName() : null,
                                d.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "PT" -> partnerRepository.findByPartnerId(hierarchyId)
                        .map(p -> new String[]{
                                p.getOutletAddress() != null ? p.getOutletAddress().getCompanyName() : null,
                                p.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "SD" -> superDistributorRepository.findBySuperDistributorId(hierarchyId)
                        .map(sd -> new String[]{
                                sd.getOutletAddress() != null ? sd.getOutletAddress().getCompanyName() : null,
                                sd.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "WL" -> whiteLabelRepository.findByWhiteLabelId(hierarchyId)
                        .map(wl -> new String[]{
                                wl.getOutletAddress() != null ? wl.getOutletAddress().getCompanyName() : null,
                                wl.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "ET" -> enterprisesRepository.findByEnterpriseId(hierarchyId)
                        .map(e -> new String[]{
                                e.getOutletAddress() != null ? e.getOutletAddress().getCompanyName() : null,
                                e.getCreatedBy()})
                        .orElse(new String[]{null, null});
                case "AD" -> adminRepository.findById(hierarchyId)
                        .map(a -> new String[]{null, a.getCreatedBy()})
                        .orElse(new String[]{null, null});
                default -> new String[]{null, null};
            };
        } catch (Exception e) {
            log.warn("Could not resolve outletName/createdBy for hierarchyId={}: {}", hierarchyId, e.getMessage());
            return new String[]{null, null};
        }
    }

    // -------------------------------------------------------------------------
    // Excel generation
    // -------------------------------------------------------------------------

    private void writeExcel(List<MyTransactionReportResponse> data, File file) throws Exception {

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Transaction Report");

            // Bold header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {
                "ID", "Date", "Time", "Wallet Type", "Hierarchy ID",
                "Sub Product", "Product", "Scheme",
                "Balance Before", "Amount", "Balance After",
                "Action Type", "Status", "Transaction ID", "Narration",
                "Vendor Name", "Vendor TxID", "Latitude", "Longitude",
                // New columns
                "Remarks", "Transaction Type", "Client IP", "Medium",
                "Request Domain Name", "Mobile No",
                "Updated Date", "Updated Time", "Operator Name",
                "Outlet Name", "Created By"
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
                // New columns
                row.createCell(19).setCellValue(r.getRemarks() != null ? r.getRemarks() : "");
                row.createCell(20).setCellValue(r.getTransactionType() != null ? r.getTransactionType() : "");
                row.createCell(21).setCellValue(r.getClientIp() != null ? r.getClientIp() : "");
                row.createCell(22).setCellValue(r.getMedium() != null ? r.getMedium() : "");
                row.createCell(23).setCellValue(r.getRequestDomainName() != null ? r.getRequestDomainName() : "");
                row.createCell(24).setCellValue(r.getMobileNo() != null ? r.getMobileNo() : "");
                row.createCell(25).setCellValue(r.getUpdatedDateOfTransaction() != null ? r.getUpdatedDateOfTransaction().toString() : "");
                row.createCell(26).setCellValue(r.getUpdatedTimeOfTransaction() != null ? r.getUpdatedTimeOfTransaction().toString() : "");
                row.createCell(27).setCellValue(r.getOperatorName() != null ? r.getOperatorName() : "");
                row.createCell(28).setCellValue(r.getOutletName() != null ? r.getOutletName() : "");
                row.createCell(29).setCellValue(r.getCreatedBy() != null ? r.getCreatedBy() : "");
            }

            workbook.write(fos);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private File ensureFile(String relativePath) {
        File file = new File(reportProperties.getBaseDir(), relativePath);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    /**
     * Converts 24h hour to 12h AM/PM label.
     * 0→"12 AM", 1→"1 AM", 12→"12 PM", 13→"1 PM"
     */
    private String formatHourLabel(int hour) {
        if (hour == 0)  return "12 AM";
        if (hour < 12)  return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }
}
