package com.scs.admin.repositories;


import com.scs.admin.entities.DmtTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DmtTransactionRepository extends JpaRepository<DmtTransaction, Long> {

    Optional<DmtTransaction> findByOrderId(String orderId);
}
