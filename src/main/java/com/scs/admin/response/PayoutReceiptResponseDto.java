package com.scs.admin.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayoutReceiptResponseDto {

    private String txnId;
    private String txnStatus;
    private Long txnAmount;
    private String dateTime;

    private String accountNumber;
    private String ifscCode;
    private String bankName;
    private String accountName;

    private String txnMode;
    private String utrNumber;

    private String guid;
}
