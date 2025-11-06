package com.im.admin.service.impl.reports;

import com.im.admin.dto.DebitCreditReportRequest;
import com.im.admin.response.DebitCreditReportResponse;
import com.im.admin.service.reports.DebitCreditReportService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DebitCreditReportServiceImpl implements DebitCreditReportService {

    private static final Logger log = LoggerFactory.getLogger(DebitCreditReportServiceImpl.class);

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Override
    public Page<DebitCreditReportResponse> getDebitCreditReport(DebitCreditReportRequest request, int page, int size) {
        log.info("Fetching Debit/Credit report for hierarchyId: {}", request.getHierarchyId());

        String hierarchyTypeForTable = StringUtils.hasText(request.getHierarchyId())
                ? commonMethods.extractHierarchyType(request.getHierarchyId())
                : request.getHierarchyType();

        List<Object[]> allResults = new ArrayList<>();
        long totalElements;

        if (!StringUtils.hasText(request.getWalletType())) {
            // Fetch both prepaid + postpaid
            String prepaidTable = commonMethods.getTransactionTable(hierarchyTypeForTable, "PREPAID");
            String postpaidTable = commonMethods.getTransactionTable(hierarchyTypeForTable, "POSTPAID");

            allResults.addAll(executeTransactionQuery(prepaidTable, request));
            allResults.addAll(executeTransactionQuery(postpaidTable, request));

            totalElements = allResults.size();

            // Merge + sort by date & time DESC
            allResults.sort(Comparator.comparing((Object[] o) -> ((Date) o[0]))
                    .thenComparing(o -> ((Time) o[1]))
                    .reversed());

            // Manual pagination
            int start = Math.min(page * size, allResults.size());
            int end = Math.min(start + size, allResults.size());
            allResults = allResults.subList(start, end);
        } else {
            // Single wallet type
            String tableName = commonMethods.getTransactionTable(hierarchyTypeForTable, request.getWalletType());
            allResults = executeTransactionQuery(tableName, request);
            totalElements = allResults.size();
        }

        List<DebitCreditReportResponse> content = allResults.stream()
                .map(obj -> new DebitCreditReportResponse(
                        ((Date) obj[0]).toLocalDate(),
                        ((Time) obj[1]).toLocalTime(),
                        (BigDecimal) obj[2],
                        (BigDecimal) obj[3],
                        (BigDecimal) obj[4],
                        (BigDecimal) obj[5],
                        (String) obj[6],                      // service
                        (String) obj[7],                      // transactionId
                        obj.length > 8 ? (String) obj[8] : null,   // narration
                        obj.length > 9 ? (String) obj[9] : null    // transactionType
                ))
                .collect(Collectors.toList());



        return new PageImpl<>(content, PageRequest.of(page, size), totalElements);
    }

    private List<Object[]> executeTransactionQuery(String tableName, DebitCreditReportRequest request) {
        String hierarchyTypeForTable = StringUtils.hasText(request.getHierarchyId())
                ? commonMethods.extractHierarchyType(request.getHierarchyId())
                : request.getHierarchyType();

        String columnName = commonMethods.getHierarchyColumnName(hierarchyTypeForTable);

        //  Check if the table has the column `transaction_type`
        boolean hasTransactionType = tableHasColumn(tableName, "transaction_type");

        StringBuilder sql = new StringBuilder(String.format("""
            SELECT 
                date_of_transaction, 
                time_of_transaction, 
                bal_before_tran, 
                amount, 
                bal_after_tran, 
                final_balance, 
                service, 
                transaction_id
                   narration
        """));

        if (hasTransactionType) {
            sql.append(", transaction_type");
        }

        sql.append(" FROM ").append(tableName)
                .append(" WHERE 1=1")
                .append(" AND date_of_transaction BETWEEN :fromDate AND :toDate");

        if (StringUtils.hasText(request.getHierarchyId()))
            sql.append(" AND ").append(columnName).append(" = :hierarchyId");
        if (request.getDateOfTransaction() != null)
            sql.append(" AND date_of_transaction = :dateOfTransaction");
        if (request.getTimeOfTransaction() != null)
            sql.append(" AND time_of_transaction = :timeOfTransaction");
        if (StringUtils.hasText(request.getService()))
            sql.append(" AND service = :service");
        if (StringUtils.hasText(request.getTransactionId()))
            sql.append(" AND transaction_id LIKE :transactionId");
        if (hasTransactionType && StringUtils.hasText(request.getTransactionType()))
            sql.append(" AND transaction_type = :transactionType");
        if (request.getAmount() != null)
            sql.append(" AND amount = :amount");
        if (request.getBalBeforeTran() != null)
            sql.append(" AND bal_before_tran = :balBeforeTran");
        if (request.getBalAfterTran() != null)
            sql.append(" AND bal_after_tran = :balAfterTran");
        if (request.getFinalBalance() != null)
            sql.append(" AND final_balance = :finalBalance");

        sql.append(" ORDER BY date_of_transaction DESC, time_of_transaction DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, request, hasTransactionType);
        return query.getResultList();
    }

    private void setQueryParameters(Query query, DebitCreditReportRequest request, boolean hasTransactionType) {
        if (StringUtils.hasText(request.getHierarchyId()))
            query.setParameter("hierarchyId", request.getHierarchyId());

        query.setParameter("fromDate", request.getFromDate());
        query.setParameter("toDate", request.getToDate());

        if (request.getDateOfTransaction() != null)
            query.setParameter("dateOfTransaction", Date.valueOf(request.getDateOfTransaction()));
        if (request.getTimeOfTransaction() != null)
            query.setParameter("timeOfTransaction", Time.valueOf(request.getTimeOfTransaction()));
        if (StringUtils.hasText(request.getService()))
            query.setParameter("service", request.getService());
        if (StringUtils.hasText(request.getTransactionId()))
            query.setParameter("transactionId", "%" + request.getTransactionId() + "%");
        if (hasTransactionType && StringUtils.hasText(request.getTransactionType()))
            query.setParameter("transactionType", request.getTransactionType());
        if (request.getAmount() != null)
            query.setParameter("amount", request.getAmount());
        if (request.getBalBeforeTran() != null)
            query.setParameter("balBeforeTran", request.getBalBeforeTran());
        if (request.getBalAfterTran() != null)
            query.setParameter("balAfterTran", request.getBalAfterTran());
        if (request.getFinalBalance() != null)
            query.setParameter("finalBalance", request.getFinalBalance());
    }

    //  Utility method to check if column exists in a table
    private boolean tableHasColumn(String tableName, String columnName) {
        try {
            String checkQuery = "SHOW COLUMNS FROM " + tableName + " LIKE :columnName";
            Query query = entityManager.createNativeQuery(checkQuery);
            query.setParameter("columnName", columnName);
            return !query.getResultList().isEmpty();
        } catch (Exception e) {
            log.warn("Could not check columns for table {}: {}", tableName, e.getMessage());
            return false;
        }
    }
}
