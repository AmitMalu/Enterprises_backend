package com.im.admin.service.impl;

import com.im.admin.entities.product.Products;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductsRepository productsRepository;

    @Override
    public Products saveProduct(Products product) {
        return productsRepository.save(product);
    }

    @Override
    public Products updateProduct(Long id, Products product) {
        return productsRepository.findById(id)
                .map(existingProduct -> {
                    product.setId(id);
                    return productsRepository.save(product);
                }).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public Page<Products> getAllProducts(Pageable pageable) {
        return productsRepository.findAll(pageable);
    }

    @Override
    public Products getProductById(Long id) {
        return productsRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public void deleteProduct(Long id) {
        if (!productsRepository.existsById(id)) {
            throw new RuntimeException("Product not found");
        }
        productsRepository.deleteById(id);
    }
}


