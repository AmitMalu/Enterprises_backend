package com.im.admin.service;

import com.im.admin.entities.SubProducts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SubProductsService {

    SubProducts saveSubProduct(SubProducts subProduct);

    void deleteSubProduct(String id);

    SubProducts updateSubProduct(String id, SubProducts subProduct);

    Page<SubProducts> getAllSubProducts(Pageable pageable);

    SubProducts getSubProductById(String id);
}

