package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FundReversalResponseDto {

    private Long fundReversalRequestId;
    private String otp;
    private String transactionId;

}

