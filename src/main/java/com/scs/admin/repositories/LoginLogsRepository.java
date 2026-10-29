package com.scs.admin.repositories;

import com.scs.admin.entities.LoginLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface LoginLogsRepository extends JpaRepository<LoginLogs, Integer> {

    @Query(value = "CALL login_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getLoginLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);
}
