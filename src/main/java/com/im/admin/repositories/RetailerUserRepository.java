package com.im.admin.repositories;

import com.im.admin.entities.RetailerUser;
import com.im.admin.spec.projectionFilter.RetailerUserProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RetailerUserRepository extends JpaRepository<RetailerUser,String> {

    long countByParentAccount(String retailerId);

    @Query("SELECT r.retailerUserId AS retailerUserId, " +
            "r.name AS name, " +
            "r.mobileNumber AS mobileNumber, " +
            "r.emailId AS emailId, " +
            "r.emailVerified AS emailVerified, " +
            "r.status AS status, " +
            "r.createdOn AS createdOn, " +
            "r.createdBy AS createdBy, " +
            "r.lastEditedOn AS lastEditedOn, " +
            "r.editedBy AS editedBy " +
            "FROM RetailerUser r " +
            "WHERE (:name IS NULL OR r.name LIKE %:name%) " +
            "AND (:mobileNumber IS NULL OR r.mobileNumber LIKE %:mobileNumber%) " +
            "AND (:emailId IS NULL OR r.emailId LIKE %:emailId%) " +
            "AND (:emailVerified IS NULL OR r.emailVerified = :emailVerified) " +
            "AND (:status IS NULL OR r.status = :status)")
    Page<RetailerUserProjection> findFiltered(
            @Param("name") String name,
            @Param("mobileNumber") String mobileNumber,
            @Param("emailId") String emailId,
            @Param("emailVerified") Boolean emailVerified,
            @Param("status") Boolean status,
            Pageable pageable
    );


    @Query("SELECT ru FROM RetailerUser ru LEFT JOIN FETCH ru.retailer WHERE ru.retailerUserId = :id")
    Optional<RetailerUser> findByIdWithRetailer(@Param("id") String id);

}
