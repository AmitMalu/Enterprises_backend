package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPostpaidTransactionDetails;
import com.im.admin.response.DashboardTransactionDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PartnerPrepaidTransactionDetailsRepository extends JpaRepository<PartnerPrepaidTransactionDetails, Integer>, BaseTransactionRepository<PartnerPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT pt FROM PartnerPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% " +
            "AND pt.partnerId IN :networkIds")
    List<PartnerPrepaidTransactionDetails> findTransactionsByTransactionIdAndPartnerIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds,
            @Param("hierarchyId") String hierarchyId,
            @Param("networkIds") List<String> networkIds
        );

    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM PartnerPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<PartnerPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM PartnerPrepaidTransactionDetails t
       WHERE t.partnerId = :ptId
       AND t.transactionId LIKE 'COM%'
       AND t.tranStatus = 'SUCCESS'
       AND (:fromDate IS NULL OR t.dateOfTransaction >= :fromDate)
       AND (:toDate IS NULL OR t.dateOfTransaction <= :toDate)
       """)
    BigDecimal sumCommission(
            @Param("ptId") String ptId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Query("""
    SELECT new com.im.admin.response.DashboardTransactionDto(
        pt.dateOfTransaction,
        pt.timeOfTransaction,
        pt.amount,
        p.name,
        sp.walletType,
        pt.tranStatus
    )
    FROM PartnerPrepaidTransactionDetails pt
    LEFT JOIN SubProducts sp ON sp.subProductTitle = pt.subProductName
    LEFT JOIN sp.product p
    WHERE pt.partnerId = :partnerId
    ORDER BY pt.dateOfTransaction DESC, pt.timeOfTransaction DESC
""")
    List<DashboardTransactionDto> fetchPTDashboardTransactions(
            @Param("partnerId") String partnerId
    );

}
