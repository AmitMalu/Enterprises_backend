package com.scs.admin.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BbpsReceiptResponseDto {
    private String createdDateTime;
    private String txnStatus;
    private String billerName;
    private Long txnAmount;
    private String customerName;
    private String consumerNumber;
    private String category;
    private String txnId;
    private String referenceNumber;
}
