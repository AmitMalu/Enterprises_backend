package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillRechargeCheckStatusResponseDto {

    private Integer status;          // e.g. 43, 44, 55
    private String message;          // e.g. "SUCCESS", "FAILED", "PENDING"
    private String description;      // e.g. "Transaction is success"
    private String bankRefNo;        // e.g. "456"
    private String clientId;         // e.g. "TEST_3"
    private Long txnId;
}
