package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import com.im.admin.entities.DistributorPrepaidTransactionDetails;
import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DistributorPrepaidTransactionDetailsRepository extends JpaRepository<DistributorPrepaidTransactionDetails, Integer>, BaseTransactionRepository<DistributorPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT dt FROM DistributorPrepaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId% " +
            "AND dt.distributorId IN :networkIds")
    List<DistributorPrepaidTransactionDetails> findTransactionsByTransactionIdAndDistributorIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT dt FROM DistributorPrepaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId%")
    List<DistributorPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
