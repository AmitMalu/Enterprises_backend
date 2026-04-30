package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author SHUBHAM KHOPADE
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RechargeReceiptResponseDto {

    private String status;
    private String txnType;
    private String tranxId;
    private String operatorName;
    private String dateTime;
    private String customerNumber;
    private BigDecimal amount;
    private String vendorResp;

}
