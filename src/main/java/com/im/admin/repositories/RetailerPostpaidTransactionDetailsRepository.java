package com.im.admin.repositories;

import com.im.admin.entities.RetailerPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerPostpaidTransactionDetailsRepository extends JpaRepository<RetailerPostpaidTransactionDetails, Long> {
}
