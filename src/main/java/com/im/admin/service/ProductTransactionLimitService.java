package com.im.admin.service;

import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductTransactionLimitService {

    CommonResponse<ProductTransactionLimit> save(ProductTransactionLimitDTO dto);

    CommonResponse<ProductTransactionLimit> getById(Long id);

    CommonResponse<Page<ProductTransactionLimit>> getAll(int page, int size);

    CommonResponse<ProductTransactionLimit> update(Long id, ProductTransactionLimitDTO dto);

}
