package com.im.admin.repositories;

import com.im.admin.entities.EnterprisesPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterprisesPrepaidTransactionDetailsRepository extends JpaRepository<EnterprisesPrepaidTransactionDetails, Integer> {
}
