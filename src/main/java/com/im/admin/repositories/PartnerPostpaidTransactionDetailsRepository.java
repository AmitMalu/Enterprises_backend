package com.im.admin.repositories;


import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import com.im.admin.response.DashboardTransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface PartnerPostpaidTransactionDetailsRepository extends JpaRepository<PartnerPostpaidTransactionDetails, Integer>, BaseTransactionRepository<PartnerPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT pt FROM PartnerPostpaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% " +
            "AND pt.partnerId IN :networkIds")
    List<PartnerPostpaidTransactionDetails> findTransactionsByTransactionIdAndPartnerIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM PartnerPostpaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<PartnerPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    @Query("""
       SELECT COALESCE(SUM(t.amount), 0)
       FROM PartnerPostpaidTransactionDetails t
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
    FROM PartnerPostpaidTransactionDetails pt
    LEFT JOIN SubProducts sp ON sp.subProductTitle = pt.subProductName
    LEFT JOIN sp.product p
    WHERE pt.partnerId = :partnerId
    ORDER BY pt.dateOfTransaction DESC, pt.timeOfTransaction DESC
""")
    List<DashboardTransactionDto> fetchPTDashboardTransactions(
            @Param("partnerId") String partnerId
    );

    boolean existsByTransactionId(String transactionId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT pt FROM PartnerPostpaidTransactionDetails pt
            WHERE (:id IS NULL OR pt.id = :id)
              AND (:fromDate IS NULL OR pt.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR pt.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR pt.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR pt.partnerId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(pt.partnerId) LIKE LOWER(CONCAT('%', :userId, '%')))
              AND (:mobileNo IS NULL OR LOWER(pt.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
              AND (:schemeName IS NULL OR LOWER(pt.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
              AND (:amount IS NULL OR pt.amount = :amount)
              AND (pt.transactionId LIKE 'TXN%'
                   OR pt.transactionId LIKE 'FLD%'
                   OR pt.transactionId LIKE 'REVTXN%')
              AND (:txId IS NULL OR LOWER(pt.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
              AND (:remarks IS NULL OR LOWER(pt.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
              AND (:actionOnBalanceAmount IS NULL
                   OR LOWER(pt.actionOnBalanceAmount) LIKE CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%'))
              AND (:txStatus IS NULL OR LOWER(pt.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
              AND (:dateOfTransaction IS NULL OR pt.dateOfTransaction = :dateOfTransaction)
              AND (:timeOfTransaction IS NULL OR CAST(pt.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%'))
              AND (:updatedDateOfTransaction IS NULL OR pt.updatedDateOfTransaction = :updatedDateOfTransaction)
            """)
    Page<PartnerPostpaidTransactionDetails> findFilteredTransactionTXN(
            @Param("id") Integer id,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("subProductNames") List<String> subProductNames,
            @Param("hierarchyIds") List<String> hierarchyIds,
            @Param("userId") String userId,
            @Param("mobileNo") String mobileNo,
            @Param("schemeName") String schemeName,
            @Param("amount") Double amount,
            @Param("txId") String txId,
            @Param("remarks") String remarks,
            @Param("actionOnBalanceAmount") String actionOnBalanceAmount,
            @Param("txStatus") String txStatus,
            @Param("dateOfTransaction") LocalDate dateOfTransaction,
            @Param("timeOfTransaction") LocalTime timeOfTransaction,
            @Param("updatedDateOfTransaction") LocalDate updatedDateOfTransaction,
            Pageable pageable);

}
