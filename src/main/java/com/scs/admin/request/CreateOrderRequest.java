package com.scs.admin.request;


import com.scs.admin.dto.TransactionSchemeDetailsRequestDto;
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
    private String medium;
    private String clientIp;
    private String requestDomainName;

    // Include full scheme details here
    private TransactionSchemeDetailsRequestDto transactionScheme;


}

