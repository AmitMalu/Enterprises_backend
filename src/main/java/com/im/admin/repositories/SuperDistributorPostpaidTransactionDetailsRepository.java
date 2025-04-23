package com.im.admin.repositories;

import com.im.admin.entities.SuperDistributorPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperDistributorPostpaidTransactionDetailsRepository extends JpaRepository<SuperDistributorPostpaidTransactionDetails, Integer> {
}
