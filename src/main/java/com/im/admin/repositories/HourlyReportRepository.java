package com.im.admin.repositories;


import com.im.admin.entities.HourlyReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HourlyReportRepository extends JpaRepository<HourlyReportEntity, String> {

    List<HourlyReportEntity> findByDatedOrderByTimestampAsc(LocalDate dated);

    List<HourlyReportEntity> findByCreatedAtBefore(LocalDateTime cutoff);

}

