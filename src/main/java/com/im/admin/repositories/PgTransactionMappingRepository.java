package com.im.admin.repositories;

import com.im.admin.entities.PgTransactionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PgTransactionMappingRepository extends JpaRepository<PgTransactionMapping, Long> {

    Optional<PgTransactionMapping> findByOrderId(String orderId);

    Optional<PgTransactionMapping> findByTxnId(String txnId);


}

