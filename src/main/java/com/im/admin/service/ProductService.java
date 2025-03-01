package com.im.admin.service;

import com.im.admin.entities.product.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Products saveProduct(Products product);

    Products updateProduct(Long id, Products product);

    Page<Products> getAllProducts(Pageable pageable);

    Products getProductById(Long id);

    void deleteProduct(Long id);
}


