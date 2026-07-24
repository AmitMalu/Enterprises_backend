package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * author abdul.M
 */
@Data
@Builder
public class BalanceTransferReceiptResponseDto {

    private String dateTime;

    private String txnId;

    private BigDecimal amount;

    private String remarks;

    private String status;

    private String paymentMode;
}
