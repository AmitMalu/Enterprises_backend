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
import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerPostpaidTransactionDetailsRepository extends JpaRepository<RetailerPostpaidTransactionDetails, Integer>, BaseTransactionRepository<RetailerPostpaidTransactionDetails, Integer> {

    Optional<RetailerPostpaidTransactionDetails> findByTransactionId(String transactionId);

    @Query("SELECT r FROM RetailerPostpaidTransactionDetails r " +
            "WHERE (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate) " +
            "AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate) " +
            "AND r.subProductName = :subProductName " +
            "AND (:vendorName IS NULL OR r.vendorName = :vendorName) " +
            "AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)")
    Page<RetailerPostpaidTransactionDetails> findFilteredTransactions(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("subProductName") String subProductName,
            @Param("vendorName") String vendorName,
            @Param("tranStatusList") List<String> tranStatusList,
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
