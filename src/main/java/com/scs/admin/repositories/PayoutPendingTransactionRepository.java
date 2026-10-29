package com.scs.admin.repositories;

import com.scs.admin.entities.PayoutPendingTransactions;
import jakarta.persistence.Column;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayoutPendingTransactionRepository extends JpaRepository<PayoutPendingTransactions, Long> {

    Optional<PayoutPendingTransactions> findByTransactionId(String transactionId);

    List<PayoutPendingTransactions> findByStatus(String status);

    Optional<PayoutPendingTransactions> findByTempTxnId(String tempTxnId);

}
