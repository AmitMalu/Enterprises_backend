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

    @Transactional
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

        // Fetch last_id from id_tracker
        List<Integer> lastIdList = entityManager.createNativeQuery(
                        "SELECT last_id FROM id_tracker WHERE entity_type = :entityType FOR UPDATE")
                .setParameter("entityType", entityType)
                .getResultList();

        int nextId = 1; // Default to 1 if no record exists

        if (!lastIdList.isEmpty()) {
            nextId = lastIdList.get(0) + 1; // Increment last_id
        } else {
            // Insert new entry if it does not exist
            entityManager.createNativeQuery(
                            "INSERT INTO id_tracker (entity_type, last_id) VALUES (:entityType, :nextId)")
                    .setParameter("entityType", entityType)
                    .setParameter("nextId", nextId)
                    .executeUpdate();
        }

        // Update last_id in id_tracker
        entityManager.createNativeQuery(
                        "UPDATE id_tracker SET last_id = :nextId WHERE entity_type = :entityType")
                .setParameter("nextId", nextId)
                .setParameter("entityType", entityType)
                .executeUpdate();

        // Corrected return statement
        return String.format("%s%03d", prefix, nextId);
    }


}

