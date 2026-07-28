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

    /*
    * for Instants = flag 0  ----> PG
    * for Tatkal = flag 1   ----> PG
    * for Payout = flag 2   ----->  Payout
     */
    private Integer isSettlement;
}
