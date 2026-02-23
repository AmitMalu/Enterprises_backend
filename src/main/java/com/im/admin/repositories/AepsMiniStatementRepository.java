package com.im.admin.repositories;

import com.im.admin.entities.AepsBalanceEnquiry;
import com.im.admin.entities.AepsMiniStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsMiniStatementRepository extends JpaRepository<AepsMiniStatement,Long> {

    Optional<AepsMiniStatement> findByTransactionId(String transactionId);

}
