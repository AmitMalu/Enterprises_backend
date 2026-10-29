package com.scs.admin.repositories;

import com.scs.admin.entities.PayoutPendingTransactions;
import com.scs.admin.entities.PgTransactionMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PgTransactionMappingRepository extends JpaRepository<PgTransactionMapping, Long> {

    Optional<PgTransactionMapping> findByOrderId(String orderId);

    Optional<PgTransactionMapping> findByTxnId(String txnId);

    Optional<PgTransactionMapping> findByVendorSessionId(String vendorSessionId);

    List<PgTransactionMapping> findByStatus(String status);

    @Query("SELECT p FROM PgTransactionMapping p WHERE p.orderId LIKE %:orderIdPart%")
    Optional<PgTransactionMapping> findByOrderIdLike(@Param("orderIdPart") String orderIdPart);


}

