package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RechargeResponse {
    private String transactionRef;
    private String status;
    private String message;
}
