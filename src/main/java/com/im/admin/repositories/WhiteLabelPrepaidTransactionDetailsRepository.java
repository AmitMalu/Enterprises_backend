package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhiteLabelPrepaidTransactionDetailsRepository extends JpaRepository<WhiteLabelPrepaidTransactionDetails, Integer> {
}
