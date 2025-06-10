package com.im.admin.service.impl;

import com.im.admin.dto.ProductDropdownResponseDto;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.service.ProductsService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    public Products updateProduct(Short id, Products product) {
        log.info("Updating product with ID: {}", id);

        return productsRepository.findById(id).map(existingProduct -> {
            product.setId(id); // Preserve the existing ID
            Products updatedProduct = productsRepository.save(product);
            log.info("Product updated successfully: {}", updatedProduct);
            return updatedProduct;
        }).orElseThrow(() -> new EntityNotFoundException("Product not found with ID: " + id));
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
    public List<Products> getAllProducts(Pageable pageable) {
        log.info("Fetching all products with pagination.");
        return productsRepository.findAll(pageable).getContent();
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



