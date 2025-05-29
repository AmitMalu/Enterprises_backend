package com.im.admin.service.impl;

import com.im.admin.service.TicketService;
import com.im.admin.util.CommonMethods;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TicketServiceImpl implements TicketService {
    @Autowired
    private CommonMethods commonMethods;

    @PersistenceContext
    private EntityManager entityManager;


    @Override
    public Object getTransactionDetails(String transactionId, String hierarchyId, String walletType) {
        String hierarchyType = commonMethods.extractHierarchyType(hierarchyId);
        String tableName = commonMethods.getTransactionTable(hierarchyType, walletType);

        String sql = "SELECT * FROM " + tableName + " WHERE transaction_id = :transactionId";

        try {
            Query query = entityManager.createNativeQuery(sql, Map.class);
            query.setParameter("transactionId", transactionId);
            return query.getSingleResult();
        } catch (NoResultException e) {
            throw new RuntimeException("No record found for transactionId: " + transactionId);
        }
    }
}

