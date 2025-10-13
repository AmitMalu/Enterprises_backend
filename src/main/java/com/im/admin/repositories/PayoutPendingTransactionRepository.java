package com.im.admin.repositories;

import com.im.admin.entities.PayoutPendingTransactions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PayoutPendingTransactionRepository extends JpaRepository<PayoutPendingTransactions, Long> {

    Optional<PayoutPendingTransactions> findByTransactionIdAndStatus(String transactionId, String status);

}
