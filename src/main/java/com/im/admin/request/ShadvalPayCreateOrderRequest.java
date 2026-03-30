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
public class ShadvalPayCreateOrderRequest {

    private String initiatedBy;
    private Short productId;
    private String subProductId;
    private Double amount;
    private String customerName;
    private String mobileNo;
    private String emailId;
    private String remark;


}
