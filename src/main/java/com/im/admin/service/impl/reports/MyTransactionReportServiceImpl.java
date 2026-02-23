package com.im.admin.service.impl.reports;

import com.im.admin.entities.SubProducts;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.request.reports.MyTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.MyTransactionReportResponse;
import com.im.admin.service.reports.MyTransactionReportService;
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
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MyTransactionReportServiceImpl implements MyTransactionReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;


    @Override
    public CommonResponse<Page<MyTransactionReportResponse>> getReport(
            MyTransactionReportRequest request) {

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
//        String productParam = "ALL";
//        if (StringUtils.hasText(request.getProductName())
//                && !"ALL".equalsIgnoreCase(request.getProductName())) {
//
//            productParam = Arrays.stream(request.getProductName().split(","))
//                    .map(String::trim)
//                    .filter(s -> !s.isEmpty())
//                    .collect(Collectors.joining(","));
//        }

        // ===== PRODUCT FILTER =====
        String productParam = "ALL";

//  Highest priority → productId
        if (request.getProductId() != null) {
            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

            productParam = product.getName();
        }
// Fallback → productName (old behavior)
        else if (StringUtils.hasText(request.getProductName())
                && !"ALL".equalsIgnoreCase(request.getProductName())) {

            productParam = Arrays.stream(request.getProductName().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }



        List<MyTransactionReportResponse> allData = new ArrayList<>();
        long totalCount = 0;

        List<WalletType> walletTypes =
                (request.getWalletType() == null || request.getWalletType() == WalletType.BOTH)
                        ? Arrays.asList(WalletType.PREPAID, WalletType.POSTPAID)
                        : List.of(request.getWalletType());

        for (WalletType walletType : walletTypes) {

            Map<String, Object> walletData =
                    fetchTransactionsForWalletWithCount(
                            walletType,
                            request,
                            from,
                            to,
                            subProductsParam,
                            productParam,
                            pageable
                    );

            allData.addAll(
                    (List<MyTransactionReportResponse>) walletData.get("results")
            );
            totalCount += (Long) walletData.get("totalCount");
        }

        Page<MyTransactionReportResponse> page =
                new PageImpl<>(allData, pageable, totalCount);

        return new CommonResponse<>(200, "Report fetched successfully", page);
    }




    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchTransactionsForWalletWithCount(
            WalletType walletType,
            MyTransactionReportRequest request,
            LocalDate from,
            LocalDate to,
            String subProducts,
            String products,
            Pageable pageable
    ) {

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("my_transaction_report");

        // ===== REGISTER PARAMS (EXACTLY SP KE ORDER ME) =====
        query.registerStoredProcedureParameter("p_table_name", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_type", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_hierarchy_id", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_from_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_to_date", Date.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_sub_products", String.class, ParameterMode.IN);
        query.registerStoredProcedureParameter("p_product_name", String.class, ParameterMode.IN);
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

        // ===== SET PARAMS =====
//        query.setParameter("p_table_name", "ALL");          // 🔥 MAIN POINT
        // PREPAID or POSTPAID
        query.setParameter("p_table_name", walletType.name());
// PREPAID or POSTPAID

        query.setParameter(
                "p_hierarchy_type",
                StringUtils.hasText(request.getHierarchyType()) ? request.getHierarchyType() : null
        );

//        query.setParameter("p_hierarchy_id", null);
        query.setParameter(
                "p_hierarchy_id",
                StringUtils.hasText(request.getHierarchyId())
                        ? request.getHierarchyId()
                        : null
        );


        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);
        query.setParameter("p_sub_products", subProducts);
        query.setParameter("p_product_name", products);

        query.setParameter(
                "p_status",
                request.getStatus() != null ? request.getStatus().name() : "ALL"
        );

        // dummy value (SP expects it, but filter nahi lagega)
        query.setParameter("p_column_name", "retailer_id");

        query.setParameter("p_id", request.getId());

        Date tranDate = null;
        if (StringUtils.hasText(request.getTransactionDate())) {
            try {
                tranDate = new Date(
                        new SimpleDateFormat("dd-MM-yyyy")
                                .parse(request.getTransactionDate()).getTime()
                );
            } catch (ParseException ignored) {}
        }

        query.setParameter("p_transaction_date", tranDate);
        query.setParameter(
                "p_transaction_time",
                StringUtils.hasText(request.getTransactionTime())
                        ? Time.valueOf(request.getTransactionTime())
                        : null
        );

        query.setParameter("p_bal_before", request.getBalBefore());
        query.setParameter("p_amount", request.getAmount());
        query.setParameter("p_bal_after", request.getBalAfter());
        query.setParameter("p_action_on_balance_amount", request.getActionOnBalanceAmount());
        query.setParameter("p_tranx_id", request.getTranxId());
        query.setParameter("p_narration", request.getNarration());

        query.setParameter("p_offset", pageable.getOffset());
        query.setParameter("p_page_size", pageable.getPageSize());

        // ===== EXECUTE =====
        query.execute();

        List<MyTransactionReportResponse> results = new ArrayList<>();
        Long totalCount = 0L;

        List<Object[]> rows = query.getResultList();
        for (Object[] row : rows) {

            MyTransactionReportResponse dto = new MyTransactionReportResponse();

            dto.setId((Integer) row[0]);
            dto.setTransactionDate(((Date) row[1]).toLocalDate());
            dto.setTransactionTime(((Time) row[2]).toLocalTime());
            dto.setBalBefore((BigDecimal) row[3]);
            dto.setAmount((BigDecimal) row[4]);
            dto.setBalAfter((BigDecimal) row[5]);
            dto.setSubProductName((String) row[6]);
            dto.setProductName((String) row[7]);
//            dto.setActionOnBalanceAmount((String) row[8]);
//
//            String statusStr =
//                    ((String) row[9]).trim().replace(" ", "_").toUpperCase();
//            dto.setStatus(Status.valueOf(statusStr));
//
//            dto.setTranxId((String) row[10]);
//            dto.setNarration((String) row[11]);
//            dto.setHierarchyId((String) row[12]);

            dto.setSchemeName((String) row[8]);
            dto.setActionOnBalanceAmount((String) row[9]);

            String statusStr =
                    ((String) row[10]).trim().replace(" ", "_").toUpperCase();
            dto.setStatus(Status.valueOf(statusStr));

            dto.setTranxId((String) row[11]);
            dto.setNarration((String) row[12]);
            dto.setHierarchyId((String) row[13]);


            // wallet type
            dto.setWalletType(walletType);

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
