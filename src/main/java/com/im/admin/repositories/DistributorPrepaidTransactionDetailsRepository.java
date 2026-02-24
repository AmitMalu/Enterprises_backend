package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import com.im.admin.entities.DistributorPrepaidTransactionDetails;
import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DistributorPrepaidTransactionDetailsRepository extends JpaRepository<DistributorPrepaidTransactionDetails, Integer>, BaseTransactionRepository<DistributorPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT dt FROM DistributorPrepaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId% AND dt.distributorId = :hierarchyId")
    List<DistributorPrepaidTransactionDetails> findTransactionsByTransactionIdAndDistributorId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT dt FROM DistributorPrepaidTransactionDetails dt " +
            "WHERE dt.transactionId LIKE %:txnId%")
    List<DistributorPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM DistributorPrepaidTransactionDetails t
       WHERE t.distributorId = :dtId
       AND t.transactionId LIKE 'COM%'
       AND t.tranStatus = 'SUCCESS'
       AND (:fromDate IS NULL OR t.dateOfTransaction >= :fromDate)
       AND (:toDate IS NULL OR t.dateOfTransaction <= :toDate)
       """)
    BigDecimal sumCommission(
            @Param("dtId") String dtId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

}
