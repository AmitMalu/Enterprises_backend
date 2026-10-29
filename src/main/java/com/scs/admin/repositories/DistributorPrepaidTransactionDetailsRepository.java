package com.scs.admin.repositories;

import com.scs.admin.entities.DistributorPostpaidTransactionDetails;
import com.scs.admin.entities.DistributorPrepaidTransactionDetails;
import com.scs.admin.entities.RetailerPrepaidTransactionDetails;
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

    boolean existsByTransactionId(String transactionId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT dt FROM DistributorPrepaidTransactionDetails dt
            WHERE (:id IS NULL OR dt.id = :id)
              AND (:fromDate IS NULL OR dt.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR dt.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR dt.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR dt.distributorId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(dt.distributorId) LIKE LOWER(CONCAT('%', :userId, '%')))
              AND (:mobileNo IS NULL OR LOWER(dt.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
              AND (:schemeName IS NULL OR LOWER(dt.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
              AND (:amount IS NULL OR dt.amount = :amount)
              AND (dt.transactionId LIKE 'TXN%'
                   OR dt.transactionId LIKE 'FLD%'
                   OR dt.transactionId LIKE 'REVTXN%')
              AND (:txId IS NULL OR LOWER(dt.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
              AND (:remarks IS NULL OR LOWER(dt.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
              AND (:actionOnBalanceAmount IS NULL
                   OR LOWER(dt.actionOnBalanceAmount) LIKE CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%'))
              AND (:txStatus IS NULL OR LOWER(dt.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
              AND (:dateOfTransaction IS NULL OR dt.dateOfTransaction = :dateOfTransaction)
              AND (:timeOfTransaction IS NULL OR CAST(dt.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%'))
              AND (:updatedDateOfTransaction IS NULL OR dt.updatedDateOfTransaction = :updatedDateOfTransaction)
            """)
    Page<DistributorPrepaidTransactionDetails> findFilteredTransactionTXN(
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
