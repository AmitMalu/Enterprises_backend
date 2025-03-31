package com.im.admin.repositories;

import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RetailerPrepaidTransactionDetailsRepository extends JpaRepository<RetailerPrepaidTransactionDetails, Long> {



}
