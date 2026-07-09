package com.im.admin.service.impl.reports;

import com.im.admin.entities.SubProducts;
import com.im.admin.entities.UserDetails;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.AccessHierarchyPathRepository;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.request.reports.MyTransactionReportRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.reports.MyTransactionReportResponse;
import com.im.admin.service.reports.MyTransactionReportService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.TokenUtil;
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

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private TokenUtil tokenUtil;

    @Autowired
    private AccessHierarchyPathRepository accessHierarchyPathRepository;


    @Override
    public CommonResponse<Page<MyTransactionReportResponse>> getReport(
            MyTransactionReportRequest request, int page,
            int size) {

        log.info("Entered getReport() with request={}, page={}, size={}", request, page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("transactionDate").descending()
        );

        log.info("Pageable created successfully with page={}, size={}", page, size);

//        String loggedInHierarchyId =
//                (request.getHierarchyId() != null
//                        && !request.getHierarchyId().trim().isEmpty())
//                        ? request.getHierarchyId()
//                        : tokenUtil.extractHierarchyId();

        String loggedInHierarchyId =
                (request.getHierarchyId() != null
                        && !request.getHierarchyId().trim().isEmpty())
                        ? request.getHierarchyId()
                        : tokenUtil.extractHierarchyId();

        log.info("Resolved loggedInHierarchyId={}", loggedInHierarchyId);

        // For WLAU: use parent WL's hierarchy ID so they see same data as WL
        if ("WLAU".equalsIgnoreCase(commonMethods.extractHierarchyType(loggedInHierarchyId))) {

            log.info("Hierarchy type is WLAU. Fetching parent WL hierarchy for {}", loggedInHierarchyId);

            String parentWlId = userDetailsRepository
                    .findFirstByHierarchyId(loggedInHierarchyId)
                    .map(ud -> ud.getParentAccount())
                    .orElse(loggedInHierarchyId); // fallback to self if parent not found

            loggedInHierarchyId = parentWlId;

            log.info("WLAU detected — using parent WL id={} for report", loggedInHierarchyId);
        }

        String hierarchyIdsCsv =
                getHierarchyIdsCsv(loggedInHierarchyId);

        log.info("Hierarchy CSV generated={}", hierarchyIdsCsv);

        LocalDate from = StringUtils.hasText(request.getFromDate())
                ? LocalDate.parse(request.getFromDate())
                : null;

        log.info("Parsed fromDate={}", from);

        LocalDate to = StringUtils.hasText(request.getToDate())
                ? LocalDate.parse(request.getToDate())
                : null;

        log.info("Parsed toDate={}", to);

        // ===== SUB PRODUCT FILTER =====
        String subProductsParam = "ALL";

        if (StringUtils.hasText(request.getSubProductName())
                && !"ALL".equalsIgnoreCase(request.getSubProductName())) {

            log.info("Applying sub product filter={}", request.getSubProductName());

            subProductsParam = Arrays.stream(request.getSubProductName().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }

        log.info("Final subProductsParam={}", subProductsParam);

        // ===== PRODUCT FILTER =====
        String productParam = "ALL";

        // Highest priority → productId
        if (request.getProductId() != null) {

            log.info("Fetching product using productId={}", request.getProductId());

            Products product = productsRepository.findById(request.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId"));

            productParam = product.getName();

            log.info("Resolved product name={} from productId={}", productParam, request.getProductId());
        }

        // Fallback → productName (old behavior)
        else if (StringUtils.hasText(request.getProductName())
                && !"ALL".equalsIgnoreCase(request.getProductName())) {

            log.info("Applying product name filter={}", request.getProductName());

            productParam = Arrays.stream(request.getProductName().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
        }

        log.info("Final productParam={}", productParam);

        List<MyTransactionReportResponse> allData = new ArrayList<>();
        long totalCount = 0;

        List<WalletType> walletTypes =
                (request.getWalletType() == null || request.getWalletType() == WalletType.BOTH)
                        ? Arrays.asList(WalletType.PREPAID, WalletType.POSTPAID)
                        : List.of(request.getWalletType());

        log.info("Wallet types selected={}", walletTypes);

        for (WalletType walletType : walletTypes) {

            log.info("Fetching transactions for walletType={}", walletType);

            Map<String, Object> walletData =

//                    fetchTransactionsForWalletWithCount(
//                            walletType,
//                            request,
//                            from,
//                            to,
//                            subProductsParam,
//                            productParam,
//                            pageable,
//                            loggedInHierarchyId
//                    );

                    fetchTransactionsForWalletWithCount(
                            walletType,
                            request,
                            from,
                            to,
                            subProductsParam,
                            productParam,
                            pageable,
                            hierarchyIdsCsv,
                            loggedInHierarchyId
                    );

            log.info("Transactions fetched successfully for walletType={}", walletType);

            List<MyTransactionReportResponse> walletResults =
                    (List<MyTransactionReportResponse>) walletData.get("results");

            Long walletCount = (Long) walletData.get("totalCount");

            log.info("walletType={} returned {} records with totalCount={}",
                    walletType,
                    walletResults.size(),
                    walletCount);

            allData.addAll(walletResults);
            totalCount += walletCount;
        }

        log.info("Total records collected before sorting={}", allData.size());

        allData.sort(
                Comparator.comparing(MyTransactionReportResponse::getTransactionDate,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(MyTransactionReportResponse::getTransactionTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
        );

        log.info("Sorting completed.");

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allData.size());

        log.info("Pagination calculated. start={}, end={}, totalRecords={}", start, end, allData.size());

        List<MyTransactionReportResponse> pagedData;

        if (start >= allData.size()) {

            log.info("Requested page exceeds available data. Returning empty page.");

            pagedData = Collections.emptyList();

        } else {

            pagedData = allData.subList(start, end);

            log.info("Paged data size={}", pagedData.size());
        }

        Page<MyTransactionReportResponse> resultPage =
                new PageImpl<>(pagedData, pageable, totalCount);

        log.info("Page created successfully. totalCount={}, pageSize={}",
                totalCount,
                resultPage.getNumberOfElements());

        log.info("Exiting getReport() successfully.");

        return new CommonResponse<>(200, "Report fetched successfully", resultPage);
    }


    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchTransactionsForWalletWithCount(
            WalletType walletType,
            MyTransactionReportRequest request,
            LocalDate from,
            LocalDate to,
            String subProducts,
            String products,
            Pageable pageable,
            String hierarchyIdsCsv,
            String loggedInHierarchyId
    ) {

        log.info("Entered fetchTransactionsForWalletWithCount()");
        log.info("walletType={}", walletType);
        log.info("loggedInHierarchyId={}", loggedInHierarchyId);
        log.info("hierarchyIdsCsv={}", hierarchyIdsCsv);
        log.info("fromDate={}, toDate={}", from, to);
        log.info("subProducts={}, products={}", subProducts, products);

        StoredProcedureQuery query =
                entityManager.createStoredProcedureQuery("my_transaction_report");

        log.info("Stored procedure 'my_transaction_report' initialized.");

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

        log.info("All stored procedure parameters registered successfully.");

        // ===== SET PARAMS =====
        query.setParameter("p_table_name", walletType.name());

        log.info("Set p_table_name={}", walletType.name());

//        query.setParameter(
//                "p_hierarchy_type",
//                commonMethods.extractHierarchyType(loggedInHierarchyId)
//        );

        query.setParameter(
                "p_hierarchy_type",
                commonMethods.extractHierarchyType(loggedInHierarchyId)
        );

        log.info("Set p_hierarchy_type={}", commonMethods.extractHierarchyType(loggedInHierarchyId));

//        query.setParameter("p_hierarchy_id", loggedInHierarchyId);

        query.setParameter(
                "p_hierarchy_id",
                hierarchyIdsCsv
        );

        log.info("Set p_hierarchy_id={}", hierarchyIdsCsv);

        query.setParameter("p_from_date", from);
        query.setParameter("p_to_date", to);
        query.setParameter("p_sub_products", subProducts);
        query.setParameter("p_product_name", products);

        log.info("Set date and product related parameters.");

        query.setParameter(
                "p_status",
                request.getStatus() != null ? request.getStatus().name() : "ALL"
        );

        log.info("Set p_status={}", request.getStatus() != null ? request.getStatus().name() : "ALL");

        // dummy value (SP expects it, but filter nahi lagega)
        query.setParameter("p_column_name", "retailer_id");

        log.info("Set p_column_name=retailer_id");

        query.setParameter("p_id", request.getId());

        log.info("Set p_id={}", request.getId());

        Date tranDate = null;

        if (StringUtils.hasText(request.getTransactionDate())) {

            log.info("Parsing transaction date={}", request.getTransactionDate());

            try {
                tranDate = new Date(
                        new SimpleDateFormat("dd-MM-yyyy")
                                .parse(request.getTransactionDate()).getTime()
                );

                log.info("Parsed transaction date={}", tranDate);

            } catch (ParseException ignored) {

                log.info("Unable to parse transaction date={}", request.getTransactionDate());
            }
        }

        query.setParameter("p_transaction_date", tranDate);

        query.setParameter(
                "p_transaction_time",
                StringUtils.hasText(request.getTransactionTime())
                        ? Time.valueOf(request.getTransactionTime())
                        : null
        );

        log.info("Set transaction date={} and transaction time={}",
                tranDate,
                request.getTransactionTime());

        query.setParameter("p_bal_before", request.getBalBefore());
        query.setParameter("p_amount", request.getAmount());
        query.setParameter("p_bal_after", request.getBalAfter());
        query.setParameter("p_action_on_balance_amount", request.getActionOnBalanceAmount());
        query.setParameter("p_tranx_id", request.getTranxId());
        query.setParameter("p_narration", request.getNarration());

        log.info("Set balance and transaction related parameters.");

        query.setParameter("p_offset", pageable.getOffset());
        query.setParameter("p_page_size", pageable.getPageSize());

        log.info("Set pagination parameters. offset={}, pageSize={}",
                pageable.getOffset(),
                pageable.getPageSize());

        // ===== EXECUTE =====

        log.info("Executing stored procedure.");

        query.execute();

        log.info("Stored procedure executed successfully.");

        List<MyTransactionReportResponse> results = new ArrayList<>();
        Long totalCount = 0L;

        List<Object[]> rows = query.getResultList();

        log.info("Total rows returned from stored procedure={}", rows.size());

        for (Object[] row : rows) {

            log.info("Processing transaction row with id={}", row[0]);

            MyTransactionReportResponse dto = new MyTransactionReportResponse();

            dto.setId((Integer) row[0]);
            dto.setTransactionDate(((Date) row[1]).toLocalDate());
            dto.setTransactionTime(((Time) row[2]).toLocalTime());
            dto.setBalBefore((BigDecimal) row[3]);
            dto.setAmount((BigDecimal) row[4]);
            dto.setBalAfter((BigDecimal) row[5]);
            dto.setSubProductName((String) row[6]);
            dto.setProductName((String) row[7]);
            dto.setSchemeName((String) row[8]);
            dto.setActionOnBalanceAmount((String) row[9]);

            String statusStr =
                    ((String) row[10]).trim().replace(" ", "_").toUpperCase();

            dto.setStatus(Status.valueOf(statusStr));

            dto.setTranxId((String) row[11]);
            dto.setNarration((String) row[12]);
            dto.setHierarchyId((String) row[13]);

            dto.setVendorName((String) row[14]);
            dto.setVendorTransactionId((String) row[15]);
            dto.setLatitude((Double) row[16]);
            dto.setLongitude((Double) row[17]);

            // wallet type
            dto.setWalletType(walletType);

            results.add(dto);

            log.info("Transaction mapped successfully. tranxId={}, status={}",
                    dto.getTranxId(),
                    dto.getStatus());
        }

        log.info("Completed mapping all transaction rows. Total mapped records={}", results.size());

        if (query.hasMoreResults()) {

            log.info("Stored procedure returned additional result set for total count.");

            Object countObj =
                    ((List<Object>) query.getResultList()).get(0);

            totalCount = ((Number) countObj).longValue();

            log.info("Total count received from stored procedure={}", totalCount);

        } else {

            log.info("Stored procedure did not return total count result set.");
        }

        Map<String, Object> map = new HashMap<>();

        map.put("results", results);
        map.put("totalCount", totalCount);

        log.info("Response map prepared. resultsSize={}, totalCount={}",
                results.size(),
                totalCount);

        log.info("Exiting fetchTransactionsForWalletWithCount().");

        return map;
    }

    private String getHierarchyIdsCsv(String hierarchyId) {

        log.info("Entered getHierarchyIdsCsv() with hierarchyId={}", hierarchyId);

        List<String> hierarchyIds =
                accessHierarchyPathRepository
                        .findDescendantsByAncestor(hierarchyId);

        log.info("Hierarchy IDs fetched from repository={}", hierarchyIds);

        if (hierarchyIds == null || hierarchyIds.isEmpty()) {

            log.info("No descendants found for hierarchyId={}. Returning same hierarchyId.",
                    hierarchyId);

            return hierarchyId;
        }

        String hierarchyCsv = String.join(",", hierarchyIds);

        log.info("Generated hierarchyIdsCsv={}", hierarchyCsv);

        log.info("Exiting getHierarchyIdsCsv().");

        return hierarchyCsv;
    }

    @Override
    public ByteArrayInputStream downloadExcel(MyTransactionReportRequest request) throws IOException {
        List<MyTransactionReportResponse> data = getReport(request,request.getPage(),request.getSize()).getData().getContent();

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
        List<MyTransactionReportResponse> data = getReport(request, request.getPage(), request.getSize()).getData().getContent();

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
