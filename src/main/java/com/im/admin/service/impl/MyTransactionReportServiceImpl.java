package com.im.admin.service.impl;

import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.CommissionTransactionReportRequest;
import com.im.admin.request.MyTransactionReportRequest;
import com.im.admin.response.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.MyTransactionReportResponse;
import com.im.admin.service.MyTransactionReportService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.*;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyTransactionReportServiceImpl implements MyTransactionReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Override
    public CommonResponse<Page<MyTransactionReportResponse>> getReport(MyTransactionReportRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("transactionDate").descending());

        LocalDate from = StringUtils.hasText(request.getFromDate()) ? LocalDate.parse(request.getFromDate()) : null;
        LocalDate to = StringUtils.hasText(request.getToDate()) ? LocalDate.parse(request.getToDate()) : null;

        List<String> subProducts = new ArrayList<>();
        if (StringUtils.hasText(request.getSubProductName()) && !request.getSubProductName().equalsIgnoreCase("ALL")) {
            subProducts = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }

        List<MyTransactionReportResponse> allData = new ArrayList<>();

        List<WalletType> walletTypes = (request.getWalletType() == WalletType.BOTH)
                ? Arrays.asList(WalletType.PREPAID, WalletType.POSTPAID)
                : List.of(request.getWalletType());

        for (WalletType walletType : walletTypes) {
            allData.addAll(fetchTransactionsForWallet(walletType, request, from, to, subProducts, pageable));
        }

        Page<MyTransactionReportResponse> page = new PageImpl<>(
                allData,
                pageable,
                pageable.getOffset() + allData.size() + 1
        );

        return new CommonResponse<>(200, "Report fetched successfully", page);
    }

    private List<MyTransactionReportResponse> fetchTransactionsForWallet(
            WalletType walletType,
            MyTransactionReportRequest request,
            LocalDate from,
            LocalDate to,
            List<String> subProducts,
            Pageable pageable
    ) {
        String hierarchyType = commonMethods.extractHierarchyType(request.getHierarchyId());
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType.name());
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("my_transaction_report");

        query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_status", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_column_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_offset", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_page_size", Integer.class, ParameterMode.IN);

        query.setParameter("p_table_name", tableName);
        query.setParameter("p_hierarchy_id", request.getHierarchyId());
        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);
        query.setParameter("p_sub_products", String.join(",", subProducts));
        query.setParameter("p_status", request.getStatus().name());
        query.setParameter("p_column_name", columnName);
        query.setParameter("p_offset", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("p_page_size", pageable.getPageSize());

        List<Object[]> resultList = query.getResultList();

        List<MyTransactionReportResponse> results = new ArrayList<>();

        for (Object[] row : resultList) {
            MyTransactionReportResponse dto = new MyTransactionReportResponse();
            dto.setId((Integer) row[0]);
            dto.setTransactionDate(((Date) row[1]).toLocalDate());
            dto.setTransactionTime(((Time) row[2]).toLocalTime());
            dto.setBalBefore((BigDecimal) row[3]);
            dto.setAmount((BigDecimal) row[4]);
            dto.setBalAfter((BigDecimal) row[5]);
            dto.setSubProductName((String) row[6]);
            dto.setActionOnBalanceAmount((String) row[7]);
            dto.setStatus(Status.valueOf((String) row[8]));
            dto.setTranxId((String) row[9]);
            dto.setNarration((String) row[10]);
            //dto.setWalletType(walletType);
            results.add(dto);
        }

        return results;
    }

    @Override
    public ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException {
        List<MyTransactionReportResponse> data = getReport(request).getData().getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Transaction_Report");

            Row header = sheet.createRow(0);
            String[] columns = {"Dated", "Txid", "Balance Before", "Amount", "Balance After", "Action Type", "Status", "SubProduct", "Narration"};

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (MyTransactionReportResponse tx : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getTransactionDate() + " " + tx.getTransactionTime());
                row.createCell(1).setCellValue(tx.getTranxId());
                row.createCell(2).setCellValue(tx.getBalBefore().toPlainString());
                row.createCell(3).setCellValue(tx.getAmount().toPlainString());
                row.createCell(4).setCellValue(tx.getBalAfter().toPlainString());
                row.createCell(5).setCellValue(tx.getActionOnBalanceAmount());
                row.createCell(6).setCellValue(tx.getStatus().name());
                row.createCell(7).setCellValue(tx.getSubProductName());
                row.createCell(8).setCellValue(tx.getNarration());
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    @Override
    public ByteArrayInputStream downloadCsv(MyTransactionReportRequest request) throws IOException {
        List<MyTransactionReportResponse> data = getReport(request).getData().getContent();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        writer.write("Dated,Txid,Balance Before,Amount,Balance After,Action Type,Status,SubProduct,Narration");
        writer.newLine();

        for (MyTransactionReportResponse tx : data) {
            writer.write(String.join(",",
                    tx.getTransactionDate() + " " + tx.getTransactionTime(),
                    tx.getTranxId(),
                    tx.getBalBefore().toPlainString(),
                    tx.getAmount().toPlainString(),
                    tx.getBalAfter().toPlainString(),
                    tx.getActionOnBalanceAmount(),
                    tx.getStatus().name(),
                    tx.getSubProductName(),
                    "\"" + tx.getNarration().replace("\"", "\"\"") + "\""
            ));
            writer.newLine();
        }

        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }


}
