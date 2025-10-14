package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchReceiptResponseDto {

    private String retailerId;
    private LocalDateTime createdDateTime;
    private String txnId;
    private String txnStatus;
    private BigDecimal txnAmount;
    private String consumerNumber;
    private String refNo;

}
