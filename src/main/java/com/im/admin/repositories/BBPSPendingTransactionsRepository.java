package com.im.admin.repositories;

import com.im.admin.entities.BBPSPendingTransactions;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BBPSPendingTransactionsRepository extends JpaRepository<BBPSPendingTransactions, Long> {

    List<BBPSPendingTransactions> findByStatus(String status);

    BBPSPendingTransactions findByTransactionId(String transactionId);

}
