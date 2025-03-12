package com.im.admin.service.impl;


import com.im.admin.entities.SubOperator;
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
    private CommonMethods commonMethods;

    @Override
    @Transactional
    public SubOperator saveSubOperator(SubOperator subOperator, String createdBy) {

        subOperator.setId(commonMethods.generateNextIdForTables("operators", "id"));
        subOperator.setCreatedBy(createdBy);

        //log.info("Saving new SubOperator with ID: {}", nextId);
        return subOperatorRepository.save(subOperator);
    }

    @Override
    @Transactional
    public SubOperator updateSubOperator(String id, SubOperator subOperator, String editedBy) {
        SubOperator existingSubOperator = subOperatorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SubOperator not found with ID: " + id));

        existingSubOperator.setOperatorCode(subOperator.getOperatorCode());
        existingSubOperator.setOperatorName(subOperator.getOperatorName());
        existingSubOperator.setSubOperatorName(subOperator.getSubOperatorName());
        existingSubOperator.setVendor(subOperator.getVendor());
        existingSubOperator.setEditedBy(editedBy);

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
