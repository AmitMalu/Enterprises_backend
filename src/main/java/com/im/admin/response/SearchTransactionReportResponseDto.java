package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchTransactionReportResponseDto {

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


    private String vendorName;
    private String vendorTransactionId;
    private Double latitude;
    private Double longitude;

}
