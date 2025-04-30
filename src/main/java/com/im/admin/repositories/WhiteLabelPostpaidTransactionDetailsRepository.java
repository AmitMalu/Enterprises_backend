package com.im.admin.repositories;

import com.im.admin.entities.WhiteLabelPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WhiteLabelPostpaidTransactionDetailsRepository extends JpaRepository<WhiteLabelPostpaidTransactionDetails, Integer> {
}
