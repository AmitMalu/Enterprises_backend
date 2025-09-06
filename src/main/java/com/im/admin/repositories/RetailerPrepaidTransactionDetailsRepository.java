package com.im.admin.repositories;

import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RetailerPrepaidTransactionDetailsRepository extends JpaRepository<RetailerPrepaidTransactionDetails, Integer>, BaseTransactionRepository<RetailerPrepaidTransactionDetails, Integer> {

    Optional<RetailerPrepaidTransactionDetails> findByTransactionId(String transactionId);

    @Query("SELECT r FROM RetailerPrepaidTransactionDetails r " +
            "WHERE (:fromDate IS NULL OR r.dateOfTransaction >= :fromDate) " +
            "AND (:toDate IS NULL OR r.dateOfTransaction <= :toDate) " +
            "AND r.subProductName = :subProductName " +
            "AND (:vendorName IS NULL OR r.vendorName = :vendorName) " +
            "AND (:tranStatusList IS NULL OR r.tranStatus IN :tranStatusList)")
    Page<RetailerPrepaidTransactionDetails> findFilteredTransactions(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("subProductName") String subProductName,
            @Param("vendorName") String vendorName,
            @Param("tranStatusList") List<String> tranStatusList,
            Pageable pageable
    );

    // Search by normalized transactionId + retailerId
    @Query("SELECT t FROM RetailerPrepaidTransactionDetails t " +
            "WHERE t.transactionId LIKE %:txnId% AND t.retailerId = :hierarchyId")
    List<RetailerPrepaidTransactionDetails> findTransactionsByTransactionIdAndRetailerId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT t FROM RetailerPrepaidTransactionDetails t " +
            "WHERE t.transactionId LIKE %:txnId%")
    List<RetailerPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
