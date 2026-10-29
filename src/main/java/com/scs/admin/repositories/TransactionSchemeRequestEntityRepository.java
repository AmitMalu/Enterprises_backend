package com.scs.admin.repositories;


import com.scs.admin.entities.TransactionSchemeRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionSchemeRequestEntityRepository extends JpaRepository<TransactionSchemeRequestEntity, String> {

    Optional<TransactionSchemeRequestEntity> findByOrderId(String orderId);

}

