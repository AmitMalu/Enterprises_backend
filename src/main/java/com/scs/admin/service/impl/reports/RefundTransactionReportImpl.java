package com.scs.admin.service.impl.reports;

import com.scs.admin.dto.RefundTransactionDto;
import com.scs.admin.service.reports.RefundTransactionReportService;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;
import java.util.List;

import com.scs.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class RefundTransactionReportImpl implements RefundTransactionReportService {

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private EntityManager entityManager;

    @Override
    public Page<RefundTransactionDto> getRefundTransactions(String hierarchyType, String walletType, Date fromDate, Date toDate,
                                                            String transactionId, BigDecimal amount, BigDecimal balAfterTran,
                                                            BigDecimal balBeforeTran, Date dateOfTransaction,
                                                            BigDecimal finalBalance, Integer id, Time timeOfTransaction,
                                                            Date updatedDateOfTransaction, Time updatedTimeOfTransaction,
                                                            String actionOnBalanceAmount, String bankRefId,
                                                            String operatorName, String userId,
                                                            String schemeHierarchyName, String schemeName,
                                                            String service, String subOperatorName,
                                                            String subProductName, String tranStatus,
                                                            String transactionType, Pageable pageable) {

        String entityName = commonMethods.getEntityName(hierarchyType);

        List<String> walletTypes = new ArrayList<>();
        if (walletType != null && (walletType.equalsIgnoreCase("prepaid") || walletType.equalsIgnoreCase("postpaid"))) {
            walletTypes.add(walletType.toLowerCase());
        } else {
            walletTypes.add("prepaid");
            walletTypes.add("postpaid");
        }

        List<RefundTransactionDto> allResults = new ArrayList<>();

        for (String wType : walletTypes) {
            String tableName = entityName + "_" + wType + "_transaction_details";

            StringBuilder sql = new StringBuilder("SELECT * FROM " + tableName + " WHERE transaction_id LIKE 'REV%' AND date_of_transaction BETWEEN :startDate AND :endDate");

            if (transactionId != null) sql.append(" AND transaction_id LIKE :transactionId");
            if (amount != null) sql.append(" AND amount = :amount");
            if (balAfterTran != null) sql.append(" AND bal_after_tran = :balAfterTran");
            if (balBeforeTran != null) sql.append(" AND bal_before_tran = :balBeforeTran");
            if (dateOfTransaction != null) sql.append(" AND date_of_transaction = :dateOfTransaction");
            if (finalBalance != null) sql.append(" AND final_balance = :finalBalance");
            if (id != null) sql.append(" AND id = :id");
            if (timeOfTransaction != null) sql.append(" AND time_of_transaction = :timeOfTransaction");
            if (updatedDateOfTransaction != null) sql.append(" AND updated_date_of_transaction = :updatedDateOfTransaction");
            if (updatedTimeOfTransaction != null) sql.append(" AND updated_time_of_transaction = :updatedTimeOfTransaction");
            if (actionOnBalanceAmount != null) sql.append(" AND action_on_balance_amount LIKE :actionOnBalanceAmount");
            if (bankRefId != null) sql.append(" AND bank_ref_id LIKE :bankRefId");
            if (operatorName != null) sql.append(" AND operator_name LIKE :operatorName");
            if (userId != null) sql.append(" AND " + getHierarchyIdColumn(hierarchyType) + " LIKE :userId");
            if (schemeHierarchyName != null) sql.append(" AND scheme_hierarchy_name LIKE :schemeHierarchyName");
            if (schemeName != null) sql.append(" AND scheme_name LIKE :schemeName");
            if (service != null) sql.append(" AND service LIKE :service");
            if (subOperatorName != null) sql.append(" AND sub_operator_name LIKE :subOperatorName");
            if (subProductName != null) sql.append(" AND sub_product_name LIKE :subProductName");
            if (tranStatus != null) sql.append(" AND tran_status LIKE :tranStatus");
            if (transactionType != null) sql.append(" AND transaction_type LIKE :transactionType");

            Query query = entityManager.createNativeQuery(sql.toString(), Map.class);

            query.setParameter("startDate", fromDate);
            query.setParameter("endDate", toDate);
            if (transactionId != null) query.setParameter("transactionId", "%" + transactionId + "%");
            if (amount != null) query.setParameter("amount", amount);
            if (balAfterTran != null) query.setParameter("balAfterTran", balAfterTran);
            if (balBeforeTran != null) query.setParameter("balBeforeTran", balBeforeTran);
            if (dateOfTransaction != null) query.setParameter("dateOfTransaction", dateOfTransaction);
            if (finalBalance != null) query.setParameter("finalBalance", finalBalance);
            if (id != null) query.setParameter("id", id);
            if (timeOfTransaction != null) query.setParameter("timeOfTransaction", timeOfTransaction);
            if (updatedDateOfTransaction != null) query.setParameter("updatedDateOfTransaction", updatedDateOfTransaction);
            if (updatedTimeOfTransaction != null) query.setParameter("updatedTimeOfTransaction", updatedTimeOfTransaction);
            if (actionOnBalanceAmount != null) query.setParameter("actionOnBalanceAmount", "%" + actionOnBalanceAmount + "%");
            if (bankRefId != null) query.setParameter("bankRefId", "%" + bankRefId + "%");
            if (operatorName != null) query.setParameter("operatorName", "%" + operatorName + "%");
            if (userId != null) query.setParameter("userId", "%" + userId + "%");
            if (schemeHierarchyName != null) query.setParameter("schemeHierarchyName", "%" + schemeHierarchyName + "%");
            if (schemeName != null) query.setParameter("schemeName", "%" + schemeName + "%");
            if (service != null) query.setParameter("service", "%" + service + "%");
            if (subOperatorName != null) query.setParameter("subOperatorName", "%" + subOperatorName + "%");
            if (subProductName != null) query.setParameter("subProductName", "%" + subProductName + "%");
            if (tranStatus != null) query.setParameter("tranStatus", "%" + tranStatus + "%");
            if (transactionType != null) query.setParameter("transactionType", "%" + transactionType + "%");

            List<Map<String, Object>> records = query.getResultList();
            for (Map<String, Object> record : records) {
                allResults.add(this.mapToRefundTransactionDto(record, hierarchyType));
            }
        }

        // Pagination logic
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allResults.size());
        List<RefundTransactionDto> pagedList = (start <= end) ? allResults.subList(start, end) : new ArrayList<>();

        return new PageImpl<>(pagedList, pageable, allResults.size());
    }

    private RefundTransactionDto mapToRefundTransactionDto(Map<String, Object> record, String hierarchyType) {
        RefundTransactionDto dto = new RefundTransactionDto();

        dto.setTransactionId((String) record.get("transaction_id"));
        dto.setAmount(record.get("amount") != null ? new BigDecimal(record.get("amount").toString()) : null);
        dto.setBalAfterTran(record.get("bal_after_tran") != null ? new BigDecimal(record.get("bal_after_tran").toString()) : null);
        dto.setBalBeforeTran(record.get("bal_before_tran") != null ? new BigDecimal(record.get("bal_before_tran").toString()) : null);
        dto.setDateOfTransaction((Date) record.get("date_of_transaction"));
        dto.setFinalBalance(record.get("final_balance") != null ? new BigDecimal(record.get("final_balance").toString()) : null);
        dto.setId(record.get("id") != null ? Integer.parseInt(record.get("id").toString()) : null);
        dto.setTimeOfTransaction((Time) record.get("time_of_transaction"));
        dto.setUpdatedDateOfTransaction((Date) record.get("updated_date_of_transaction"));
        dto.setUpdatedTimeOfTransaction((Time) record.get("updated_time_of_transaction"));
        dto.setActionOnBalanceAmount((String) record.get("action_on_balance_amount"));
        dto.setBankRefId((String) record.get("bank_ref_id"));
        dto.setOperatorName((String) record.get("operator_name"));
        dto.setUserId((String) record.get(getHierarchyIdColumn(hierarchyType)));
        dto.setSchemeHierarchyName((String) record.get("scheme_hierarchy_name"));
        dto.setSchemeName((String) record.get("scheme_name"));
        dto.setService((String) record.get("service"));
        dto.setSubOperatorName((String) record.get("sub_operator_name"));
        dto.setSubProductName((String) record.get("sub_product_name"));
        dto.setTranStatus((String) record.get("tran_status"));
        dto.setTransactionType((String) record.get("transaction_type"));

        return dto;
    }

    public String getHierarchyIdColumn(String hierarchyType) {
        switch (hierarchyType.toUpperCase()) {
            case "RT": return "retailer_id";
            case "PT": return "partner_id";
            case "ET": return "enterprise_id";
            case "DT": return "distributor_id";
            case "SD": return "super_distributor_id";
            case "AD": return "admin_id";
            case "WL": return "white_label_id";
            case "RU": return "retailer_user_id";
            default: throw new IllegalArgumentException("Unknown hierarchy type: " + hierarchyType);
        }
    }



}
