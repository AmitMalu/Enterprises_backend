package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import com.im.admin.entities.WhiteLabelPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhiteLabelPrepaidTransactionDetailsRepository extends JpaRepository<WhiteLabelPrepaidTransactionDetails, Integer>, BaseTransactionRepository<WhiteLabelPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + whitelabelId
    @Query("SELECT wl FROM WhiteLabelPrepaidTransactionDetails wl " +
            "WHERE wl.transactionId LIKE %:txnId% " +
            "AND wl.whitelabelId IN :networkIds")
    List<WhiteLabelPrepaidTransactionDetails> findTransactionsByTransactionIdAndWhiteLabelIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);




    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT wl FROM WhiteLabelPrepaidTransactionDetails wl " +
            "WHERE wl.transactionId LIKE %:txnId%")
    List<WhiteLabelPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
