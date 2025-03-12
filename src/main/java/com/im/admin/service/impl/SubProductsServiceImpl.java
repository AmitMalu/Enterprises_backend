package com.im.admin.service.impl;

import com.im.admin.entities.SubProducts;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.service.SubProductsService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class SubProductsServiceImpl implements SubProductsService {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public SubProducts saveSubProduct(SubProducts subProduct) {
        subProduct.setId(commonMethods.generateNextIdForTables("sub_products", "id"));
        SubProducts savedSubProduct = subProductsRepository.save(subProduct);
        log.info("SubProduct {} saved successfully.", savedSubProduct.getId());
        return savedSubProduct;
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
                .orElseThrow(() -> new RuntimeException("SubProduct with ID " + id + " not found."));

        existingSubProduct.setSubProductTitle(subProduct.getSubProductTitle());
        existingSubProduct.setProduct(subProduct.getProduct());
        existingSubProduct.setSubProductCode(subProduct.getSubProductCode());
        existingSubProduct.setIsSettlement(subProduct.getIsSettlement());
        existingSubProduct.setEnabled(subProduct.getEnabled());
        existingSubProduct.setEditedBy(subProduct.getEditedBy());

        SubProducts updatedSubProduct = subProductsRepository.save(existingSubProduct);
        log.info("Updated SubProduct with ID: {}", id);
        return updatedSubProduct;
    }

    @Override
    public Page<SubProducts> getAllSubProducts(Pageable pageable) {
        log.info("Fetching all SubProducts with pagination");
        return subProductsRepository.findAll(pageable);
    }

    @Override
    public SubProducts getSubProductById(String id) {
        return subProductsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubProduct with ID " + id + " not found."));
    }
}

