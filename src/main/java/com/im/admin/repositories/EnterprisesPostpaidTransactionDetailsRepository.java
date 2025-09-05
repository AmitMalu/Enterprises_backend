package com.im.admin.repositories;

import com.im.admin.entities.EnterprisesPostpaidTransactionDetails;
import com.im.admin.entities.EnterprisesPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnterprisesPostpaidTransactionDetailsRepository extends JpaRepository<EnterprisesPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + enterprisesId
    @Query("SELECT et FROM EnterprisesPostpaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId% AND et.enterprisesId = :hierarchyId")
    List<EnterprisesPostpaidTransactionDetails> findTransactionsByTransactionIdAndEnterprisesId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);

    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT et FROM EnterprisesPostpaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId%")
    List<EnterprisesPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
