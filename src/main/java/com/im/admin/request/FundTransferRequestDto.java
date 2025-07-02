package com.im.admin.request;

import com.im.admin.entities.enums.WalletType;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferRequestDto {

    private String parentHierarchyId;
    private String childHierarchyId;
    private BigDecimal amount;
    private WalletType fromWalletType;
    private WalletType toWalletType;
    private String narration;
}

