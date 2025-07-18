package com.im.admin.repositories;

import com.im.admin.entities.HierarchyLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface HierarchyLogsRepository extends JpaRepository<HierarchyLogs, Integer> {

    @Query(value = "CALL hierarchy_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getHierarchyLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);
}
