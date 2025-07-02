package com.im.admin.service.impl.reports;


import com.im.admin.request.CommissionTransactionReportRequest;
import com.im.admin.response.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.CommissionChargeTransactionReportService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommissionChargeTransactionReportServiceImpl implements CommissionChargeTransactionReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Override
    public CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("transactionTime").descending());

        // 1. Parse dates
        LocalDate from = StringUtils.hasText(request.getFromDate())
                ? LocalDate.parse(request.getFromDate()) : null;
        LocalDate to = StringUtils.hasText(request.getToDate())
                ? LocalDate.parse(request.getToDate()) : null;

        // 2. Parse sub-product names
        List<String> subProducts = new ArrayList<>();
        if (StringUtils.hasText(request.getSubProductName())) {
            subProducts = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        // 3. Build dynamic table name
        String hierarchyType = commonMethods.extractHierarchyType(request.getHierarchyId());
        String tableName = commonMethods.getTransactionTable(hierarchyType, request.getWalletType().name());
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        // 4. Prepare stored procedure call
        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("commission_charge_transaction_report");

        query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_transaction_type", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_column_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_offset", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_page_size", Integer.class, ParameterMode.IN);

        query.setParameter("p_table_name", tableName);
        query.setParameter("p_hierarchy_id", request.getHierarchyId());
        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);
        query.setParameter("p_sub_products", String.join(",", subProducts));
        query.setParameter("p_transaction_type",request.getReportType());
        query.setParameter("p_column_name", columnName);
        query.setParameter("p_offset", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("p_page_size", pageable.getPageSize());

        // 5. Execute and map results
        List<Object[]> results = query.getResultList();

        List<CommissionTransactionReportResponse> dataList = results.stream()
                .map(record -> {
                    CommissionTransactionReportResponse dto = new CommissionTransactionReportResponse();
                    dto.setId((Integer) record[0]);
                    dto.setTransactionDate(((Date) record[11]).toLocalDate());
                    dto.setTransactionTime(((Time) record[25]).toLocalTime());
                    dto.setSubProductName((String) record[24]);
                    dto.setTxType((String) record[22]);
                    dto.setBalBefore((BigDecimal) record[4]);
                    dto.setAmount((BigDecimal) record[2]);
                    dto.setBalAfter((BigDecimal) record[3]);
                    dto.setOperator((String) record[16]);
                    //dto.setNarration();
                    dto.setTxId((String) record[27]);

                    // Map more fields if needed
                    return dto;
                }).collect(Collectors.toList());

        // Since stored procedures don’t return count, fake Page manually
        Page<CommissionTransactionReportResponse> page = new PageImpl<>(
                dataList,
                pageable,
                pageable.getOffset() + dataList.size() + 1 // Approximate
        );

        return new CommonResponse<>(200, "Records fetched successfully", page);
    }

    @Override
    public ByteArrayInputStream downloadExcel(CommissionTransactionReportRequest request) throws IOException {
        List<CommissionTransactionReportResponse> reportData = getReport(request).getData().getContent();

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Transactions");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Transaction ID");
            header.createCell(1).setCellValue("Date");
            header.createCell(2).setCellValue("Time");
            header.createCell(3).setCellValue("SubProduct");
            header.createCell(4).setCellValue("Type");
            header.createCell(5).setCellValue("Amount");
            header.createCell(6).setCellValue("Balance Before");
            header.createCell(7).setCellValue("Balance After");
            header.createCell(8).setCellValue("Operator");

            int rowIdx = 1;
            for (CommissionTransactionReportResponse dto : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getTxId());
                row.createCell(1).setCellValue(dto.getTransactionDate().toString());
                row.createCell(2).setCellValue(dto.getTransactionTime().toString());
                row.createCell(3).setCellValue(dto.getSubProductName());
                row.createCell(4).setCellValue(dto.getTxType());
                row.createCell(5).setCellValue(dto.getAmount().doubleValue());
                row.createCell(6).setCellValue(dto.getBalBefore().doubleValue());
                row.createCell(7).setCellValue(dto.getBalAfter().doubleValue());
                row.createCell(8).setCellValue(dto.getOperator());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    public ByteArrayInputStream downloadCsv(CommissionTransactionReportRequest request) {
        List<CommissionTransactionReportResponse> reportData = getReport(request).getData().getContent();
        final StringWriter writer = new StringWriter();
        writer.append("Transaction ID,Date,Time,SubProduct,Type,Amount,Balance Before,Balance After,Operator\n");

        for (CommissionTransactionReportResponse dto : reportData) {
            writer.append(dto.getTxId()).append(",")
                    .append(dto.getTransactionDate().toString()).append(",")
                    .append(dto.getTransactionTime().toString()).append(",")
                    .append(dto.getSubProductName()).append(",")
                    .append(dto.getTxType()).append(",")
                    .append(dto.getAmount().toString()).append(",")
                    .append(dto.getBalBefore().toString()).append(",")
                    .append(dto.getBalAfter().toString()).append(",")
                    .append(dto.getOperator()).append("\n");
        }

        return new ByteArrayInputStream(writer.toString().getBytes(StandardCharsets.UTF_8));
    }
}
