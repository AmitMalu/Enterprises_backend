package com.scs.admin.repositories;

import com.scs.admin.entities.SmsLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SmsLogsRepository extends JpaRepository<SmsLog,Long> {


    @Query(value = "CALL sms_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getSmsLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
}
