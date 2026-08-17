package com.im.admin.repositories;

import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import com.im.admin.entities.WhiteLabelPostpaidTransactionDetails;
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
public interface SuperDistributorPrepaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPrepaidTransactionDetails, Integer>, BaseTransactionRepository<SuperDistributorPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + superdistributorId
    @Query("SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% " +
            "AND pt.superDistributorId IN :networkIds")
    List<SuperDistributorPrepaidTransactionDetails> findTransactionsByTransactionIdAndSuperDistributorIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



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

    @Query("""
    SELECT new com.im.admin.response.DashboardTransactionDto(
        t.dateOfTransaction,
        t.timeOfTransaction,
        t.amount,
        p.name,
        sp.walletType,
        t.tranStatus
    )
    FROM SuperDistributorPrepaidTransactionDetails t
    LEFT JOIN SubProducts sp ON sp.subProductTitle = t.subProductName
    LEFT JOIN sp.product p
    WHERE t.superDistributorId = :superDistributorId
    ORDER BY t.dateOfTransaction DESC, t.timeOfTransaction DESC
""")
    List<DashboardTransactionDto> fetchDashboardTransactions(
            @Param("superDistributorId") String superDistributorId
    );

    boolean existsByTransactionId(String transactionId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt
            WHERE (:id IS NULL OR pt.id = :id)
              AND (:fromDate IS NULL OR pt.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR pt.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR pt.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR pt.superDistributorId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(pt.superDistributorId) LIKE LOWER(CONCAT('%', :userId, '%')))
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
    Page<SuperDistributorPrepaidTransactionDetails> findFilteredTransactionTXN(
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
