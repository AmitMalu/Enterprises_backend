package com.im.admin.service;

import com.im.admin.dto.ProductLimitResponseDto;
import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.entities.enums.WalletType;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface ProductTransactionLimitService {

    CommonResponse<ProductLimitResponseDto> save(ProductTransactionLimitDTO dto);

    CommonResponse<ProductLimitResponseDto> getById(Long id);



    CommonResponse<ProductLimitResponseDto> update(Long id, ProductTransactionLimitDTO dto);

    CommonResponse<Page<ProductLimitResponseDto>> getAll(Long id, String description, WalletType walletType, String paymentCurrency, Boolean isAllowPerDayCardLimit, Boolean isAllowMonthlyCardLimit, Boolean checkAmountPattern, BigDecimal maxAmount, String perDayTransactionLimit, String monthlyTransactionLimit, Short productId, String productName, String subProductId, String subProductName, Boolean blocked, String createdBy, String editedBy, Pageable pageable);

}
