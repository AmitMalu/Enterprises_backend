package com.im.admin.repositories;

import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperDistributorPrepaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + superdistributorId
    @Query("SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% AND pt.superDistributorId = :hierarchyId")
    List<SuperDistributorPrepaidTransactionDetails> findTransactionsByTransactionIdAndSuperDistributorId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM SuperDistributorPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<SuperDistributorPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);
}
