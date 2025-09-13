package com.im.admin.repositories;


import com.im.admin.entities.HourlyReportEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HourlyReportRepository extends JpaRepository<HourlyReportEntity, String> {

    Page<HourlyReportEntity> findByDatedOrderByTimestampAsc(LocalDate date, Pageable pageable);

    List<HourlyReportEntity> findByCreatedAtBefore(LocalDateTime cutoff);

}

