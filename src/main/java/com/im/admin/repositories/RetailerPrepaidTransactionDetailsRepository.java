package com.im.admin.repositories;

import com.im.admin.entities.RetailerPrepaidTransactionDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RetailerPrepaidTransactionDetailsRepository extends JpaRepository<RetailerPrepaidTransactionDetails, Integer> {

    Optional<RetailerPrepaidTransactionDetails> findByTransactionId(String transactionId);

}
