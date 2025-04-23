package com.im.admin.repositories;

import com.im.admin.entities.AdminPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminPrepaidTransactionDetailsRepository extends JpaRepository<AdminPrepaidTransactionDetails, Integer> {


}
