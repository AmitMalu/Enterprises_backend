package com.im.admin.repositories;

import com.im.admin.dto.DynamicTransactionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public class DynamicTransactionDetailsRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<DynamicTransactionDTO> findTransactions(String tableName, String transactionId) {
        String sql = "SELECT * FROM " + tableName + " WHERE transaction_id = ?";
        return jdbcTemplate.query(sql, new Object[]{transactionId}, (rs, rowNum) -> {
            DynamicTransactionDTO dto = new DynamicTransactionDTO();
            dto.setTransactionId(rs.getString("transaction_id"));
            dto.setAmount(rs.getBigDecimal("amount"));
            dto.setBalBeforeTran(rs.getBigDecimal("bal_before_tran"));
            dto.setBalAfterTran(rs.getBigDecimal("bal_after_tran"));
            dto.setFinalBalance(rs.getBigDecimal("final_balance"));
            dto.setTranStatus(rs.getString("tran_status"));
            dto.setActionOnBalanceAmount(rs.getString("action_on_balance_amount"));
            dto.setService(rs.getString("service"));
            dto.setOperatorName(rs.getString("operator_name"));
            dto.setSchemeName(rs.getString("scheme_name"));
            dto.setSubProductName(rs.getString("sub_product_name"));
            dto.setMobileNo(rs.getString("mobile_no"));
            dto.setRemarks(rs.getString("remarks"));
            dto.setNarration(rs.getString("narration"));
            return dto;
        });
    }

    public void insertTransaction(String tableName, DynamicTransactionDTO dto) {
        String sql = "INSERT INTO " + tableName + " (transaction_id, amount, bal_before_tran, bal_after_tran, " +
                "final_balance, tran_status, action_on_balance_amount, service, operator_name, sub_product_name, " +
                "scheme_name, mobile_no, remarks, narration, date_of_transaction, time_of_transaction) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.update(sql,
                dto.getTransactionId(),
                dto.getAmount(),
                dto.getBalBeforeTran(),
                dto.getBalAfterTran(),
                dto.getFinalBalance(),
                dto.getTranStatus(),
                dto.getActionOnBalanceAmount(),
                dto.getService(),
                dto.getOperatorName(),
                dto.getSubProductName(),
                dto.getSchemeName(),
                dto.getMobileNo(),
                dto.getRemarks(),
                dto.getNarration(),
                LocalDate.now(),
                LocalTime.now());
    }
}

