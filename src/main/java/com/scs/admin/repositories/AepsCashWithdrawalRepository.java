package com.scs.admin.repositories;

import com.scs.admin.entities.AepsCashWithdrawalLog;
import com.scs.admin.entities.AepsMiniStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AepsCashWithdrawalRepository extends JpaRepository<AepsCashWithdrawalLog, Long> {
    Optional<AepsCashWithdrawalLog> findByTransactionId(String transactionId);

}
