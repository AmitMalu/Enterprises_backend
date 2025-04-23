package com.im.admin.repositories;

import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DynamicTransactionRepository {

    List<Object> findTransactionsByBaseId(String tableName, String baseId);

    void save(String tableName, Object transaction);

    void insert(String tableName, Object transaction);

    BigDecimal getBalance(String amountTable, String hierarchyId);

    void updateBalance(String amountTable, String hierarchyId, BigDecimal newBalance);
}

