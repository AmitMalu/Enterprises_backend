package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelPostpaidTransactionDetails;
import com.im.admin.entities.WhiteLabelPrepaidTransactionDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface WhiteLabelPostpaidTransactionDetailsRepository extends JpaRepository<WhiteLabelPostpaidTransactionDetails, Integer>, BaseTransactionRepository<WhiteLabelPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + whitelabelId
    @Query("SELECT wl FROM WhiteLabelPostpaidTransactionDetails wl " +
            "WHERE wl.transactionId LIKE %:txnId% " +
            "AND wl.whitelabelId IN :networkIds")
    List<WhiteLabelPostpaidTransactionDetails> findTransactionsByTransactionIdAndWhiteLabelIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT wl FROM WhiteLabelPostpaidTransactionDetails wl " +
            "WHERE wl.transactionId LIKE %:txnId%")
    List<WhiteLabelPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT wl FROM WhiteLabelPostpaidTransactionDetails wl
            WHERE (:id IS NULL OR wl.id = :id)
              AND (:fromDate IS NULL OR wl.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR wl.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR wl.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR wl.whitelabelId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(wl.whitelabelId) LIKE LOWER(CONCAT('%', :userId, '%')))
              AND (:mobileNo IS NULL OR LOWER(wl.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
              AND (:schemeName IS NULL OR LOWER(wl.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
              AND (:amount IS NULL OR wl.amount = :amount)
              AND (wl.transactionId LIKE 'TXN%'
                   OR wl.transactionId LIKE 'FLD%'
                   OR wl.transactionId LIKE 'REVTXN%')
              AND (:txId IS NULL OR LOWER(wl.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
              AND (:remarks IS NULL OR LOWER(wl.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
              AND (:actionOnBalanceAmount IS NULL
                   OR LOWER(wl.actionOnBalanceAmount) LIKE CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%'))
              AND (:txStatus IS NULL OR LOWER(wl.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
              AND (:dateOfTransaction IS NULL OR wl.dateOfTransaction = :dateOfTransaction)
              AND (:timeOfTransaction IS NULL OR CAST(wl.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%'))
              AND (:updatedDateOfTransaction IS NULL OR wl.updatedDateOfTransaction = :updatedDateOfTransaction)
            """)
    Page<WhiteLabelPostpaidTransactionDetails> findFilteredTransactionTXN(
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
