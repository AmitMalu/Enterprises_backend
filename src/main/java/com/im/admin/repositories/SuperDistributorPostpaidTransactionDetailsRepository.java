package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPostpaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperDistributorPostpaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPostpaidTransactionDetails, Integer> {


    // Search by normalized transactionId + distributorId
    @Query("SELECT sd FROM SuperDistributorPostpaidTransactionDetails sd " +
            "WHERE sd.transactionId LIKE %:txnId% AND sd.superDistributorId = :hierarchyId")
    List<SuperDistributorPostpaidTransactionDetails> findTransactionsByTransactionIdAndSuperDistributorId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT sd FROM SuperDistributorPostpaidTransactionDetails sd " +
            "WHERE sd.transactionId LIKE %:txnId%")
    List<SuperDistributorPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
