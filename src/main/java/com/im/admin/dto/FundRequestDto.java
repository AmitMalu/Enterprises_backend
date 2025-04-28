package com.im.admin.dto;

import com.im.admin.entities.enums.WalletType;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FundRequestDto {

    private String requesterHierarchyId; // Who is raising the request
    private BigDecimal requestedAmount;
    private WalletType walletType; // PREPAID or POSTPAID

}

