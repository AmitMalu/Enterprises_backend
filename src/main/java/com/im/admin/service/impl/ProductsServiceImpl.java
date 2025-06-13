package com.im.admin.service.impl;

import com.im.admin.dto.ProductDropdownResponseDto;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.service.ProductsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductsServiceImpl implements ProductsService {

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public Products saveProduct(Products product) {
        log.info("Saving new product: {}", product);

        Short lastInsertedId = productsRepository.findMaxId(); // Fetch the max ID

        if (lastInsertedId != null && lastInsertedId >= 99) {
            throw new IllegalArgumentException("Product ID limit reached (10-99). No more IDs available.");
        }

        // Convert SGST and TDS to percentage form (e.g., 18 → 0.18)
//        if (product.getSgst() != null && product.getSgst() > 1) {
//            product.setSgst(product.getSgst() / 100);
//        }
//
//        if (product.getTds() != null && product.getTds() > 1) {
//            product.setTds(product.getTds() / 100);
//        }

        Products savedProduct = productsRepository.save(product);

        log.info("Product saved successfully with ID: {}", savedProduct.getId());
        return savedProduct;
    }


    @Override
    @Transactional
    public Products updateProduct(Short id, Products product) {
        log.info("Updating product with ID: {}", id);

        Products existingProduct = productsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));

        // Field-by-field update
        existingProduct.setName(product.getName());
        existingProduct.setProductCode(product.getProductCode());
        existingProduct.setProductCodeForScheme(product.getProductCodeForScheme());
        existingProduct.setIsVisible(product.getIsVisible());
        existingProduct.setTransactionType(product.getTransactionType());
        existingProduct.setIsCommissions(product.getIsCommissions());
        existingProduct.setSelectIsCommission(product.getSelectIsCommission());
        existingProduct.setActive(product.getActive());
        existingProduct.setDashboardUrl(product.getDashboardUrl());
        existingProduct.setDashboardUrlForApk(product.getDashboardUrlForApk());
        existingProduct.setDashboardImage(product.getDashboardImage());
        existingProduct.setPriority(product.getPriority());
        existingProduct.setProductGroup(product.getProductGroup());
        existingProduct.setReverseTx(product.getReverseTx());
        existingProduct.setClusterMode(product.getClusterMode());
        existingProduct.setIsSettlement(product.getIsSettlement());
        existingProduct.setHasCharges(product.getHasCharges());
        existingProduct.setBlocked(product.getBlocked());

        // Embedded objects
        existingProduct.setInventory(product.getInventory());
        existingProduct.setCluster(product.getCluster());

        // Financials
        existingProduct.setMinimumPostpaidBalance(product.getMinimumPostpaidBalance());
        existingProduct.setMinimumPrepaidBalance(product.getMinimumPrepaidBalance());
        existingProduct.setMinimumGullakBalance(product.getMinimumGullakBalance());
        existingProduct.setMinimumSingleTransactionAmount(product.getMinimumSingleTransactionAmount());
        existingProduct.setMaximumSingleTransactionAmount(product.getMaximumSingleTransactionAmount());
        existingProduct.setSgst(product.getSgst());
        existingProduct.setTds(product.getTds());

        // Only editedBy is updated — preserve createdBy
        existingProduct.setEditedBy(product.getEditedBy());

        Products updatedProduct = productsRepository.save(existingProduct);
        log.info("Product updated successfully: {}", updatedProduct);
        return updatedProduct;
    }


    @Override
    public void deleteProduct(Short id) {
        log.info("Deleting product with ID: {}", id);

        if (!productsRepository.existsById(id)) {
            throw new EntityNotFoundException("Product not found with ID: " + id);
        }

        productsRepository.deleteById(id);
        log.info("Product deleted successfully.");
    }

    @Override
    public Page<Products> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination.");
        return productsRepository.findAll(pageable);
    }

    @Override
    public Products getProductById(Short id) {
        log.info("Fetching product with ID: {}", id);

        return productsRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
    }

    @Override
    public List<ProductDropdownResponseDto> getAllActiveProducts() {
        List<Products> activeProducts = productsRepository.findByActiveTrue();

        return activeProducts.stream()
                .map(product -> new ProductDropdownResponseDto(
                        product.getId(),
                        product.getName(),
                        product.getProductCode()))
                .collect(Collectors.toList());
    }

}



