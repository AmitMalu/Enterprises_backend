package com.im.admin.repositories;

import com.im.admin.entities.AdminPrepaidTransactionDetails;
import com.im.admin.entities.EnterprisesPrepaidTransactionDetails;
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
public interface AdminPrepaidTransactionDetailsRepository extends JpaRepository<AdminPrepaidTransactionDetails, Integer>, BaseTransactionRepository<AdminPrepaidTransactionDetails, Integer> {


    // Search by normalized transactionId + enterprisesId
    @Query("SELECT ad FROM AdminPrepaidTransactionDetails ad " +
            "WHERE ad.transactionId LIKE %:txnId% " +
            "AND ad.hierarchyId IN :networkIds")
    List<AdminPrepaidTransactionDetails> findTransactionsByTransactionIdAndAdminId(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT ad FROM AdminPrepaidTransactionDetails ad " +
            "WHERE ad.transactionId LIKE %:txnId%")
    List<AdminPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

    // Filtered transaction search scoped to the logged-in user only (no child/network rows)
    @Query("""
            SELECT ad FROM AdminPrepaidTransactionDetails ad
            WHERE (:id IS NULL OR ad.id = :id)
              AND (:fromDate IS NULL OR ad.dateOfTransaction >= :fromDate)
              AND (:toDate IS NULL OR ad.dateOfTransaction <= :toDate)
              AND (:subProductNames IS NULL OR ad.subProductName IN :subProductNames)
              AND (:hierarchyIds IS NULL OR ad.adminId IN :hierarchyIds)
              AND (:userId IS NULL OR LOWER(ad.adminId) LIKE LOWER(CONCAT('%', :userId, '%')))
              AND (:mobileNo IS NULL OR LOWER(ad.mobileNo) LIKE LOWER(CONCAT('%', :mobileNo, '%')))
              AND (:schemeName IS NULL OR LOWER(ad.schemeName) LIKE LOWER(CONCAT('%', :schemeName, '%')))
              AND (:amount IS NULL OR ad.amount = :amount)
              AND (ad.transactionId LIKE 'TXN%'
                   OR ad.transactionId LIKE 'FLD%'
                   OR ad.transactionId LIKE 'REVTXN%')
              AND (:txId IS NULL OR LOWER(ad.transactionId) LIKE LOWER(CONCAT('%', :txId, '%')))
              AND (:remarks IS NULL OR LOWER(ad.remarks) LIKE LOWER(CONCAT('%', :remarks, '%')))
              AND (:actionOnBalanceAmount IS NULL
                   OR LOWER(ad.actionOnBalanceAmount) LIKE CONCAT('%', LOWER(TRIM(:actionOnBalanceAmount)), '%'))
              AND (:txStatus IS NULL OR LOWER(ad.tranStatus) LIKE LOWER(CONCAT('%', :txStatus, '%')))
              AND (:dateOfTransaction IS NULL OR ad.dateOfTransaction = :dateOfTransaction)
              AND (:timeOfTransaction IS NULL OR CAST(ad.timeOfTransaction AS string) LIKE CONCAT(:timeOfTransaction, '%'))
              AND (:updatedDateOfTransaction IS NULL OR ad.updatedDateOfTransaction = :updatedDateOfTransaction)
            """)
    Page<AdminPrepaidTransactionDetails> findFilteredTransactionTXN(
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
