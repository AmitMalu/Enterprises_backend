package com.im.admin.repositories;

import com.im.admin.entities.PartnerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PartnerPrepaidTransactionDetailsRepository extends JpaRepository<PartnerPrepaidTransactionDetails, Integer> {
//    List<PartnerPrepaidTransactionDetails> findByTransactionIdAndHierarchyId(String txnId, String hierarchyId);

}
