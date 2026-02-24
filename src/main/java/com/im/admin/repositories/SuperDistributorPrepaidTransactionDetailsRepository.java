package com.im.admin.repositories;

import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import com.im.admin.entities.WhiteLabelPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SuperDistributorPrepaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPrepaidTransactionDetails, Integer>, BaseTransactionRepository<SuperDistributorPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + superdistributorId
    @Query("SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% AND pt.superDistributorId = :hierarchyId")
    List<SuperDistributorPrepaidTransactionDetails> findTransactionsByTransactionIdAndSuperDistributorId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<SuperDistributorPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM SuperDistributorPrepaidTransactionDetails t
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
