package com.scs.admin.response.reports;

import com.scs.admin.entities.enums.WalletReportFrequency;
import com.scs.admin.entities.enums.WalletReportType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDownloadReportResponse {

    private Long id;
    private LocalDate date;
    private LocalDateTime timestamp;
    private String subject;
    private WalletReportFrequency frequency;
    private WalletReportType reportType;
    private String fileName;
    private Long recordCount;
    private LocalDateTime createdAt;
}
