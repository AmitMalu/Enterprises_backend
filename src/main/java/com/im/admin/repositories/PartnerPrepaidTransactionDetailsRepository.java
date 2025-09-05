package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerPrepaidTransactionDetailsRepository extends JpaRepository<PartnerPrepaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT pt FROM PartnerPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% AND pt.partnerId = :hierarchyId")
    List<PartnerPrepaidTransactionDetails> findTransactionsByTransactionIdAndPartnerId(
            @Param("txnId") String txnId,
            @Param("hierarchyId") String hierarchyId);


    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM PartnerPrepaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<PartnerPrepaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
