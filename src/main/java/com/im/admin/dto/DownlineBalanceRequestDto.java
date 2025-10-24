package com.im.admin.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DownlineBalanceRequestDto {

    private String hierarchyId;
    private String userId;
    private String name;
    private String parentAccount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private BigDecimal minPrepaidBalance;
    private BigDecimal maxPrepaidBalance;
    private BigDecimal minPostpaidBalance;
    private BigDecimal maxPostpaidBalance;
}
