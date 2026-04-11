package com.im.admin.dto;

import com.im.admin.entities.enums.WalletType;
import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    private String perDayTransactionLimit;
    private Boolean isAllowMonthlyCardLimit;
    private String monthlyTransactionLimit;
    private Boolean checkAmountPattern;
    private BigDecimal maxAmount;
    private BigDecimal minAmount;
    private Boolean blocked;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;

    private String editedBy;
}
