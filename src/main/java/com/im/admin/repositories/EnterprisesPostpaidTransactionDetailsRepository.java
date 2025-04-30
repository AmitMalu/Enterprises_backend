package com.im.admin.repositories;

import com.im.admin.entities.EnterprisesPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnterprisesPostpaidTransactionDetailsRepository extends JpaRepository<EnterprisesPostpaidTransactionDetails, Integer> {
}
