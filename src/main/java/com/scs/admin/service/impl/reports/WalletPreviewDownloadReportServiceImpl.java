package com.scs.admin.service.impl.reports;

import com.scs.admin.entities.WalletDownloadReport;
import com.scs.admin.entities.enums.WalletReportFrequency;
import com.scs.admin.entities.enums.WalletReportType;
import com.scs.admin.properties.ReportProperties;
import com.scs.admin.repositories.WalletDownloadReportRepository;
import com.scs.admin.response.HierarchyWalletReportDto;
import com.scs.admin.service.reports.WalletPreviewDownloadReportService;
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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletPreviewDownloadReportServiceImpl implements WalletPreviewDownloadReportService {

    @Autowired
    private ReportProperties reportProperties;

    @Autowired
    private WalletDownloadReportRepository reportRepository;

    @Autowired
    private EntityManager entityManager;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HH");
    private static final String SUB_FOLDER = "wallet-preview";

    // -------------------------------------------------------------------------
    // Hourly cumulative report
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateHourlyReport(LocalDate reportDate, LocalDateTime hourEnd) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping hourly wallet preview report generation");
            return;
        }

        String fromLabel = "12 AM";
        String toLabel   = formatHourLabel(hourEnd.getHour());
        String subject   = "Hourly Wallet Preview Report " + fromLabel + " - " + toLabel;

        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.WALLET_PREVIEW, reportDate, WalletReportFrequency.HOURLY, subject).isPresent()) {
            log.info("Hourly wallet preview report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating hourly wallet preview report: {}", subject);

        List<HierarchyWalletReportDto> data = fetchAllWalletPreviewData(null); // null = all types

        String fname        = "wallet_preview_" + hourEnd.format(HOUR_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File file           = ensureFile(relativePath);
        writeExcel(data, file);

        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(hourEnd);
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.HOURLY);
        entity.setReportType(WalletReportType.WALLET_PREVIEW);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Hourly wallet preview report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // Daily report
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public void generateDailyReport(LocalDate reportDate) throws Exception {

        if (reportProperties.getBaseDir() == null || reportProperties.getBaseDir().isBlank()) {
            log.warn("reports.base-dir not configured — skipping daily wallet preview report generation");
            return;
        }

        String subject = "Daily Wallet Preview Report " + reportDate;

        if (reportRepository.findByReportTypeAndReportDateAndFrequencyAndSubject(
                WalletReportType.WALLET_PREVIEW, reportDate, WalletReportFrequency.DAILY, subject).isPresent()) {
            log.info("Daily wallet preview report already exists, skipping: {}", subject);
            return;
        }

        log.info("Generating daily wallet preview report: {}", subject);

        List<HierarchyWalletReportDto> data = fetchAllWalletPreviewData(null);

        String fname        = "wallet_preview_daily_" + reportDate.format(DATE_FMT) + ".xlsx";
        String relativePath = SUB_FOLDER + "/" + reportDate + "/" + fname;
        File file           = ensureFile(relativePath);
        writeExcel(data, file);

        WalletDownloadReport entity = new WalletDownloadReport();
        entity.setReportDate(reportDate);
        entity.setReportTimestamp(reportDate.atTime(23, 59, 59));
        entity.setSubject(subject);
        entity.setFrequency(WalletReportFrequency.DAILY);
        entity.setReportType(WalletReportType.WALLET_PREVIEW);
        entity.setFileName(fname);
        entity.setFilePath(relativePath);
        entity.setRecordCount((long) data.size());
        reportRepository.save(entity);

        log.info("Daily wallet preview report saved: {} | records={}", subject, data.size());
    }

    // -------------------------------------------------------------------------
    // Fetch data via sp_hierarchy_wallet_report (same SP as /funding/hierarchy-wallet)
    // -------------------------------------------------------------------------

    private List<HierarchyWalletReportDto> fetchAllWalletPreviewData(String hierarchyType) {

        List<HierarchyWalletReportDto> all = new ArrayList<>();
        int offset    = 0;
        int pageSize  = 1000; // batch size

        while (true) {
            StoredProcedureQuery query =
                    entityManager.createStoredProcedureQuery("sp_hierarchy_wallet_report");

            query.registerStoredProcedureParameter("p_hierarchy_type", String.class,  ParameterMode.IN);
            query.registerStoredProcedureParameter("p_offset",         Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_limit",          Integer.class, ParameterMode.IN);

            query.setParameter("p_hierarchy_type", hierarchyType != null ? hierarchyType : "");
            query.setParameter("p_offset",         offset);
            query.setParameter("p_limit",          pageSize);

            @SuppressWarnings("unchecked")
            List<Object[]> rows = query.getResultList();

            if (rows == null || rows.isEmpty()) {
                break;
            }

            for (Object[] row : rows) {
                HierarchyWalletReportDto dto = new HierarchyWalletReportDto();
                dto.setHierarchyType((String) row[0]);
                dto.setHierarchyId((String) row[1]);
                dto.setFullName((String) row[2]);
                dto.setOutletCompanyName((String) row[3]);
                dto.setMobileNumber((String) row[4]);
                dto.setEmailId((String) row[5]);
                dto.setPrepaidBalance((BigDecimal) row[6]);
                dto.setPrepaidCutoffAmount((BigDecimal) row[7]);
                dto.setPostpaidBalance((BigDecimal) row[8]);
                dto.setPostpaidCutoffAmount((BigDecimal) row[9]);
                dto.setCreatedOn(row[10] != null ? ((Timestamp) row[10]).toLocalDateTime() : null);
                dto.setCreatedBy((String) row[11]);
                dto.setLastUpdatedDate(row[12] != null ? ((Date) row[12]).toLocalDate() : null);
                dto.setRemark((String) row[13]);
                all.add(dto);
            }

            log.info("sp_hierarchy_wallet_report: fetched {} rows at offset={}", rows.size(), offset);

            if (rows.size() < pageSize) {
                break; // last page
            }
            offset += pageSize;
        }

        log.info("fetchAllWalletPreviewData: total rows = {}", all.size());
        return all;
    }

    // -------------------------------------------------------------------------
    // Excel generation
    // -------------------------------------------------------------------------

    private void writeExcel(List<HierarchyWalletReportDto> data, File file) throws Exception {

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Wallet Preview");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {
                "Hierarchy Type", "Hierarchy ID", "Full Name", "Outlet / Company Name",
                "Mobile Number", "Email ID",
                "Prepaid Balance", "Prepaid Cutoff Amount",
                "Postpaid Balance", "Postpaid Cutoff Amount",
                "Created On", "Created By",
                "Last Updated Date", "Remark"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            int rowIdx = 1;
            for (HierarchyWalletReportDto r : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(nullSafe(r.getHierarchyType()));
                row.createCell(1).setCellValue(nullSafe(r.getHierarchyId()));
                row.createCell(2).setCellValue(nullSafe(r.getFullName()));
                row.createCell(3).setCellValue(nullSafe(r.getOutletCompanyName()));
                row.createCell(4).setCellValue(nullSafe(r.getMobileNumber()));
                row.createCell(5).setCellValue(nullSafe(r.getEmailId()));
                row.createCell(6).setCellValue(r.getPrepaidBalance() != null ? r.getPrepaidBalance().toPlainString() : "0.00");
                row.createCell(7).setCellValue(r.getPrepaidCutoffAmount() != null ? r.getPrepaidCutoffAmount().toPlainString() : "0.00");
                row.createCell(8).setCellValue(r.getPostpaidBalance() != null ? r.getPostpaidBalance().toPlainString() : "0.00");
                row.createCell(9).setCellValue(r.getPostpaidCutoffAmount() != null ? r.getPostpaidCutoffAmount().toPlainString() : "0.00");
                row.createCell(10).setCellValue(r.getCreatedOn() != null ? r.getCreatedOn().toString() : "");
                row.createCell(11).setCellValue(nullSafe(r.getCreatedBy()));
                row.createCell(12).setCellValue(r.getLastUpdatedDate() != null ? r.getLastUpdatedDate().toString() : "");
                row.createCell(13).setCellValue(nullSafe(r.getRemark()));
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

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String formatHourLabel(int hour) {
        if (hour == 0)  return "12 AM";
        if (hour < 12)  return hour + " AM";
        if (hour == 12) return "12 PM";
        return (hour - 12) + " PM";
    }
}
