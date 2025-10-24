package com.im.admin.service.impl.reports;


import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.request.reports.MyTransactionReportRequest;
import com.im.admin.request.reports.NetworkTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.MyTransactionReportResponse;
import com.im.admin.response.reports.NetworkTransactionReportResponse;
import com.im.admin.service.reports.NetworkTransactionReportService;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class NetworkTransactionReportServiceImpl implements NetworkTransactionReportService {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public CommonResponse<Page<NetworkTransactionReportResponse>> viewNetworkTransactions(NetworkTransactionReportRequest request) {
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("transactionDate").descending());

        // 1. Validate inputs
        validateRequest(request);

        // 2. Parse date and subproducts
        LocalDate from = parseDate(request.getFromDate());
        LocalDate to = parseDate(request.getToDate());
        List<String> subProducts = parseSubProducts(request.getSubProductName());

        // 3. Determine effective hierarchy
        String effectiveHierarchyId = resolveEffectiveHierarchyId(request);
        String hierarchyType = commonMethods.extractHierarchyType(effectiveHierarchyId);

        // 4. Fetch transactions for both wallet types
        List<NetworkTransactionReportResponse> combinedResults = new ArrayList<>();
        for (WalletType walletType : List.of(WalletType.PREPAID, WalletType.POSTPAID)) {
            combinedResults.addAll(
                    fetchTransactionsFromSP(walletType, hierarchyType, effectiveHierarchyId, from, to, subProducts, request, pageable)
            );
        }

        // 5. Wrap and return paginated response
        if (combinedResults.isEmpty()) {
            return new CommonResponse<>(204, "No transactions found for the given criteria", Page.empty(pageable));
        }

//        Page<NetworkTransactionReportResponse> resultPage = new PageImpl<>(
//                combinedResults,
//                pageable,
//                pageable.getOffset() + combinedResults.size()
//        );
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), combinedResults.size());

        List<NetworkTransactionReportResponse> pagedList;
        if (start >= combinedResults.size()) {
            pagedList = Collections.emptyList();
        } else {
            pagedList = combinedResults.subList(start, end);
        }

        Page<NetworkTransactionReportResponse> resultPage =
                new PageImpl<>(pagedList, pageable, combinedResults.size());



        return new CommonResponse<>(200, "Network transaction report fetched successfully", resultPage);
    }

    private void validateRequest(NetworkTransactionReportRequest request) {
        if (!StringUtils.hasText(request.getLoginHierarchyId())) {
            throw new IllegalArgumentException("Login hierarchy ID is mandatory");
        }

        boolean childSelected = Stream.of(
                request.getRetailerId(),
                request.getDistributorId(),
                request.getPartnerId(),
                request.getSuperDistributorId(),
                request.getWhitelabelId(),
                request.getEnterprisesId()
        ).anyMatch(StringUtils::hasText);

        if (!childSelected) {
            throw new IllegalArgumentException("At least one child hierarchy ID must be selected under loginHierarchyId");
        }
    }

    private LocalDate parseDate(String dateStr) {
        return StringUtils.hasText(dateStr) ? LocalDate.parse(dateStr) : null;
    }

    private List<String> parseSubProducts(String subProductStr) {
        if (!StringUtils.hasText(subProductStr) || subProductStr.equalsIgnoreCase("ALL")) {
            return new ArrayList<>();
        }
        return Arrays.stream(subProductStr.split(",")).map(String::trim).toList();
    }

    private String resolveEffectiveHierarchyId(NetworkTransactionReportRequest request) {
        if (StringUtils.hasText(request.getRetailerId())) return request.getRetailerId();
        if (StringUtils.hasText(request.getDistributorId())) return request.getDistributorId();
        if (StringUtils.hasText(request.getPartnerId())) return request.getPartnerId();
        if (StringUtils.hasText(request.getSuperDistributorId())) return request.getSuperDistributorId();
        if (StringUtils.hasText(request.getWhitelabelId())) return request.getWhitelabelId();
        if (StringUtils.hasText(request.getEnterprisesId())) return request.getEnterprisesId();

        throw new IllegalArgumentException("Invalid hierarchy selection");
    }

    private List<NetworkTransactionReportResponse> fetchTransactionsFromSP(
            WalletType walletType,
            String hierarchyType,
            String hierarchyId,
            LocalDate from,
            LocalDate to,
            List<String> subProducts,
            NetworkTransactionReportRequest request,
            Pageable pageable
    ) {
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType.name());
        String columnName = commonMethods.getHierarchyColumnName(hierarchyType);

        StoredProcedureQuery query = entityManager.createStoredProcedureQuery("network_transaction_report");
        query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_status", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_column_name", String.class, ParameterMode.IN);


        query.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_transaction_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_transaction_time", Time.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_bal_before", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_amount", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_bal_after", BigDecimal.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_action_on_balance_amount", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_tranx_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_narration", String.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_offset", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_page_size", Integer.class, ParameterMode.IN);

        query.setParameter("p_table_name", tableName);
        query.setParameter("p_hierarchy_id", hierarchyId);
        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);
        query.setParameter("p_sub_products", String.join(",", subProducts));
        query.setParameter("p_status", request.getStatus() != null ? request.getStatus().name() : "ALL");
        query.setParameter("p_column_name", columnName);
        query.setParameter("p_id", request.getId());
        Date transactionDateParam = null;
        if (StringUtils.hasText(request.getTransactionDate())) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                java.util.Date parsed = sdf.parse(request.getTransactionDate());
                transactionDateParam = new Date(parsed.getTime());
            } catch (ParseException e) {
                // optional: log warning
                transactionDateParam = null;
            }
        }
        query.setParameter("p_transaction_date", transactionDateParam);

        query.setParameter("p_transaction_time",
                StringUtils.hasText(request.getTransactionTime()) ? Time.valueOf(request.getTransactionTime()) : null);
        query.setParameter("p_bal_before", request.getBalBefore());
        query.setParameter("p_amount", request.getAmount());
        query.setParameter("p_bal_after", request.getBalAfter());
        query.setParameter("p_action_on_balance_amount", request.getActionOnBalanceAmount());
        query.setParameter("p_tranx_id", request.getTranxId());
        query.setParameter("p_narration", request.getNarration());

        query.setParameter("p_offset", pageable.getPageNumber() * pageable.getPageSize());
        query.setParameter("p_page_size", pageable.getPageSize());

        List<Object[]> resultList = query.getResultList();

        return resultList.stream().map(row -> {
            NetworkTransactionReportResponse dto = new NetworkTransactionReportResponse();
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
            return dto;
        }).collect(Collectors.toList());
    }


    @Override
    public ByteArrayInputStream downloadExcel(NetworkTransactionReportRequest request) throws IOException {
        List<NetworkTransactionReportResponse> data = viewNetworkTransactions(request).getData().getContent();

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Network_Transaction");

            Row header = sheet.createRow(0);
            String[] columns = {"Dated", "Txid", "Balance Before", "Amount", "Balance After", "Action Type", "Status", "SubProduct", "Narration"};

            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (NetworkTransactionReportResponse tx : data) {
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
    public ByteArrayInputStream downloadCsv(NetworkTransactionReportRequest request) throws IOException {
        List<NetworkTransactionReportResponse> data = viewNetworkTransactions(request).getData().getContent();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));

        writer.write("Dated,Txid,Balance Before,Amount,Balance After,Action Type,Status,SubProduct,Narration");
        writer.newLine();

        for (NetworkTransactionReportResponse tx : data) {
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
