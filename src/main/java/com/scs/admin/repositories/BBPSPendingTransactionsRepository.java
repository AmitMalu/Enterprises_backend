package com.scs.admin.repositories;

import com.scs.admin.entities.BBPSPendingTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BBPSPendingTransactionsRepository extends JpaRepository<BBPSPendingTransactions, Long> {

    List<BBPSPendingTransactions> findByStatus(String status);

    BBPSPendingTransactions findByTransactionId(String transactionId);

}
