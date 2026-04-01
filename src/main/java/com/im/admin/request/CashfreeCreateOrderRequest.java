package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CashfreeCreateOrderRequest {

    private String initiatedBy;
    private Short productId;
    private String subProductId;
    private String order_id;
    private Double order_amount;
    private String order_currency;
    private CustomerDetails customer_details;
    //private OrderMeta order_meta; // optional

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CustomerDetails {
        private String customer_id;
        private String customer_name;
        private String customer_email;
        private String customer_phone;
    }

//    @Data
//    @AllArgsConstructor
//    @NoArgsConstructor
//    public static class OrderMeta {
//        private String return_url;
//        private String notify_url;
//    }
}
