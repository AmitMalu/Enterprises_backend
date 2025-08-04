package com.im.admin.service.impl;

import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductTransactionLimitRepository;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductTransactionLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductTransactionLimitServiceImpl implements ProductTransactionLimitService {

    @Autowired
    private ProductTransactionLimitRepository repository;

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public CommonResponse<ProductTransactionLimit> save(ProductTransactionLimitDTO dto) {
        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        ProductTransactionLimit limit = new ProductTransactionLimit();
        limit.setProduct(product);
        limit.setDailyTransactionLimit(dto.getDailyTransactionLimit());
        limit.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
        limit.setYearlyTransactionLimit(dto.getYearlyTransactionLimit());
        limit.setMaxPerTransactionAmt(dto.getMaxPerTransactionAmt());

        ProductTransactionLimit saved = repository.save(limit);
        return CommonResponse.success(saved,"Limit saved successfully");
    }

    @Override
    public CommonResponse<ProductTransactionLimit> update(Long id, ProductTransactionLimitDTO dto) {
        ProductTransactionLimit existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductTransactionLimit not found with id: " + id));

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + dto.getProductId()));

        existing.setProduct(product);
        existing.setDailyTransactionLimit(dto.getDailyTransactionLimit());
        existing.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
        existing.setYearlyTransactionLimit(dto.getYearlyTransactionLimit());
        existing.setMaxPerTransactionAmt(dto.getMaxPerTransactionAmt());

        ProductTransactionLimit updated = repository.save(existing);
        return CommonResponse.success(updated,"Limit updated successfully");
    }

    @Override
    public CommonResponse<ProductTransactionLimit> getById(Long id) {
        ProductTransactionLimit limit = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Limit not found"));
        return CommonResponse.success(limit,"Limit fetched successfully");
    }

    @Override
    public CommonResponse<Page<ProductTransactionLimit>> getAll(int page, int size) {
        Page<ProductTransactionLimit> pagedResult = repository.findAll(PageRequest.of(page, size));
        return CommonResponse.success(pagedResult,"Limits fetched successfully");
    }
}

