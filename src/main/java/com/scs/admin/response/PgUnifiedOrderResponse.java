package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PgUnifiedOrderResponse {

    private String vendor;
    private String orderId;
    private String paymentLink;
    private String orderToken;
    private String status;
}