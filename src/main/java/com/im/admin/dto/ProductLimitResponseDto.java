package com.im.admin.dto;

import com.im.admin.entities.enums.WalletType;
import jakarta.persistence.Column;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductLimitResponseDto {


    private Long id;
    private String description;
    private WalletType walletType;

    private String paymentCurrency;
    private Boolean isAllowPerDayCardLimit;
    private Boolean isAllowMonthlyCardLimit;
    private Boolean checkAmountPattern;
    private BigDecimal maxAmount;
    private String perDayTransactionLimit;
    private String monthlyTransactionLimit;

    // Flattened fields
    private Short productId;
    private String productName;
    private String subProductId;
    private String subProductName;
    private Boolean blocked;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime editedOn;
    private String editedBy;
}
