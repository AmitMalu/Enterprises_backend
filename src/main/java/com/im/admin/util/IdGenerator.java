package com.im.admin.util;

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
            prefix = "IM-SA-";
        } else if ("Admin".equals(entityType)) {
            prefix = "IM-AD-";
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

        // Format new ID (e.g., IM-SA-001, IM-SA-002)
        return String.format("%s%03d", prefix, nextId);
    }

    public String generateHierarchyEntityId(String entityType, String abbreviation) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            throw new IllegalArgumentException("Abbreviation cannot be null or empty.");
        }

        String prefix = switch (entityType) {
            case "Enterprises" -> abbreviation + "-ET-";
            case "White Label" -> abbreviation + "-WL-";
            case "Super Distributor" -> abbreviation + "-SD-";
            case "Partner" -> abbreviation + "-PT-";
            case "Distributor" -> abbreviation + "-DT-";
            case "Retailer" -> abbreviation + "-RT-";
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



}

