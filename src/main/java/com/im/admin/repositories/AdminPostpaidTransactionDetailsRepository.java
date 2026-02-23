package com.im.admin.repositories;

import com.im.admin.entities.AdminPostpaidTransactionDetails;
import com.im.admin.entities.AdminPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminPostpaidTransactionDetailsRepository extends JpaRepository<AdminPostpaidTransactionDetails, Integer>, BaseTransactionRepository<AdminPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + enterprisesId
    @Query("SELECT ad FROM AdminPostpaidTransactionDetails ad " +
            "WHERE ad.transactionId LIKE %:txnId% " +
            "AND ad.hierarchyId IN :networkIds")
    List<AdminPostpaidTransactionDetails> findTransactionsByTransactionIdAndAdminId(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT ad FROM AdminPostpaidTransactionDetails ad " +
            "WHERE ad.transactionId LIKE %:txnId%")
    List<AdminPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
