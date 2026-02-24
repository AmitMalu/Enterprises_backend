package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPostpaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SuperDistributorPostpaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPostpaidTransactionDetails, Integer>, BaseTransactionRepository<SuperDistributorPostpaidTransactionDetails, Integer> {


    // Search by normalized transactionId + distributorId
    @Query("SELECT sd FROM SuperDistributorPostpaidTransactionDetails sd " +
            "WHERE sd.transactionId LIKE %:txnId% " +
            "AND sd.superDistributorId IN :networkIds")
    List<SuperDistributorPostpaidTransactionDetails> findTransactionsByTransactionIdAndSuperDistributorIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT sd FROM SuperDistributorPostpaidTransactionDetails sd " +
            "WHERE sd.transactionId LIKE %:txnId%")
    List<SuperDistributorPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM SuperDistributorPostpaidTransactionDetails t
       WHERE t.superDistributorId = :sdId
       AND t.transactionId LIKE 'COM%'
       AND t.tranStatus = 'SUCCESS'
       AND (:fromDate IS NULL OR t.dateOfTransaction >= :fromDate)
       AND (:toDate IS NULL OR t.dateOfTransaction <= :toDate)
       """)
    BigDecimal sumCommission(
            @Param("sdId") String sdId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
