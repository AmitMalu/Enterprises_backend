package com.scs.admin.dto;


import com.scs.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CheckBalanceRequestDto {

    private String hierarchyId;
    private BigDecimal amount;
    private WalletType walletType;

}
