package com.im.admin.service;

import com.im.admin.dto.ProductDropdownResponseDto;
import com.im.admin.entities.product.Products;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductsService {

    Products saveProduct(Products product);

    Products updateProduct(Short id, Products product);

    void deleteProduct(Short id);

    Page<Products> getAllProducts(Pageable pageable,List<Short>  id,String name, String productCode, String productCodeForScheme,
                                  String productGroup, Boolean isShow, Boolean hasCharges,
                                  Boolean isCommissions, Boolean isInventory, Boolean isSettlement,
                                  Boolean isVirtual, Boolean isVisible, Boolean reverseTx,
                                  Boolean clusterMode, Boolean blocked, Boolean active);

    Products getProductById(Short id);

    List<ProductDropdownResponseDto> getAllActiveProducts();
}



