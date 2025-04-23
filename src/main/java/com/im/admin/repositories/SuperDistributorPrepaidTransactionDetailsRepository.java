package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperDistributorPrepaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPrepaidTransactionDetails, Integer> {
}
