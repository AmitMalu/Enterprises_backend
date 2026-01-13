package com.im.admin.repositories;

import com.im.admin.entities.AepsBalanceEnquiry;
import com.im.admin.entities.AepsMiniStatement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AepsMiniStatementRepository extends JpaRepository<AepsMiniStatement,Long> {
}
