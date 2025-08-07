package com.im.admin.service.impl;

import com.im.admin.dto.ProductLimitResponseDto;
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

import java.time.LocalDateTime;
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
    public CommonResponse<ProductLimitResponseDto> save(ProductTransactionLimitDTO dto) {
        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<SubProducts> optSubProducts = subProductsRepository.findById(dto.getSubProductId());
        SubProducts subProduct = optSubProducts
                .orElseThrow(() -> new RuntimeException("SubProduct not found"));

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
        limit.setBlocked(dto.getBlocked());
        limit.setCreatedOn(LocalDateTime.now());
        limit.setCreatedBy(dto.getCreatedBy());

        ProductTransactionLimit saved = repository.save(limit);

        // Map to Response DTO
        ProductLimitResponseDto responseDto = new ProductLimitResponseDto();
        responseDto.setId(saved.getId());
        responseDto.setDescription(saved.getDescription());
        responseDto.setWalletType(saved.getWalletType());
        responseDto.setPaymentCurrency(saved.getPaymentCurrency());
        responseDto.setIsAllowPerDayCardLimit(saved.getIsAllowPerDayCardLimit());
        responseDto.setPerDayTransactionLimit(saved.getPerDayTransactionLimit());
        responseDto.setIsAllowMonthlyCardLimit(saved.getIsAllowMonthlyCardLimit());
        responseDto.setMonthlyTransactionLimit(saved.getMonthlyTransactionLimit());
        responseDto.setCheckAmountPattern(saved.getCheckAmountPattern());
        responseDto.setMaxAmount(saved.getMaxAmount());
        responseDto.setBlocked(saved.getBlocked());
        responseDto.setCreatedOn(saved.getCreatedOn());
        responseDto.setCreatedBy(saved.getCreatedBy());

        if (saved.getProduct() != null) {
            responseDto.setProductId(saved.getProduct().getId());
            responseDto.setProductName(saved.getProduct().getName());
        }

        if (saved.getSubProduct() != null) {
            responseDto.setSubProductId(saved.getSubProduct().getId());
            responseDto.setSubProductName(saved.getSubProduct().getSubProductTitle());
        }

        return CommonResponse.success(responseDto, "Limit saved successfully");
    }


    @Override
    public CommonResponse<ProductLimitResponseDto> update(Long id, ProductTransactionLimitDTO dto) {
        ProductTransactionLimit existing = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProductTransactionLimit not found with id: " + id));

        Products product = productsRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + dto.getProductId()));

        SubProducts subProduct = subProductsRepository.findById(dto.getSubProductId())
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
        existing.setBlocked(dto.getBlocked());
        existing.setEditedOn(LocalDateTime.now());
        existing.setEditedBy(dto.getEditedBy());

        ProductTransactionLimit updated = repository.save(existing);

        // Map to Response DTO
        ProductLimitResponseDto responseDto = new ProductLimitResponseDto();
        responseDto.setId(updated.getId());
        responseDto.setDescription(updated.getDescription());
        responseDto.setWalletType(updated.getWalletType());
        responseDto.setPaymentCurrency(updated.getPaymentCurrency());
        responseDto.setIsAllowPerDayCardLimit(updated.getIsAllowPerDayCardLimit());
        responseDto.setPerDayTransactionLimit(updated.getPerDayTransactionLimit());
        responseDto.setIsAllowMonthlyCardLimit(updated.getIsAllowMonthlyCardLimit());
        responseDto.setMonthlyTransactionLimit(updated.getMonthlyTransactionLimit());
        responseDto.setCheckAmountPattern(updated.getCheckAmountPattern());
        responseDto.setMaxAmount(updated.getMaxAmount());
        responseDto.setBlocked(updated.getBlocked());
        responseDto.setEditedOn(updated.getEditedOn());
        responseDto.setEditedBy(updated.getEditedBy());

        if (updated.getProduct() != null) {
            responseDto.setProductId(updated.getProduct().getId());
            responseDto.setProductName(updated.getProduct().getName());
        }

        if (updated.getSubProduct() != null) {
            responseDto.setSubProductId(updated.getSubProduct().getId());
            responseDto.setSubProductName(updated.getSubProduct().getSubProductTitle());
        }

        return CommonResponse.success(responseDto, "Limit updated successfully");
    }


    @Override
    public CommonResponse<ProductLimitResponseDto> getById(Long id) {
        ProductTransactionLimit limit = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Limit not found"));

        ProductLimitResponseDto dto = new ProductLimitResponseDto();
        dto.setId(limit.getId());
        dto.setDescription(limit.getDescription());
        dto.setWalletType(limit.getWalletType());
        dto.setPaymentCurrency(limit.getPaymentCurrency());
        dto.setIsAllowPerDayCardLimit(limit.getIsAllowPerDayCardLimit());
        dto.setIsAllowMonthlyCardLimit(limit.getIsAllowMonthlyCardLimit());
        dto.setCheckAmountPattern(limit.getCheckAmountPattern());
        dto.setMaxAmount(limit.getMaxAmount());
        dto.setPerDayTransactionLimit(limit.getPerDayTransactionLimit());
        dto.setMonthlyTransactionLimit(limit.getMonthlyTransactionLimit());
        dto.setBlocked(limit.getBlocked());
        dto.setCreatedOn(limit.getCreatedOn());
        dto.setCreatedBy(limit.getCreatedBy());
        dto.setEditedOn(limit.getEditedOn());
        dto.setEditedBy(limit.getEditedBy());

        // Flatten product
        if (limit.getProduct() != null) {
            dto.setProductId(limit.getProduct().getId());
            dto.setProductName(limit.getProduct().getName());
        }

        // Flatten subproduct
        if (limit.getSubProduct() != null) {
            dto.setSubProductId(limit.getSubProduct().getId());
            dto.setSubProductName(limit.getSubProduct().getSubProductTitle());
        }

        return CommonResponse.success(dto, "Limit fetched successfully");
    }


    @Override
    public CommonResponse<Page<ProductLimitResponseDto>> getAll(int page, int size) {
        Page<ProductTransactionLimit> pagedResult = repository.findAll(PageRequest.of(page, size));

        Page<ProductLimitResponseDto> dtoPage = pagedResult.map(limit -> {
            ProductLimitResponseDto dto = new ProductLimitResponseDto();

            dto.setId(limit.getId());
            dto.setDescription(limit.getDescription());
            dto.setWalletType(limit.getWalletType());
            dto.setPaymentCurrency(limit.getPaymentCurrency());
            dto.setIsAllowPerDayCardLimit(limit.getIsAllowPerDayCardLimit());
            dto.setIsAllowMonthlyCardLimit(limit.getIsAllowMonthlyCardLimit());
            dto.setCheckAmountPattern(limit.getCheckAmountPattern());
            dto.setMaxAmount(limit.getMaxAmount());
            dto.setPerDayTransactionLimit(limit.getPerDayTransactionLimit());
            dto.setMonthlyTransactionLimit(limit.getMonthlyTransactionLimit());

            // Flatten product info
            if (limit.getProduct() != null) {
                dto.setProductId(limit.getProduct().getId());
                dto.setProductName(limit.getProduct().getName());
            }

            // Flatten sub-product info
            if (limit.getSubProduct() != null) {
                dto.setSubProductId(limit.getSubProduct().getId());
                dto.setSubProductName(limit.getSubProduct().getSubProductTitle());
            }

            // Audit Fields
            dto.setBlocked(limit.getBlocked());
            dto.setCreatedOn(limit.getCreatedOn());
            dto.setCreatedBy(limit.getCreatedBy());
            dto.setEditedOn(limit.getEditedOn());
            dto.setEditedBy(limit.getEditedBy());

            return dto;
        });

        return CommonResponse.success(dtoPage, "Limits fetched successfully");
    }

}

