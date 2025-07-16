package com.im.admin.service.impl;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.service.SubProductsService;
import com.im.admin.spec.SubProductSpecification;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubProductsServiceImpl implements SubProductsService {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public SubProducts saveSubProduct(SubProducts subProduct) {

        // Fetch the product by ID
        Products product = productsRepository.findById(subProduct.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProductId()));

        // Set name and code from product
        subProduct.setProductName(product.getName());
        subProduct.setProductCode(product.getProductCode());

        Optional<SubProducts> existingSubProduct = subProductsRepository
                .findBySubProductCodeOrSubProductTitle(subProduct.getSubProductCode(), subProduct.getSubProductTitle());

        if (existingSubProduct.isPresent()) {
            SubProducts existing = existingSubProduct.get();
            if (existing.getSubProductCode().equals(subProduct.getSubProductCode())) {
                throw new IllegalArgumentException("Sub Product Code '" + subProduct.getSubProductCode() + "' is already in use.");
            }
            if (existing.getSubProductTitle().equals(subProduct.getSubProductTitle())) {
                throw new IllegalArgumentException("Sub Product Title '" + subProduct.getSubProductTitle() + "' is already in use.");
            }
        }

        subProduct.setId(commonMethods.generateNextIdForTables("sub_products", "id"));

        if(!Boolean.TRUE.equals(subProduct.getIsSettlement())) {
            subProduct.setIsSettlement(false);
            subProduct.setSettlementTime("-");
        }

        if (!Boolean.TRUE.equals(subProduct.getIsWhiteLabelAdmin())) {
            subProduct.setIsWhiteLabelAdmin(false);
            subProduct.setWhiteLabelAdminId("-");
        }

        return subProductsRepository.save(subProduct);
    }


    @Override
    @Transactional
    public void deleteSubProduct(String id) {
        if (!subProductsRepository.existsById(id)) {
            throw new RuntimeException("SubProduct with ID " + id + " not found.");
        }
        subProductsRepository.deleteById(id);
        log.info("Deleted SubProduct with ID: {}", id);
    }

    @Override
    @Transactional
    public SubProducts updateSubProduct(String id, SubProducts subProduct) {
        SubProducts existingSubProduct = subProductsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("SubProduct with ID " + id + " not found."));

        // Check if productId is changed and fetch product
        if (!Objects.equals(existingSubProduct.getProductId(), subProduct.getProductId())) {
            Products product = productsRepository.findById(subProduct.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProductId()));
            existingSubProduct.setProductName(product.getName());
            existingSubProduct.setProductCode(product.getProductCode());
        }

        existingSubProduct.setSubProductTitle(subProduct.getSubProductTitle());
        existingSubProduct.setSubProductCode(subProduct.getSubProductCode());
        existingSubProduct.setProductId(subProduct.getProductId());
        existingSubProduct.setIsSettlement(subProduct.getIsSettlement());
        existingSubProduct.setWalletType(subProduct.getWalletType());
        existingSubProduct.setIsVisible(subProduct.getIsVisible());
        existingSubProduct.setIsBlocked(subProduct.getIsBlocked());
        existingSubProduct.setEditedBy(subProduct.getEditedBy());
        existingSubProduct.setIsActive(subProduct.getIsActive());

        if(Boolean.TRUE.equals(subProduct.getIsSettlement())) {
            existingSubProduct.setIsSettlement(true);
            existingSubProduct.setSettlementTime(subProduct.getSettlementTime());
        }else {
            existingSubProduct.setIsSettlement(false);
            existingSubProduct.setSettlementTime("-");
        }

        if (Boolean.TRUE.equals(subProduct.getIsWhiteLabelAdmin())) {
            existingSubProduct.setIsWhiteLabelAdmin(true);
            existingSubProduct.setWhiteLabelAdminId(subProduct.getWhiteLabelAdminId());
        }else {
            existingSubProduct.setIsWhiteLabelAdmin(false);
            existingSubProduct.setWhiteLabelAdminId("-");
        }

        SubProducts updatedSubProduct = subProductsRepository.save(existingSubProduct);
        log.info("Updated SubProduct with ID: {}", id);
        return updatedSubProduct;
    }

    @Override
    public Page<SubProducts> getAllSubProducts(Pageable pageable, List<String> id,
                                               String subProductTitle, String subProductCode, String productCode,
                                               Short productId, String productName, String walletType,
                                               Boolean isSettlement, Boolean isActive, Boolean enabled,
                                               String createdBy, String editedBy,
                                               LocalDate createdOn, LocalDate editedOn) {

        Specification<SubProducts> spec = SubProductSpecification.getFilter(
                id, subProductTitle, subProductCode, productCode, productId,
                productName, walletType, isSettlement, isActive, enabled,
                createdBy, editedBy, createdOn, editedOn
        );

        return subProductsRepository.findAll(spec, pageable);
    }


    @Override
    public SubProducts getSubProductById(String id) {
        return subProductsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubProduct with ID " + id + " not found."));
    }

    @Override
    public List<SubProductDropdownResponseDto> getActiveSubProductsByProductId(Short productId) {
        log.info("Fetching sub-products for productId: {}", productId);

        try {
            List<SubProducts> subProducts = subProductsRepository.findByProductIdAndIsActiveTrue(productId);

            log.info("Found {} active sub-products for productId: {}", subProducts.size(), productId);

            return subProducts.stream()
                    .map(subProduct -> new SubProductDropdownResponseDto(subProduct.getId(), subProduct.getSubProductTitle(), subProduct.getSubProductCode()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error while fetching sub-products for productId: {}", productId, e);
            throw new RuntimeException("Error fetching sub-products", e);
        }
    }
}

