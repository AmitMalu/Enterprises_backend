package com.im.admin.repositories;

import com.im.admin.entities.VendorLogs;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VendorLogsRepository extends JpaRepository<VendorLogs, Integer> {

    @Query(value = "CALL vendor_logs_report(:startDate, :endDate)", nativeQuery = true)
    List<Object[]> getVendorLogsBetweenDates(@Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);
}
