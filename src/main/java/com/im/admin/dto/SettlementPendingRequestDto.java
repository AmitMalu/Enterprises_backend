package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
}
