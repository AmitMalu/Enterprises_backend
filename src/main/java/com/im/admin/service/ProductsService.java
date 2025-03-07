package com.im.admin.service;

import com.im.admin.entities.product.Products;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductsService {

    Products saveProduct(Products product);

    Products updateProduct(Short id, Products product);

    void deleteProduct(Short id);

    List<Products> getAllProducts(Pageable pageable);

    Products getProductById(Short id);
}



