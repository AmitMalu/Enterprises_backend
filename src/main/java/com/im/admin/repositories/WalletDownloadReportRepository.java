package com.im.admin.repositories;

import com.im.admin.entities.WalletDownloadReport;
import com.im.admin.entities.enums.WalletReportFrequency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WalletDownloadReportRepository extends JpaRepository<WalletDownloadReport, Long> {

    /** Paginated list, latest first */
    Page<WalletDownloadReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Filter by date */
    Page<WalletDownloadReport> findByReportDateOrderByCreatedAtDesc(LocalDate reportDate, Pageable pageable);

    /** Avoid duplicate generation for the same hour/day */
    Optional<WalletDownloadReport> findByReportDateAndFrequencyAndSubject(
            LocalDate reportDate,
            WalletReportFrequency frequency,
            String subject
    );
}
