package com.scs.admin.repositories;

import com.scs.admin.entities.BillAvenueTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillAvenueTransactionRepository extends JpaRepository<BillAvenueTransaction, Long> {
    Optional<BillAvenueTransaction> findByTxnId(String txnId);

}
