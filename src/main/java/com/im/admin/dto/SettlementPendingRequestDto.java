package com.im.admin.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SettlementPendingRequestDto {
    private String walletType;       // PREPAID / POSTPAID
    private LocalDate fromDate;      // filter start date
    private LocalDate toDate;        // filter end date
    private String subProductId;
    private String vendorId;
    private String tranStatus;

    private Integer id;
    private String userId;
    private String mobileNo;
    private String schemeName;
    private Double amount;
    private String txId;
    private String remarks;
    private String medium;
    private String txType;
    private String txStatus;
    private LocalDate dateOfTransaction;
    private LocalTime timeOfTransaction;
    private LocalDate updatedDateOfTransaction;
    private LocalTime updatedTimeOfTransaction;
}
