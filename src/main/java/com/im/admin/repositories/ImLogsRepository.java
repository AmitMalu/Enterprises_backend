package com.im.admin.repositories;

import com.im.admin.entities.ImLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ImLogsRepository extends JpaRepository<ImLogs,Integer> {

    @Query(value = "CALL im_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                       @Param("endDate") LocalDateTime endDate);

}
