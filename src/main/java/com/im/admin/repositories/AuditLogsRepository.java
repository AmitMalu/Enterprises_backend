package com.im.admin.repositories;

import com.im.admin.entities.AuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogsRepository extends JpaRepository<AuditLogs,Integer> {
}
