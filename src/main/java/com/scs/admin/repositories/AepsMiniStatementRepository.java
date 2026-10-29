package com.scs.admin.repositories;

import com.scs.admin.entities.AepsBalanceEnquiry;
import com.scs.admin.entities.AepsMiniStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AepsMiniStatementRepository extends JpaRepository<AepsMiniStatement,Long> {

    Optional<AepsMiniStatement> findByTransactionId(String transactionId);

}
