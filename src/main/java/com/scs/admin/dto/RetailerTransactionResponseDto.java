package com.scs.admin.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RetailerTransactionResponseDto {

    private Integer id;
    private LocalDate updatedAt;
    private String userId;
    private String mobileNo;
    private String schemeName;
    private BigDecimal amount;
    private String txStatus;
    private String txType;
    private String remarks;
    private String txId;
    private String medium;
    private String subProductName;
    private String vendor;
    private LocalDate dateOfTransaction;
    private LocalTime timeOfTransaction;
    private LocalDate updatedDateOfTransaction;
    private LocalTime updatedTimeOfTransaction;
    private String walletType;
    private String productName;
    private String operatorName;

    private String vendorTransactionId;
    private Double latitude;
    private Double longitude;

    //  Constructor exactly matching service usage
    public RetailerTransactionResponseDto(
            Integer id,
            LocalDate updatedAt,
            String userId,
            String mobileNo,
            String schemeName,
            BigDecimal amount,
            String txStatus,
            String txType,
            String remarks,
            String txId,
            String medium,
            String subProductName,
            String vendor,
            LocalDate dateOfTransaction,
            LocalTime timeOfTransaction,
            LocalDate updatedDateOfTransaction,
            LocalTime updatedTimeOfTransaction,

               String vendorTransactionId,
            Double latitude,
            Double longitude,
            String operatorName
    ) {
        this.id = id;
        this.updatedAt = updatedAt;
        this.userId = userId;
        this.mobileNo = mobileNo;
        this.schemeName = schemeName;
        this.amount = amount;
        this.txStatus = txStatus;
        this.txType = txType;
        this.remarks = remarks;
        this.txId = txId;
        this.medium = medium;
        this.subProductName = subProductName;
        this.vendor = vendor;
        this.dateOfTransaction = dateOfTransaction;
        this.timeOfTransaction = timeOfTransaction;
        this.updatedDateOfTransaction = updatedDateOfTransaction;
        this.updatedTimeOfTransaction = updatedTimeOfTransaction;

        this.vendorTransactionId = vendorTransactionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.operatorName = operatorName;
    }
}