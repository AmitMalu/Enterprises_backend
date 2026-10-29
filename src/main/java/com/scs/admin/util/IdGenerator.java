package com.scs.admin.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class IdGenerator {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String generateId(String entityType) {
        String prefix;

        if ("SuperAdmin".equals(entityType)) {
            prefix = "SCS-SA-";
        } else if ("Admin".equals(entityType)) {
            prefix = "SCS-AD-";
        } else {
            throw new IllegalArgumentException("Invalid entity type: " + entityType);
        }

        // Query to fetch the last used ID for the given entityType
        String selectQuery = "SELECT last_id FROM id_tracker WHERE entity_type = :entityType FOR UPDATE";

        Integer lastId = (Integer) entityManager.createNativeQuery(selectQuery)
                .setParameter("entityType", entityType)
                .getResultList()
                .stream()
                .findFirst()
                .orElse(0); // Default to 0 if no entry exists

        int nextId = lastId + 1; // Always increment

        // Update the tracker table with the new last_id
        String updateQuery = "INSERT INTO id_tracker (entity_type, last_id) VALUES (:entityType, :nextId) " +
                "ON DUPLICATE KEY UPDATE last_id = :nextId";

        entityManager.createNativeQuery(updateQuery)
                .setParameter("entityType", entityType)
                .setParameter("nextId", nextId)
                .executeUpdate();

        // Format new ID (e.g., SCS-SA-001, SCS-SA-002)
        return String.format("%s%03d", prefix, nextId);
    }

    public String generateHierarchyEntityId(String entityType, String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            throw new IllegalArgumentException("Abbreviation cannot be null or empty.");
        }

        String prefix = switch (entityType) {
            case "Admin User" -> abbreviation + "-AU-";
            case "Enterprises" -> abbreviation + "-ET-";
            case "White Label" -> abbreviation + "-WL-";
            case "White Label User" -> abbreviation + "-WLAU-";
            case "Super Distributor" -> abbreviation + "-SD-";
            case "Partner" -> abbreviation + "-PT-";
            case "Distributor" -> abbreviation + "-DT-";
            case "Retailer" -> abbreviation + "-RT-";
            case "RetailerUser" -> abbreviation + "-RU-";
            default -> throw new IllegalArgumentException("Invalid entity type: " + entityType);
        };

        int nextId = 1;

        // Try fetching the last_id for this abbreviation + entity type
        List<Integer> lastIdList = entityManager.createNativeQuery(
                        "SELECT last_id FROM id_tracker WHERE entity_type = :entityType AND abbreviation = :abbr FOR UPDATE")
                .setParameter("entityType", entityType)
                .setParameter("abbr", abbreviation)
                .getResultList();

        if (!lastIdList.isEmpty()) {
            nextId = lastIdList.get(0) + 1;

            // Update the existing last_id
            entityManager.createNativeQuery(
                            "UPDATE id_tracker SET last_id = :nextId WHERE entity_type = :entityType AND abbreviation = :abbr")
                    .setParameter("nextId", nextId)
                    .setParameter("entityType", entityType)
                    .setParameter("abbr", abbreviation)
                    .executeUpdate();
        } else {
            // Insert new abbreviation + entity type entry
            entityManager.createNativeQuery(
                            "INSERT INTO id_tracker (entity_type, abbreviation, last_id) VALUES (:entityType, :abbr, :nextId)")
                    .setParameter("entityType", entityType)
                    .setParameter("abbr", abbreviation)
                    .setParameter("nextId", nextId)
                    .executeUpdate();
        }

        return String.format("%s%03d", prefix, nextId);
    }


    @Transactional
    public String generateEntityId(String entityType, String abbreviation) {
        String prefix;

        // Determine prefix based on entityType and whether abbreviation is used
        switch (entityType) {
            case "SuperAdmin" -> {
                prefix = "SCS-SA-";
                abbreviation = "SCS"; // Force abbreviation to null
            }
            case "Admin" -> {
                prefix = "SCS-AD-";
                abbreviation = "SCS";
            }
            case "Enterprises" -> prefix = abbreviation + "-ET-";
            case "White Label" -> prefix = abbreviation + "-WL-";
            case "Super Distributor" -> prefix = abbreviation + "-SD-";
            case "Partner" -> prefix = abbreviation + "-PT-";
            case "Distributor" -> prefix = abbreviation + "-DT-";
            case "Retailer" -> prefix = abbreviation + "-RT-";
            default -> throw new IllegalArgumentException("Invalid entity type: " + entityType);
        }

        // Validation for abbreviation when required
        if (abbreviation == null && !entityType.equals("SuperAdmin") && !entityType.equals("Admin")) {
            throw new IllegalArgumentException("Abbreviation is required for entity type: " + entityType);
        }

        // Fetch last_id
        List<Integer> lastIdList = entityManager.createNativeQuery("""
        SELECT last_id FROM id_tracker 
        WHERE entity_type = :entityType AND (:abbr IS NULL OR abbreviation = :abbr) FOR UPDATE
        """)
                .setParameter("entityType", entityType)
                .setParameter("abbr", abbreviation)
                .getResultList();

        int nextId = lastIdList.isEmpty() ? 1 : lastIdList.get(0) + 1;

        if (lastIdList.isEmpty()) {
            // Insert new record
            entityManager.createNativeQuery("""
            INSERT INTO id_tracker (entity_type, abbreviation, last_id)
            VALUES (:entityType, :abbr, :nextId)
            """)
                    .setParameter("entityType", entityType)
                    .setParameter("abbr", abbreviation)
                    .setParameter("nextId", nextId)
                    .executeUpdate();
        } else {
            // Update existing record
            entityManager.createNativeQuery("""
            UPDATE id_tracker SET last_id = :nextId 
            WHERE entity_type = :entityType AND (:abbr IS NULL OR abbreviation = :abbr)
            """)
                    .setParameter("nextId", nextId)
                    .setParameter("entityType", entityType)
                    .setParameter("abbr", abbreviation)
                    .executeUpdate();
        }

        return String.format("%s%03d", prefix, nextId);
    }




}

