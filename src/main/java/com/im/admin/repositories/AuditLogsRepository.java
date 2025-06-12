package com.im.admin.repositories;

import com.im.admin.entities.AuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogsRepository extends JpaRepository<AuditLogs,Integer> {
}
