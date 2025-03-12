package com.im.admin.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CommonMethods {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public String generateNextIdForTables(String tableName, String idColumn) {

        String query = "SELECT MAX(" + idColumn + ") FROM " + tableName;
        String maxId = jdbcTemplate.queryForObject(query, String.class);

        if(maxId == null) {
            return "01";
        }

        int nextId = Integer.parseInt(maxId) + 1;
        if(nextId > 99) {
            throw new IllegalStateException("ID limit reached. Cannot exceed 99.");
        }

        return String.format("%02d", nextId); // Format as "01", "02", ..., "99"

    }

}
