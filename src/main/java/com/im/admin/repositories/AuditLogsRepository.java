package com.im.admin.repositories;

import com.im.admin.entities.AuditLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogsRepository extends JpaRepository<AuditLogs, Integer> {

    @Query(value = "CALL audit_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getAuditLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
