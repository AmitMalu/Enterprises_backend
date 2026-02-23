package com.im.admin.repositories;

import com.im.admin.entities.AepsCashWithdrawalLog;
import com.im.admin.entities.AepsMiniStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AepsCashWithdrawalRepository extends JpaRepository<AepsCashWithdrawalLog, Long> {
    Optional<AepsCashWithdrawalLog> findByTransactionId(String transactionId);

}
