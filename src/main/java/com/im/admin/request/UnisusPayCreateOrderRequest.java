package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UnisusPayCreateOrderRequest {

    private String initiatedBy;
    private Short productId;
    private String subProductId;
    private Double amount;
    private String cardBinNo;


}
