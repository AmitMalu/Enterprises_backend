package com.im.admin.service.impl;

import com.im.admin.dto.OperatorDropdownResponseDTO;
import com.im.admin.entities.Operators;
import com.im.admin.entities.SubProducts;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.OperatorsRepository;
import com.im.admin.repositories.ProductsRepository;
import com.im.admin.repositories.SubProductsRepository;
import com.im.admin.service.OperatorsService;
import com.im.admin.util.CommonMethods;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OperatorsServiceImpl implements OperatorsService {

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    public Operators saveOperator(Operators operator) {
        log.info("Saving new operator: {}", operator);

        if (operatorsRepository.existsByOperatorCode(operator.getOperatorCode())) {
            throw new RuntimeException("Operator Code already exists!");
        }

        // Fetch and set Product
        Products product = productsRepository.findById(Short.valueOf(operator.getProductId()))
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + operator.getProductId()));
        operator.setProductCode(product.getProductCode());

        // Fetch and set SubProduct
        SubProducts subProduct = subProductsRepository.findById(operator.getSubProductId())
                .orElseThrow(() -> new RuntimeException("SubProduct not found with ID: " + operator.getSubProductId()));
        operator.setSubProductCode(subProduct.getSubProductCode());

        operator.setId(commonMethods.generateNextIdForTables("operators", "id"));

        Operators savedOperator = operatorsRepository.save(operator);
        log.info("Operator saved successfully with ID: {}", savedOperator.getId());
        return savedOperator;
    }


    @Override
    public Operators updateOperator(String id, Operators operator) {
        log.info("Updating operator with ID: {}", id);

        Operators existingOperator = operatorsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with ID: " + id));

        Products product = productsRepository.findById(Short.valueOf(operator.getProductId()))
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + operator.getProductId()));

        SubProducts subProduct = subProductsRepository.findById(String.valueOf(operator.getSubProductId()))
                .orElseThrow(() -> new RuntimeException("SubProduct not found with ID: " + operator.getSubProductId()));

        // Update fields
        existingOperator.setOperatorCategory(operator.getOperatorCategory());
        existingOperator.setOperatorCode(operator.getOperatorCode());
        existingOperator.setOperatorName(operator.getOperatorName());
        existingOperator.setOperatorAlias(operator.getOperatorAlias());
        existingOperator.setAlias(operator.getAlias());
        existingOperator.setProductCode(product.getProductCode());
        existingOperator.setProductCode(operator.getProductCode());
        existingOperator.setSubProductCode(subProduct.getSubProductCode());
        existingOperator.setSubProductCode(operator.getSubProductCode());
        existingOperator.setIsActive(operator.getIsActive());
        existingOperator.setEditedBy(operator.getEditedBy());

        Operators updatedOperator = operatorsRepository.save(existingOperator);
        log.info("Operator updated successfully: {}", updatedOperator);
        return updatedOperator;
    }

    @Override
    public void deleteOperator(String id) {
        log.info("Deleting operator with ID: {}", id);

        if (!operatorsRepository.existsById(id)) {
            throw new RuntimeException("Operator not found with ID: " + id);
        }

        operatorsRepository.deleteById(id);
        log.info("Operator deleted successfully: {}", id);
    }

    @Override
    public Page<Operators> getAllOperators(int page, int size) {
        log.info("Fetching all operators - Page: {}, Size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return operatorsRepository.findAll(pageable);
    }

    @Override
    public Operators getOperatorById(String id) {
        log.info("Fetching operator with ID: {}", id);

        return operatorsRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Operator not found with ID: " + id));
    }

    @Override
    public List<OperatorDropdownResponseDTO> getActiveOperatorsBySubProductId(String subProductId) {
        List<Operators> list = operatorsRepository.findBySubProductIdAndIsActiveTrue(subProductId);

        return list.stream()
                .map(op -> new OperatorDropdownResponseDTO(op.getId(), op.getOperatorName(), op.getOperatorCode()))
                .collect(Collectors.toList());
    }

}

