package com.scs.admin.repositories;

import com.scs.admin.dto.DynamicWalletDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Repository
public class DynamicWalletRepository {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public DynamicWalletDTO findWallet(String tableName, String idColumnName, String idValue) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{idValue}, (rs, rowNum) -> {
            DynamicWalletDTO dto = new DynamicWalletDTO();
            dto.setTotalCash(rs.getBigDecimal("total_cash"));
            dto.setUsedCash(rs.getBigDecimal("used_cash"));
            dto.setCutOfAmount(rs.getBigDecimal("cut_of_amount"));
            dto.setAvailableBalance(rs.getBigDecimal("available_balance"));
            dto.setLastUpdateDate(rs.getDate("last_updated_date"));
            dto.setLastUpdateTime(rs.getTime("last_updated_time"));
            dto.setLastUpdatedAmount(rs.getBigDecimal("last_updated_amount"));
            return dto;
        });
    }

    public void updateWallet(String tableName, String idColumnName, String idValue, BigDecimal newBalance, BigDecimal lastUpdatedAmount) {
        String sql = "UPDATE " + tableName + " SET available_balance = ?, last_updated_amount = ?, last_updated_date = ?, last_updated_time = ? WHERE " + idColumnName + " = ?";
        jdbcTemplate.update(sql, newBalance, lastUpdatedAmount, new Date(), new Time(System.currentTimeMillis()), idValue);
    }

}

