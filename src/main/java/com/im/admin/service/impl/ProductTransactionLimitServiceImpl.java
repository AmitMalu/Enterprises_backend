package com.im.admin.service.impl;

import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductTransactionLimitRepository;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductTransactionLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductTransactionLimitServiceImpl implements ProductTransactionLimitService {

    @Autowired
    private ProductTransactionLimitRepository repository;

    @Autowired
    private ProductsRepository productsRepository;
    @Autowired
    private SubProductsRepository subProductsRepository;

    @Override
    public CommonResponse<ProductTransactionLimit> save(ProductTransactionLimitDTO dto) {
        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<SubProducts> optSubProducts = subProductsRepository.findById(dto.getSubProductId());
        SubProducts subProduct = optSubProducts.get();

        ProductTransactionLimit limit = new ProductTransactionLimit();
        limit.setProduct(product);
        limit.setSubProduct(subProduct);
        limit.setDescription(dto.getDescription());
        limit.setWalletType(dto.getWalletType());
        limit.setPaymentCurrency(dto.getPaymentCurrency());
        limit.setIsAllowPerDayCardLimit(dto.getIsAllowPerDayCardLimit());
        limit.setPerDayTransactionLimit(dto.getPerDayTransactionLimit());
        limit.setIsAllowMonthlyCardLimit(dto.getIsAllowMonthlyCardLimit());
        limit.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
        limit.setCheckAmountPattern(dto.getCheckAmountPattern());
        limit.setMaxAmount(dto.getMaxAmount());
        limit.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());

        ProductTransactionLimit saved = repository.save(limit);
        return CommonResponse.success(saved,"Limit saved successfully");
    }

    @Override
    public CommonResponse<ProductTransactionLimit> update(Long id, ProductTransactionLimitDTO dto) {
        ProductTransactionLimit existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductTransactionLimit not found with id: " + id));

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + dto.getProductId()));

        SubProducts subProduct = subProductsRepository.findById(dto.getProductId().toString())
                .orElseThrow(() -> new RuntimeException("SubProduct not found with id: " + dto.getSubProductId()));

        // Update fields
        existing.setProduct(product);
        existing.setSubProduct(subProduct);
        existing.setDescription(dto.getDescription());
        existing.setWalletType(dto.getWalletType());
        existing.setPaymentCurrency(dto.getPaymentCurrency());
        existing.setIsAllowPerDayCardLimit(dto.getIsAllowPerDayCardLimit());
        existing.setPerDayTransactionLimit(dto.getPerDayTransactionLimit());
        existing.setIsAllowMonthlyCardLimit(dto.getIsAllowMonthlyCardLimit());
        existing.setMonthlyTransactionLimit(dto.getMonthlyTransactionLimit());
        existing.setCheckAmountPattern(dto.getCheckAmountPattern());
        existing.setMaxAmount(dto.getMaxAmount());

        ProductTransactionLimit updated = repository.save(existing);
        return CommonResponse.success(updated, "Limit updated successfully");
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

