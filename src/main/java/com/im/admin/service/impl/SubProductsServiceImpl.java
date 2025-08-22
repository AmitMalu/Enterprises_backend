package com.im.admin.service.impl;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.SubProductsResponseDTO;
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

        Products product = productsRepository.findById(subProduct.getProduct().getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProduct().getId()));

        subProduct.setProduct(product);

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
        if (!Objects.equals(existingSubProduct.getProduct().getId(), subProduct.getProduct().getId())) {
            Products product = productsRepository.findById(subProduct.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid productId: " + subProduct.getProduct().getId()));
            existingSubProduct.setProduct(product);
        }

        existingSubProduct.setSubProductTitle(subProduct.getSubProductTitle());
        existingSubProduct.setSubProductCode(subProduct.getSubProductCode());
//        existingSubProduct.setProductId(subProduct.getProductId());
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
    public SubProductsResponseDTO getSubProductById(String id) {
        SubProducts subProduct = subProductsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubProduct with ID " + id + " not found."));

        return toDTO(subProduct);
    }

    @Override
    public Page<SubProductsResponseDTO> getAllSubProducts(Pageable pageable, List<String> id,
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

        Page<SubProducts> subProductEntities = subProductsRepository.findAll(spec, pageable);

        return subProductEntities.map(this::toDTO);
    }


    @Override
    public List<SubProductDropdownResponseDto> getActiveSubProductsByProductId(
            Short productId, Boolean isSettlement) {
        log.info("Fetching sub-products for productId: {}, isSettlement: {}", productId, isSettlement);

        try {
            List<SubProducts> subProducts;

            if (isSettlement == null) {
                subProducts = subProductsRepository.findByProductIdAndIsActiveTrue(productId);
            } else {
                subProducts = subProductsRepository.findByProductIdAndIsActiveTrueAndIsSettlement(productId, isSettlement);
            }
            log.info("Found {} sub-products for productId: {}, isSettlement: {}",
                    subProducts.size(), productId, isSettlement);

            return subProducts.stream()
                    .map(subProduct -> new SubProductDropdownResponseDto(
                            subProduct.getId(),
                            subProduct.getSubProductTitle(),
                            subProduct.getSubProductCode()))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error while fetching sub-products for productId: {}, isSettlement: {}", productId, isSettlement, e);
            throw new RuntimeException("Error fetching sub-products", e);
        }
    }

    public SubProductsResponseDTO toDTO(SubProducts entity) {
        SubProductsResponseDTO dto = new SubProductsResponseDTO();
        dto.setId(entity.getId());
        dto.setSubProductTitle(entity.getSubProductTitle());
        dto.setSubProductCode(entity.getSubProductCode());
        dto.setWalletType(entity.getWalletType());
        dto.setIsSettlement(entity.getIsSettlement());
        dto.setSettlementTime(entity.getSettlementTime());
        dto.setIsActive(entity.getIsActive());
        dto.setIsVisible(entity.getIsVisible());
        dto.setIsBlocked(entity.getIsBlocked());
        dto.setIsWhiteLabelAdmin(entity.getIsWhiteLabelAdmin());
        dto.setWhiteLabelAdminId(entity.getWhiteLabelAdminId());
        dto.setUserField1(entity.getUserField1());
        dto.setUserField2(entity.getUserField2());
        dto.setUserField3(entity.getUserField3());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setEditedBy(entity.getEditedBy());
        dto.setCreatedOn(entity.getCreatedOn());
        dto.setEditedOn(entity.getEditedOn());

        if (entity.getProduct() != null) {
            dto.setProductId(entity.getProduct().getId());
            dto.setProductCode(entity.getProduct().getProductCode());
            dto.setProductName(entity.getProduct().getName());
        }

        return dto;
    }

    @Override
    public List<SubProductDropdownResponseDto> getSubProductDropdown() {
        List<SubProducts> allSubProducts = subProductsRepository.findAll();

        return allSubProducts.stream()
                .map(sp -> new SubProductDropdownResponseDto(sp.getId(), sp.getSubProductTitle()))
                .collect(Collectors.toList());
    }

}

