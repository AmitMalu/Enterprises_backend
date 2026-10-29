package com.scs.admin.repositories;

import com.scs.admin.entities.ScsLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScsLogsRepository extends JpaRepository<ScsLogs,Integer> {

    @Query(value = "CALL scs_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

}
