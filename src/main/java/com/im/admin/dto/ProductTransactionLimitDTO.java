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
public class ProductTransactionLimitDTO {

    private Long id;
    private String description;
    private WalletType walletType;
    private Short productId;
    private String subProductId;
    private String paymentCurrency;
    private Boolean isAllowPerDayCardLimit;
    private Integer perDayTransactionLimit;
    private Boolean isAllowMonthlyCardLimit;
    private Integer monthlyTransactionLimit;
    private Boolean checkAmountPattern;
    private BigDecimal maxAmount;
}
