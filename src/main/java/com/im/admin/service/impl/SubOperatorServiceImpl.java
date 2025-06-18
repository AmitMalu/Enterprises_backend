package com.im.admin.service.impl;


import com.im.admin.entities.Operators;
import com.im.admin.entities.SubOperator;
import com.im.admin.repositories.OperatorsRepository;
import com.im.admin.repositories.SubOperatorRepository;
import com.im.admin.service.SubOperatorService;
import com.im.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubOperatorServiceImpl implements SubOperatorService {

    @Autowired
    private SubOperatorRepository subOperatorRepository;

    @Autowired
    private OperatorsRepository operatorsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Override
    @Transactional
    public SubOperator saveSubOperator(SubOperator subOperator) {
        // 1. Check if SubOperator with same name exists
        Optional<SubOperator> existing = subOperatorRepository.findBySubOperatorName(subOperator.getSubOperatorName());
        if (existing.isPresent()) {
            throw new IllegalArgumentException("SubOperator already exists with name: " + subOperator.getSubOperatorName());
        }

        subOperator.setId(commonMethods.generateNextIdForTables("operators", "id"));
        // 2. Find related Operator
        Operators operator = operatorsRepository.findByOperatorName(subOperator.getOperatorName())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with name: " + subOperator.getOperatorName()));

        // 3. Set alias from operator
        subOperator.setOperatorCode(operator.getOperatorCode());
        subOperator.setOperatorAlias(operator.getOperatorAlias());

        // 2. Find related Operator
        Operators operator = operatorsRepository.findByOperatorName(subOperator.getOperatorName())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with name: " + subOperator.getOperatorName()));

        // 3. Set alias from operator
        subOperator.setOperatorAlias(operator.getOperatorAlias());

        // 4. Generate ID and save
        subOperator.setId(commonMethods.generateNextIdForTables("sub_operators", "id"));
        return subOperatorRepository.save(subOperator);
    }


    @Override
    @Transactional
    public SubOperator updateSubOperator(String id, SubOperator subOperator) {
        SubOperator existingSubOperator = subOperatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubOperator not found with ID: " + id));

        // Check for duplicate subOperatorName if changed
        if (!existingSubOperator.getSubOperatorName().equalsIgnoreCase(subOperator.getSubOperatorName())) {
            subOperatorRepository.findBySubOperatorName(subOperator.getSubOperatorName()).ifPresent(existing -> {
                throw new IllegalArgumentException("SubOperator already exists with name: " + subOperator.getSubOperatorName());
            });
        }

        // Validate and fetch Operator
        Operators operator = operatorsRepository.findByOperatorName(subOperator.getOperatorName())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with name: " + subOperator.getOperatorName()));

        // Update fields
        existingSubOperator.setOperatorName(subOperator.getOperatorName());
        existingSubOperator.setOperatorCode(operator.getOperatorCode());
        existingSubOperator.setOperatorAlias(operator.getOperatorAlias()); // Update alias
        existingSubOperator.setSubOperatorName(subOperator.getSubOperatorName());
        existingSubOperator.setVendor(subOperator.getVendor());
        existingSubOperator.setEditedBy(subOperator.getEditedBy());
        existingSubOperator.setOperatorAlias(operator.getOperatorAlias()); // Update alias
        existingSubOperator.setSubOperatorName(subOperator.getSubOperatorName());
        existingSubOperator.setCategory(subOperator.getCategory());
        existingSubOperator.setIsBlocked(subOperator.getIsBlocked());
        existingSubOperator.setEditedBy(subOperator.getEditedBy());

        log.info("Updating SubOperator with ID: {}", id);
        return subOperatorRepository.save(existingSubOperator);
    }


    @Override
    @Transactional
    public void deleteSubOperator(String id) {
        if (!subOperatorRepository.existsById(id)) {
            throw new RuntimeException("SubOperator not found with ID: " + id);
        }
        subOperatorRepository.deleteById(id);
        log.info("Deleted SubOperator with ID: {}", id);
    }

    @Override
    public List<SubOperator> getAllSubOperators() {
        return subOperatorRepository.findAll();
    }

    @Override
    public Page<SubOperator> getAllSubOperatorsPaginated(Pageable pageable) {
        return subOperatorRepository.findAll(pageable);
    }

    @Override
    public Optional<SubOperator> getSubOperatorById(String id) {
        return subOperatorRepository.findById(id);
    }

}
