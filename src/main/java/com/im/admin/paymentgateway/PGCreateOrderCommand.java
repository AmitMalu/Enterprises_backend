package com.im.admin.paymentgateway;

import com.im.admin.dto.TransactionSchemeDetailsRequestDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PGCreateOrderCommand {

    private String initiatedBy;
    private Short productId;
    private String subProductId;

    private Double amount;
    private String merchantRef;
    private String cardBinNo;
    private String cardType;
    private String paymentMethod;

    private String username;
    private String callbackUrl;
    private String cardHolderName;
    private String cardHolderMobile;
    private String emailId;

    private TransactionSchemeDetailsRequestDto schemeDetails;
}
