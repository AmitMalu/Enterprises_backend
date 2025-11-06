package com.im.admin.service.impl.reports;


import com.im.admin.entities.SubProducts;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.request.reports.CommissionTransactionReportRequest;
import com.im.admin.response.reports.CommissionTransactionReportResponse;
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

import javax.sql.DataSource;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommissionChargeTransactionReportServiceImpl implements CommissionChargeTransactionReportService {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DataSource dataSource;


    @Override
    public CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request) {

        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), Sort.by("transactionTime").descending());

        //  Parse date & time safely
        LocalDate from = StringUtils.hasText(request.getFromDate()) ? LocalDate.parse(request.getFromDate()) : null;
        LocalDate to = StringUtils.hasText(request.getToDate()) ? LocalDate.parse(request.getToDate()) : null;
        LocalTime transactionTime = StringUtils.hasText(request.getTransactionTime()) ? LocalTime.parse(request.getTransactionTime()) : null;

        //  Fetch product and all sub-products for that product
        Products product = productsRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

        List<String> allSubProducts = subProductsRepository.findByProduct(product)
                .stream()
                .map(SubProducts::getSubProductTitle)
                .collect(Collectors.toList());

        //  Filter sub-products based on request
        List<String> filteredSubProducts = new ArrayList<>();

        if (StringUtils.hasText(request.getSubProductName())) {
            List<String> requestedSubs = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());

            for (String sub : requestedSubs) {
                if (allSubProducts.contains(sub)) {
                    filteredSubProducts.add(sub);
                }
            }
        } else {
            // If subProductName not provided → include all
            filteredSubProducts.addAll(allSubProducts);
        }

        //  Join filtered sub-products to comma-separated string for stored procedure
        String subProductsParam = filteredSubProducts.isEmpty() ? null : String.join(",", filteredSubProducts);

        //  Determine hierarchy type, table name & column name
        String hierarchyTypeForTable = StringUtils.hasText(request.getHierarchyId())
                ? commonMethods.extractHierarchyType(request.getHierarchyId())
                : request.getHierarchyType();

        String columnName = commonMethods.getHierarchyColumnName(hierarchyTypeForTable);

        //  Handle wallet type logic → if null or empty → both PREPAID & POSTPAID
        List<String> walletTypes = new ArrayList<>();
        if (request.getWalletType() == null || !StringUtils.hasText(request.getWalletType().name())) {
            walletTypes.add("PREPAID");
            walletTypes.add("POSTPAID");
        } else {
            walletTypes.add(request.getWalletType().name());
        }

        List<CommissionTransactionReportResponse> allData = new ArrayList<>();

        //  Loop through wallet types and fetch data for each
        for (String walletType : walletTypes) {

            String tableName = commonMethods.getTransactionTable(hierarchyTypeForTable, walletType);

            StoredProcedureQuery query = entityManager.createStoredProcedureQuery("commission_charge_transaction_report");

            //  Register all parameters
            query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_type", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_column_name", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_offset", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_page_size", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_tx_id", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_tx_type", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_operator", String.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_amount", Double.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_before", Double.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_bal_after", Double.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_date", Date.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_transaction_time", Time.class, ParameterMode.IN);
            query.registerStoredProcedureParameter("p_narration", String.class, ParameterMode.IN);

            //  Set all parameters
            query.setParameter("p_table_name", tableName);
            query.setParameter("p_hierarchy_id", StringUtils.hasText(request.getHierarchyId()) ? request.getHierarchyId() : null);
            query.setParameter("p_column_name", columnName);
            query.setParameter("p_from_date", from);
            query.setParameter("p_to_date", to);
            query.setParameter("p_sub_products", subProductsParam);
            query.setParameter("p_transaction_type", request.getReportType());
            query.setParameter("p_offset", pageable.getPageNumber() * pageable.getPageSize());
            query.setParameter("p_page_size", pageable.getPageSize());
            query.setParameter("p_id", request.getId());
            query.setParameter("p_tx_id", request.getTxId());
            query.setParameter("p_tx_type", request.getTxType());
            query.setParameter("p_operator", request.getOperator());
            query.setParameter("p_amount", request.getAmount());
            query.setParameter("p_bal_before", request.getBalBefore());
            query.setParameter("p_bal_after", request.getBalAfter());
            query.setParameter("p_narration", request.getNarration());

            //  Parse and set transaction date safely
            Date transactionDateParam = null;
            if (StringUtils.hasText(request.getTransactionDate())) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                    java.util.Date parsed = sdf.parse(request.getTransactionDate());
                    transactionDateParam = new Date(parsed.getTime());
                } catch (ParseException e) {
                    log.warn("Invalid transactionDate format (expected dd-MM-yyyy): {}", request.getTransactionDate());
                }
            }
            query.setParameter("p_transaction_date", transactionDateParam);
            query.setParameter("p_transaction_time", transactionTime != null ? Time.valueOf(transactionTime) : null);

            //  Execute stored procedure
            List<Object[]> results = query.getResultList();

            if (results == null || results.isEmpty()) {
                log.info("No records found for table: {}, hierarchyId: {}", tableName, request.getHierarchyId());
                continue;
            }

            //  Get column names dynamically
            List<String> columnNames = getColumnNames(tableName);
            if (columnNames.isEmpty()) {
                log.warn("No columns found for table: {}", tableName);
                continue;
            }

            //  Map results to DTO
            for (Object[] record : results) {
                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < columnNames.size() && i < record.length; i++) {
                    map.put(columnNames.get(i), record[i]);
                }

                CommissionTransactionReportResponse dto = new CommissionTransactionReportResponse();

                dto.setId(map.get("id") != null ? ((Number) map.get("id")).intValue() : 0);
                dto.setTransactionDate(map.get("date_of_transaction") != null ? ((Date) map.get("date_of_transaction")).toLocalDate() : null);
                dto.setTransactionTime(map.get("time_of_transaction") != null ? ((Time) map.get("time_of_transaction")).toLocalTime() : null);
                dto.setSubProductName((String) map.getOrDefault("sub_product_name", ""));
                dto.setTxType((String) map.getOrDefault("transaction_type", ""));
                dto.setTxId((String) map.getOrDefault("transaction_id", ""));
                dto.setOperator((String) map.getOrDefault("operator_name", ""));
                dto.setAmount(map.get("amount") != null ? (BigDecimal) map.get("amount") : BigDecimal.ZERO);
                dto.setBalBefore(map.get("bal_before_tran") != null ? (BigDecimal) map.get("bal_before_tran") : BigDecimal.ZERO);
                dto.setBalAfter(map.get("bal_after_tran") != null ? (BigDecimal) map.get("bal_after_tran") : BigDecimal.ZERO);
                dto.setWalletType(walletType);
                dto.setNarration((String) map.getOrDefault("narration", ""));

                allData.add(dto);
            }
        }

        //  Return combined result for both wallet types
        Page<CommissionTransactionReportResponse> page =
                new PageImpl<>(allData, pageable, pageable.getOffset() + allData.size());

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


    private List<String> getColumnNames(String tableName) {
        List<String> columnNames = new ArrayList<>();

        String query = "SELECT * FROM " + tableName + " LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i).toLowerCase());
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error fetching column names for table: " + tableName, e);
        }

        return columnNames;
    }

}

