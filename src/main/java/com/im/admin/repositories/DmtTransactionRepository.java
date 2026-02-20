package com.im.admin.repositories;


import com.im.admin.entities.DmtTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DmtTransactionRepository extends JpaRepository<DmtTransaction, Long> {

    Optional<DmtTransaction> findByOrderId(String orderId);
}
