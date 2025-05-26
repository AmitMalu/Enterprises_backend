package com.im.admin.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class CommonPhoneNumberRepositoryImpl implements CommonPhoneNumberRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public String findExistingHierarchyByPhoneNumber(String phoneNumber) {
        String query = """
            SELECT CASE
                WHEN EXISTS (SELECT 1 FROM super_admin WHERE mobile_number = :phoneNumber) THEN 'Super Admin'
                WHEN EXISTS (SELECT 1 FROM admin WHERE mobile_number = :phoneNumber) THEN 'Admin'
                WHEN EXISTS (SELECT 1 FROM enterprises WHERE mobile_number = :phoneNumber) THEN 'Enterprise'
                WHEN EXISTS (SELECT 1 FROM white_labels WHERE mobile_number = :phoneNumber) THEN 'Whitelabel'
                WHEN EXISTS (SELECT 1 FROM super_distributor WHERE mobile_number = :phoneNumber) THEN 'SuperDistributor'
                WHEN EXISTS (SELECT 1 FROM partner WHERE mobile_number = :phoneNumber) THEN 'Partner'
                WHEN EXISTS (SELECT 1 FROM distributor WHERE mobile_number = :phoneNumber) THEN 'Distributor'
                WHEN EXISTS (SELECT 1 FROM retailer WHERE mobile_number = :phoneNumber) THEN 'Retailer'
                ELSE NULL END
        """;

        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("phoneNumber", phoneNumber);

        return (String) nativeQuery.getSingleResult();
    }
}

