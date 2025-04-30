package com.im.admin.repositories;

import com.im.admin.entities.RetailerPostpaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetailerPostpaidTransactionDetailsRepository extends JpaRepository<RetailerPostpaidTransactionDetails, Integer> {

    Optional<RetailerPostpaidTransactionDetails> findByTransactionId(String transactionId);

}
