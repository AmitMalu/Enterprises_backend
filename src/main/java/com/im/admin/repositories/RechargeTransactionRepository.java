package com.im.admin.repositories;

import com.im.admin.entities.RechargeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RechargeTransactionRepository  extends JpaRepository<RechargeTransaction, Long> {

    List<RechargeTransaction> findByStatus(String status);

    Optional<RechargeTransaction> findByTransactionRef(String transactionRef);

}
