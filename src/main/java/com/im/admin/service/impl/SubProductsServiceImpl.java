package com.im.admin.service.impl;

import com.im.admin.entities.SubProducts;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.service.SubProductsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class SubProductsServiceImpl implements SubProductsService {

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Override
    public SubProducts save(SubProducts subProduct) {
        log.info("Saving SubProduct: {}", subProduct);
        return subProductsRepository.save(subProduct);
    }

    @Override
    public SubProducts update(Long id, SubProducts subProduct) {
        log.info("Updating SubProduct with ID: {}", id);
        return subProductsRepository.findById(id).map(existing -> {
            existing.setSubProductTitle(subProduct.getSubProductTitle());
            existing.setProduct(subProduct.getProduct());
            existing.setSubProductCode(subProduct.getSubProductCode());
            existing.setIsSettlement(subProduct.getIsSettlement());
            existing.setEnabled(subProduct.getEnabled());
            existing.setEditedBy(subProduct.getEditedBy());
            existing.setEditedOn(LocalDateTime.now());
            return subProductsRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("SubProduct not found"));
    }

    @Override
    public Page<SubProducts> getAll(Pageable pageable) {
        log.info("Fetching all SubProducts with pagination");
        return subProductsRepository.findAll(pageable);
    }

    @Override
    public Optional<SubProducts> getById(Long id) {
        log.info("Fetching SubProduct with ID: {}", id);
        return subProductsRepository.findById(id);
    }

    @Override
    public void delete(Long id) {
        log.info("Deleting SubProduct with ID: {}", id);
        subProductsRepository.deleteById(id);
    }
}
