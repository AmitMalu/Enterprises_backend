package com.im.admin.entities;

import com.im.admin.entities.enums.WalletReportFrequency;
import com.im.admin.entities.enums.WalletReportType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stores metadata for generated Wallet History Download Reports.
 * Table: wallet_download_reports
 */
@Getter
@Setter
@Entity
@Table(name = "wallet_download_reports")
public class WalletDownloadReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** yyyy-MM-dd of the day this report covers */
    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    /** e.g. 2026-06-23T01:00:00 for hourly, 2026-06-23T23:59:59 for daily */
    @Column(name = "report_timestamp", nullable = false)
    private LocalDateTime reportTimestamp;

    /**
     * Human-readable subject:
     * "Hourly Wallet History Report 2026-06-23 01:00:00"
     * "Daily Wallet History Report 2026-06-23"
     */
    @Column(name = "subject", nullable = false)
    private String subject;

    /** HOURLY or DAILY */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false)
    private WalletReportFrequency frequency;

    /** WALLET_HISTORY or WALLET_PREVIEW */
    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    private WalletReportType reportType;

    /** Stored filename, e.g. "wallet_history_20260623_01.xlsx" */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Relative path under reports.base-dir, e.g. "wallet-history/2026-06-23/wallet_history_20260623_01.xlsx" */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /** Total records in this report */
    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
