package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PgStatusResponse {

    private String orderId;
    private String status;     // SUCCESS / FAILED / PENDING
    private String vendorTxnId;
    private Double amount;
    private String message;
}
