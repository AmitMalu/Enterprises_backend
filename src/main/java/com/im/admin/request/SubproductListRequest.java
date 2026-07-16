package com.im.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubproductListRequest {

    /*
     * Allowed values:
     * PG
     * PAYOUT
     */
    private String serviceType;
}
