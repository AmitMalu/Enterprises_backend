package com.im.admin.service;

import com.im.admin.entities.SubProducts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface SubProductsService {

    SubProducts save(SubProducts subProduct);

    SubProducts update(Long id, SubProducts subProduct);

    Page<SubProducts> getAll(Pageable pageable);

    Optional<SubProducts> getById(Long id);

    void delete(Long id);
}
