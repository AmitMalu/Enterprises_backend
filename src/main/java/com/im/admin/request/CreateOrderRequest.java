package com.im.admin.request;


import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderRequest {

    private String orderId;
    private double amount;
    private String callbackUrl;
    private String merchantRef; // optional

    // Include full scheme details here
    private TransactionSchemeDetailsRequestDto transactionScheme;


}

