package com.im.admin.repositories;

import com.im.admin.entities.EnterprisesPrepaidTransactionDetails;
import com.im.admin.entities.WhiteLabelPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnterprisesPrepaidTransactionDetailsRepository extends JpaRepository<EnterprisesPrepaidTransactionDetails, Integer>, BaseTransactionRepository<EnterprisesPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + enterprisesId
    @Query("SELECT et FROM EnterprisesPrepaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId% " +
            "AND et.enterprisesId IN :networkIds")
    List<EnterprisesPrepaidTransactionDetails> findTransactionsByTransactionIdAndEnterprisesIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT et FROM EnterprisesPrepaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId%")
    List<EnterprisesPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
