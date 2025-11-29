package com.im.admin.repositories;

import com.im.admin.entities.AepsCashWithdrawalLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AepsCashWithdrawalRepository extends JpaRepository<AepsCashWithdrawalLog, Long> {
}
