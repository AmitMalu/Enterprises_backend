package com.im.admin.repositories;


import com.im.admin.entities.PartnerPostpaidTransactionDetails;
import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PartnerPostpaidTransactionDetailsRepository extends JpaRepository<PartnerPostpaidTransactionDetails, Integer>, BaseTransactionRepository<PartnerPostpaidTransactionDetails, Integer> {

    // Search by normalized transactionId + distributorId
    @Query("SELECT pt FROM PartnerPostpaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId% " +
            "AND pt.partnerId IN :networkIds")
    List<PartnerPostpaidTransactionDetails> findTransactionsByTransactionIdAndPartnerIdIn(
            @Param("txnId") String txnId,
            @Param("networkIds") List<String> networkIds);



    // Search only by normalized transactionId (for Admin / global search)
    @Query("SELECT pt FROM PartnerPostpaidTransactionDetails pt " +
            "WHERE pt.transactionId LIKE %:txnId%")
    List<PartnerPostpaidTransactionDetails> findTransactionsByTransactionId(@Param("txnId") String txnId);

}
