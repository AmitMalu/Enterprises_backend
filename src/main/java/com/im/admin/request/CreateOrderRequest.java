package com.im.admin.request;


import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    private String initiatedBy;
    private String username;
    private String orderId;
    private double amount;
    private String callbackUrl;
    private String merchantRef; // optional
    private Short productId;
    private String subProductId;
    private String cardBinNo;

    // Include full scheme details here
    private TransactionSchemeDetailsRequestDto transactionScheme;


}

