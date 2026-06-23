package com.im.admin.response.reports;

import com.im.admin.entities.enums.WalletReportFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO returned to the Download Report screen.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletDownloadReportResponse {

    private Long id;
    private LocalDate date;
    private LocalDateTime timestamp;
    private String subject;
    private WalletReportFrequency frequency;
    private String fileName;
    private Long recordCount;
    private LocalDateTime createdAt;
}
