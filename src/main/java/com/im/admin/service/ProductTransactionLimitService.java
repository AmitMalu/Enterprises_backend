package com.im.admin.service;

import com.im.admin.dto.ProductLimitResponseDto;
import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductTransactionLimitService {

    CommonResponse<ProductLimitResponseDto> save(ProductTransactionLimitDTO dto);

    CommonResponse<ProductLimitResponseDto> getById(Long id);

    CommonResponse<Page<ProductLimitResponseDto>>  getAll(int page, int size);

    CommonResponse<ProductLimitResponseDto> update(Long id, ProductTransactionLimitDTO dto);

}
