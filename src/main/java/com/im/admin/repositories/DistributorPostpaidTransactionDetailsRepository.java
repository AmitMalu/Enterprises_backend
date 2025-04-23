package com.im.admin.repositories;

import com.im.admin.entities.DistributorPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DistributorPostpaidTransactionDetailsRepository extends JpaRepository<DistributorPostpaidTransactionDetails, Integer> {


}
