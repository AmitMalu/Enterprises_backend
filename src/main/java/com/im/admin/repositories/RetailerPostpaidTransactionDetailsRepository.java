package com.im.admin.repositories;

import com.im.admin.entities.RetailerPostpaidTransactionDetails;
import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerPostpaidTransactionDetailsRepository extends JpaRepository<RetailerPostpaidTransactionDetails, Integer>, BaseTransactionRepository<RetailerPostpaidTransactionDetails, Integer> {

    Optional<RetailerPostpaidTransactionDetails> findByTransactionId(String transactionId);

    @Query("""
        SELECT r FROM RetailerPostpaidTransactionDetails r
        WHERE (:id IS NULL OR r.id = :id)
        AND (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate)
        AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate)
        AND (:subProductName IS NULL OR r.subProductName = :subProductName)
        AND (:vendorName IS NULL OR r.vendorName = :vendorName)
        AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)
        AND (:userId IS NULL OR r.retailerId LIKE %:userId%)
        AND (:mobileNo IS NULL OR r.mobileNo LIKE %:mobileNo%)
        AND (:schemeName IS NULL OR r.schemeName LIKE %:schemeName%)
        AND (:amount IS NULL OR r.amount = :amount)
        AND (:txId IS NULL OR r.transactionId LIKE %:txId%)
        AND (:remarks IS NULL OR r.remarks LIKE %:remarks%)
        AND (:medium IS NULL OR r.medium LIKE %:medium%)
        AND (:txType IS NULL OR r.transactionType LIKE %:txType%)
        AND (:txStatus IS NULL OR r.tranStatus = :txStatus)
         AND (:dateOfTransaction IS NULL OR r.dateOfTransaction = :dateOfTransaction)
          AND (:timeOfTransaction IS NULL OR r.timeOfTransaction = :timeOfTransaction)
           AND (:updatedDateOfTransaction IS NULL OR r.updatedDateOfTransaction = :updatedDateOfTransaction)
          AND (:updatedTimeOfTransaction IS NULL OR r.updatedTimeOfTransaction = :updatedTimeOfTransaction)
    """)
    Page<RetailerPostpaidTransactionDetails> findFilteredTransactions(
            @Param("id") Integer id,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("subProductName") String subProductName,
            @Param("vendorName") String vendorName,
            @Param("tranStatusList") List<String> tranStatusList,
            @Param("userId") String userId,
            @Param("mobileNo") String mobileNo,
            @Param("schemeName") String schemeName,
            @Param("amount") Double amount,
            @Param("txId") String txId,
            @Param("remarks") String remarks,
            @Param("medium") String medium,
            @Param("txType") String txType,
            @Param("txStatus") String txStatus,
            @Param("dateOfTransaction") LocalDate dateOfTransaction,
            @Param("timeOfTransaction") LocalTime timeOfTransaction,
            @Param("updatedDateOfTransaction") LocalDate updatedDateOfTransaction,
            @Param("updatedTimeOfTransaction") LocalTime updatedTimeOfTransaction,
            Pageable pageable
    );


    @Query("SELECT t FROM RetailerPostpaidTransactionDetails t " +
            "WHERE t.transactionId LIKE %:txnId% AND t.retailerId = :hierarchyId")
    List<RetailerPostpaidTransactionDetails> findTransactionsByTransactionIdAndRetailerId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);

    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT t FROM RetailerPostpaidTransactionDetails t " +
            "WHERE t.transactionId LIKE %:txnId%")
    List<RetailerPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
