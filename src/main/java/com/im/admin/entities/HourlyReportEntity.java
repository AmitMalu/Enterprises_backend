package com.im.admin.entities;


import com.im.admin.entities.enums.ReportType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "hourly_reports")
public class HourlyReportEntity {

    @Id
    private String id; // e.g. "2025-09-06_01_transaction"

    private LocalDate dated;
    private LocalDateTime timestamp;
    private String subject;
    private String fileName;   // stored filename, e.g. "hourly_transaction_20250906_01.xlsx"
    private String filePath;   // relative path under reports.base-dir, e.g. "2025-09-06/hourly_transaction_20250906_01.xlsx"

    @Enumerated(EnumType.STRING)
    private ReportType reportType;

    // timestamps
    private LocalDateTime createdAt;

    // Constructors, getters, setters ...
    @PrePersist
    public void prePersist() { createdAt = LocalDateTime.now(); }

}

