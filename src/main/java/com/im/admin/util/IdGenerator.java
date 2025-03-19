package com.im.admin.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class IdGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String generateId(String type) {

        String prefix = "IM-" + type + "-";
        String tableName = type.equals("SA") ? "super_admin" : "admin";
        String alias = type.equals("SA") ? "s" : "a";

        String query = "SELECT COALESCE(MAX(CAST(SUBSTRING(" + alias + ".id, LENGTH(:prefix) + 1, LENGTH(" + alias + ".id)) AS integer)), 0) + 1 " +
                "FROM " + tableName + " " + alias;

        Integer sequence = (Integer) entityManager.createQuery(query)
                .setParameter("prefix", prefix)
                .getSingleResult();

        return prefix + String.format("%02d", sequence);
    }

}

