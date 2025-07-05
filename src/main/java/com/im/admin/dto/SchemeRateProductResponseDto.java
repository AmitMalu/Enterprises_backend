package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemeRateProductResponseDto {

    private String productName;
    private String productCode;
    private String productCodeForScheme;
    private String transactionType;
    private Boolean isSettlement;
    private Double sgst;
    private Double tds;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime editedAt;
    private String editedBy;

}
