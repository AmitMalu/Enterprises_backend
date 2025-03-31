package com.im.admin.repositories;

import com.im.admin.entities.AdminPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPostpaidTransactionDetailsRepository extends JpaRepository<AdminPostpaidTransactionDetails, Long> {
}
