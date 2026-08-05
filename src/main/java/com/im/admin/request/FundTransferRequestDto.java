package com.im.admin.request;

import com.im.admin.entities.enums.WalletType;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransferRequestDto {

    private String fromHierarchyId;
    private String toHierarchyId;
    private BigDecimal amount;
    private BigDecimal confirmAmount;
    private WalletType fromWalletType;
    private WalletType toWalletType;
    private String narration;
    private String subProductId;
    private String schemeCode;
}

