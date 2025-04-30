package com.im.admin.repositories;

import com.im.admin.entities.DistributorPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorPrepaidTransactionDetailsRepository extends JpaRepository<DistributorPrepaidTransactionDetails, Integer> {
}
