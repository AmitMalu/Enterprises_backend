package com.im.admin.service.impl;

import com.im.admin.entities.Operators;
import com.im.admin.repositories.OperatorsRepository;
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
    private CommonMethods commonMethods;

    @Override
    public Operators saveOperator(Operators operator) {
        log.info("Saving new operator: {}", operator);

        if (operatorsRepository.existsByOperatorCode(operator.getOperatorCode())) {
            throw new RuntimeException("Operator Code already exists!");
        }

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

        existingOperator.setOperatorCategory(operator.getOperatorCategory());
        existingOperator.setOperatorCode(operator.getOperatorCode());
        existingOperator.setOperatorName(operator.getOperatorName());
        existingOperator.setOperatorAlias(operator.getOperatorAlias());
        existingOperator.setAlias(operator.getAlias());
        existingOperator.setProductId(operator.getProductId());
        existingOperator.setProductCode(operator.getProductCode());
        existingOperator.setSubProductId(operator.getSubProductId());
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
    public ResponseEntity<?> getActiveOperatorsBySubProductId(String subProductId) {
        List<Operators> operators = operatorsRepository.findBySubProductIdAndIsActiveTrue(subProductId);

        if (operators.isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "No active operators found for the given SubProduct ID: " + subProductId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        List<Map<String, Object>> result = operators.stream()
                .map(op -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", op.getId());
                    map.put("operatorName", op.getOperatorName());
                    map.put("operatorCode", op.getOperatorCode());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}

