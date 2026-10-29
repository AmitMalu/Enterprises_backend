package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.repository.NoRepositoryBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@NoRepositoryBean
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletReportDto {

    private String transactionId;
    private String hierarchyId;
    private String walletType; // Prepaid or Postpaid
    private BigDecimal balBefore;
    private BigDecimal amount;
    private BigDecimal balAfter;
    private String subProductName;
    private String narration;
    private String status;
    private LocalDate date;
    private LocalTime time;

}

