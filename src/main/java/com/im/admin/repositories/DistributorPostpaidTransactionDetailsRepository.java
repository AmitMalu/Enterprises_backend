package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import com.im.admin.entities.DistributorPrepaidTransactionDetails;
import com.im.admin.entities.EnterprisesPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DistributorPostpaidTransactionDetailsRepository extends JpaRepository<DistributorPostpaidTransactionDetails, Integer>, BaseTransactionRepository<DistributorPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT dt FROM DistributorPostpaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId% " +
            "AND dt.distributorId IN :networkIds")
    List<DistributorPostpaidTransactionDetails> findTransactionsByTransactionIdAndDistributorIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT dt FROM DistributorPostpaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId%")
    List<DistributorPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
