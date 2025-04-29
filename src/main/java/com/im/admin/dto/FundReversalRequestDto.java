package com.im.admin.dto;


import com.im.admin.entities.enums.ConstantStatus;
import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FundReversalRequestDto {

    private String parentHierarchyId;
    private String childHierarchyId;
    private BigDecimal reversalAmount;
    private WalletType walletType;
    private Long fundReversalRequestId;
    private ConstantStatus status; // APPROVED or REJECTED
    private String remarks;
    private String otp;
}

