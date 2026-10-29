package com.scs.admin.repositories;

import com.scs.admin.entities.DistributorPrepaidTransactionDetails;
import com.scs.admin.entities.EnterprisesPostpaidTransactionDetails;
import com.scs.admin.entities.EnterprisesPrepaidTransactionDetails;
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
public interface EnterprisesPostpaidTransactionDetailsRepository extends JpaRepository<EnterprisesPostpaidTransactionDetails, Integer>, BaseTransactionRepository<EnterprisesPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + enterprisesId
    @Query("SELECT et FROM EnterprisesPostpaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId% " +
            "AND et.enterprisesId IN :networkIds")
    List<EnterprisesPostpaidTransactionDetails> findTransactionsByTransactionIdAndEnterprisesIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT et FROM EnterprisesPostpaidTransactionDetails et " +
            "WHERE et.transactionId LIKE %:txnId%")
    List<EnterprisesPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT et FROM EnterprisesPostpaidTransactionDetails et
            WHERE (:id IS NULL OR et.id = :id)
              AND (:fromDate IS NULL OR et.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR et.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR et.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR et.enterprisesId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(et.enterprisesId) LIKE LOWER(CONCAT('%', :userId, '%')))
              AND (:mobileNo IS NULL OR LOWER(et.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
              AND (:schemeName IS NULL OR LOWER(et.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
              AND (:amount IS NULL OR et.amount = :amount)
              AND (et.transactionId LIKE 'TXN%'
                   OR et.transactionId LIKE 'FLD%'
                   OR et.transactionId LIKE 'REVTXN%')
              AND (:txId IS NULL OR LOWER(et.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
              AND (:remarks IS NULL OR LOWER(et.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
              AND (:actionOnBalanceAmount IS NULL
                   OR LOWER(et.actionOnBalanceAmount) LIKE CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%'))
              AND (:txStatus IS NULL OR LOWER(et.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
              AND (:dateOfTransaction IS NULL OR et.dateOfTransaction = :dateOfTransaction)
              AND (:timeOfTransaction IS NULL OR CAST(et.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%'))
              AND (:updatedDateOfTransaction IS NULL OR et.updatedDateOfTransaction = :updatedDateOfTransaction)
            """)
    Page<EnterprisesPostpaidTransactionDetails> findFilteredTransactionTXN(
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
