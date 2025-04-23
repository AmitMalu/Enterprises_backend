package com.im.admin.repositories;


import jakarta.persistence.Column;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Repository
public class DynamicTransactionRepositoryImpl implements DynamicTransactionRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<Object> findTransactionsByBaseId(String tableName, String baseId) {
        String sql = "SELECT * FROM " + tableName + " WHERE transaction_id LIKE ?";
        return Collections.singletonList(jdbcTemplate.queryForList(sql, "%" + baseId));
    }

    @Override
    public void save(String tableName, Object transaction) {
        Map<String, Object> map = (Map<String, Object>) transaction;

        String transactionId = (String) map.get("transaction_id");
        String tranStatus = (String) map.get("tran_status");

        LocalDate currentDate = LocalDate.now();
        LocalTime currentTime = LocalTime.now();

        String sql = "UPDATE " + tableName +
                " SET tran_status = ?, updated_date_of_transaction = ?, updated_time_of_transaction = ? WHERE transaction_id = ?";

        jdbcTemplate.update(sql, tranStatus, currentDate, currentTime, transactionId);
    }

    @Override
    public void insert(String tableName, Object transaction) {
        try {
            Map<String, Object> paramMap = new HashMap<>();
            List<String> columns = new ArrayList<>();
            List<String> values = new ArrayList<>();

            Class<?> clazz = transaction.getClass();
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                Object value = field.get(transaction);

                if (value != null) {
                    String columnName = getColumnName(field);
                    columns.add(columnName);
                    values.add(":" + columnName);
                    paramMap.put(columnName, value);
                }
            }

            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)",
                    tableName,
                    String.join(", ", columns),
                    String.join(", ", values)
            );

            jdbcTemplate.update(sql, paramMap);
        } catch (Exception e) {
            throw new RuntimeException("Insert failed for table: " + tableName, e);
        }
    }

    private String getColumnName(Field field) {
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return field.getName();
    }

    @Override
    public BigDecimal getBalance(String amountTable, String hierarchyId) {
        String sql = "SELECT available_balance FROM " + amountTable + " WHERE transaction_id = ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, hierarchyId);
    }

    @Override
    public void updateBalance(String amountTable, String hierarchyId, BigDecimal newBalance) {
        String sql = "UPDATE " + amountTable + " SET balance = ? WHERE hierarchy_id = ?";
        jdbcTemplate.update(sql, newBalance, hierarchyId);
    }
}

