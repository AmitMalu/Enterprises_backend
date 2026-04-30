package com.im.admin.service.impl.reports;


import com.im.admin.entities.SubProducts;
import com.im.admin.entities.UserDetails;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.request.reports.CommissionTransactionReportRequest;
import com.im.admin.response.reports.CommissionTransactionReportResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.reports.CommissionChargeTransactionReportService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
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

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private TokenUtil tokenUtil;


    @Override
    public CommonResponse<Page<CommissionTransactionReportResponse>> getReport(CommissionTransactionReportRequest request) {

        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by("transactionDate").descending()
        );

        LocalDate from = StringUtils.hasText(request.getFromDate())
                ? LocalDate.parse(request.getFromDate())
                : null;

        LocalDate to = StringUtils.hasText(request.getToDate())
                ? LocalDate.parse(request.getToDate())
                : null;

        // ===== SUB PRODUCT FILTER =====
        String subProductsParam = "ALL";
        if (StringUtils.hasText(request.getSubProductName())
                && !"ALL".equalsIgnoreCase(request.getSubProductName())) {

            subProductsParam = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }

        // ===== PRODUCT FILTER =====
        String productParam = "ALL";

        if (request.getProductId() != null) {
            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));
            productParam = product.getName();
        }

        List<CommissionTransactionReportResponse> allData = new ArrayList<>();
        long totalCount = 0;

        List<WalletType> walletTypes =
                (request.getWalletType() == null || request.getWalletType() == WalletType.BOTH)
                        ? Arrays.asList(WalletType.PREPAID, WalletType.POSTPAID)
                        : List.of(request.getWalletType());

        for (WalletType walletType : walletTypes) {

            Map<String, Object> walletData =
                    fetchCommissionReportForWallet(
                            walletType,
                            request,
                            from,
                            to,
                            subProductsParam,
                            productParam,
                            pageable,
                            request.getHierarchyId()
                    );

            allData.addAll(
                    (List<CommissionTransactionReportResponse>) walletData.get("results")
            );
            totalCount += (Long) walletData.get("totalCount");
        }

        Page<CommissionTransactionReportResponse> page =
                new PageImpl<>(allData, pageable, totalCount);

        return new CommonResponse<>(200, "Records fetched successfully", page);
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchCommissionReportForWallet(
            WalletType walletType,
            CommissionTransactionReportRequest request,
            LocalDate from,
            LocalDate to,
            String subProducts,
            String products,
            Pageable pageable,
            String loggedInHierarchyId) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("commission_charge_transaction_report_v2");

        // ===== REGISTER PARAMS =====

        query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_type", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_transaction_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_transaction_time", Time.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_product_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_tx_type", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_tx_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_id", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_amount", BigDecimal.class, ParameterMode.IN);

        query.registerStoredProcedureParameter("p_offset", Integer.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_page_size", Integer.class, ParameterMode.IN);

        // ===== SET PARAMS =====
        query.setParameter("p_table_name", walletType.name());

        query.setParameter(
                "p_hierarchy_type",
                commonMethods.extractHierarchyType(loggedInHierarchyId)
        );

        query.setParameter("p_hierarchy_id", loggedInHierarchyId);


        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);

        Date tranDate = null;
        if (StringUtils.hasText(request.getTransactionDate())) {
            tranDate = Date.valueOf(
                    LocalDate.parse(request.getTransactionDate())
            );
        }

        query.setParameter("p_transaction_date", tranDate);

        query.setParameter(
                "p_transaction_time",
                StringUtils.hasText(request.getTransactionTime())
                        ? Time.valueOf(request.getTransactionTime())
                        : null
        );

        query.setParameter("p_sub_products", subProducts);
        query.setParameter("p_product_name", products);
        query.setParameter("p_tx_type", request.getReportType());

        query.setParameter("p_tx_id", request.getTxId());

        query.setParameter("p_id", request.getId());

        query.setParameter("p_amount", request.getAmount());

        query.setParameter("p_offset", pageable.getOffset());
        query.setParameter("p_page_size", pageable.getPageSize());

        // ===== EXECUTE =====
        query.execute();

        List<CommissionTransactionReportResponse> results = new ArrayList<>();
        Long totalCount = 0L;

        List<Object[]> rows = query.getResultList();

        for (Object[] row : rows) {

            CommissionTransactionReportResponse dto =
                    new CommissionTransactionReportResponse();

            dto.setId((Integer) row[0]);
            dto.setTransactionDate(((Date) row[1]).toLocalDate());
            dto.setTransactionTime(((Time) row[2]).toLocalTime());
            dto.setBalBefore((BigDecimal) row[3]);
            dto.setAmount((BigDecimal) row[4]);
            dto.setBalAfter((BigDecimal) row[5]);
            dto.setSubProductName((String) row[6]);
            dto.setProductName((String) row[7]);
            dto.setTxType((String) row[8]);
            dto.setTxId((String) row[9]);
            dto.setNarration((String) row[10]);

            dto.setHierarchyId(row[11] != null ? row[11].toString() : null);

            dto.setVendorName((String) row[12]);
            dto.setVendorTransactionId((String) row[13]);
            dto.setLatitude(row[14] != null ? ((Number) row[14]).doubleValue() : null);
            dto.setLongitude(row[15] != null ? ((Number) row[15]).doubleValue() : null);

            dto.setWalletType(walletType.name());

            results.add(dto);
        }

        if (query.hasMoreResults()) {
            Object countObj =
                    ((List<Object>) query.getResultList()).get(0);
            totalCount = ((Number) countObj).longValue();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("results", results);
        map.put("totalCount", totalCount);

        return map;
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


    public List<String> getRetailersUnderHierarchy(String hierarchyId) {

        String type = commonMethods.extractHierarchyType(hierarchyId);

//        if ("RT".equals(type) || "RU".equals(type)) {
//            return List.of(hierarchyId);
//        }
        if ("RU".equals(type)) {
            return List.of(hierarchyId);
        }

        List<String> resultIds = new ArrayList<>();

        // Add itself
        resultIds.add(hierarchyId);

        List<UserDetails> allChildren = new ArrayList<>();

        fetchChildrenRecursively(hierarchyId, allChildren);

        log.info("All children IDs fetched: {}",
                allChildren.stream()
                        .map(UserDetails::getHierarchyId)
                        .toList());


        resultIds.addAll(
                allChildren.stream()
                        .filter(user ->
                                "RT".equalsIgnoreCase(user.getHierarchyCode()) ||
                                        "RU".equalsIgnoreCase(user.getHierarchyCode())
                        )
                        .map(UserDetails::getHierarchyId)
                        .toList()
        );

        return resultIds;
    }


    private void fetchChildrenRecursively(
            String parentId,
            List<UserDetails> result
    ) {

        List<UserDetails> children =
                userDetailsRepository.findByParentAccount(parentId);

        for (UserDetails child : children) {
            result.add(child);
            fetchChildrenRecursively(child.getHierarchyId(), result);
        }
    }

}

