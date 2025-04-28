package com.im.admin.dto;

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
public class WalletToWalletTransferDto {

    private String hierarchyId;
    private BigDecimal transferAmount;
    private WalletType walletTypeFrom;
    private WalletType walletTypeTo;
}
