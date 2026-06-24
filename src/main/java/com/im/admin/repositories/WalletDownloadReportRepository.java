package com.im.admin.repositories;

import com.im.admin.entities.WalletDownloadReport;
import com.im.admin.entities.enums.WalletReportFrequency;
import com.im.admin.entities.enums.WalletReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WalletDownloadReportRepository extends JpaRepository<WalletDownloadReport, Long> {

    /** All reports, latest first */
    Page<WalletDownloadReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** Filter by reportType only */
    Page<WalletDownloadReport> findByReportTypeOrderByCreatedAtDesc(WalletReportType reportType, Pageable pageable);

    /** Filter by date */
    Page<WalletDownloadReport> findByReportDateOrderByCreatedAtDesc(LocalDate reportDate, Pageable pageable);

    /** Filter by reportType + date */
    Page<WalletDownloadReport> findByReportTypeAndReportDateOrderByCreatedAtDesc(
            WalletReportType reportType, LocalDate reportDate, Pageable pageable);

    /** Duplicate check */
    Optional<WalletDownloadReport> findByReportTypeAndReportDateAndFrequencyAndSubject(
            WalletReportType reportType,
            LocalDate reportDate,
            WalletReportFrequency frequency,
            String subject
    );
}
