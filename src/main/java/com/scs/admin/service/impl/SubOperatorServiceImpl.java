package com.scs.admin.service.impl;


import com.scs.admin.entities.Operators;
import com.scs.admin.entities.SubOperator;
import com.scs.admin.repositories.OperatorsRepository;
import com.scs.admin.repositories.SubOperatorRepository;
import com.scs.admin.service.SubOperatorService;
import com.scs.admin.spec.SubOperatorSpecification;
import com.scs.admin.util.CommonMethods;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
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
        Operators operator = operatorsRepository.findByOperatorCode(subOperator.getOperatorCode())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with code: " + subOperator.getOperatorCode()));

        // 3. Set alias from operator
        subOperator.setOperatorCode(operator.getOperatorCode());
        subOperator.setOperatorName(operator.getOperatorName());
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
        Operators operator = operatorsRepository.findByOperatorCode(subOperator.getOperatorCode())
                .orElseThrow(() -> new IllegalArgumentException("Operator not found with code: " + subOperator.getOperatorCode()));


        // Update fields
        existingSubOperator.setOperatorName(operator.getOperatorName());
        existingSubOperator.setOperatorCode(operator.getOperatorCode());
        existingSubOperator.setOperatorAlias(operator.getOperatorAlias()); // Update alias
        existingSubOperator.setSubOperatorName(subOperator.getSubOperatorName());
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
    public Page<SubOperator> getAllSubOperators(Pageable pageable,
                                                List<String> id,
                                                String subOperatorName,
                                                String operatorCode,
                                                String operatorAlias,
                                                String category,
                                                String operatorName,
                                                Boolean isBlocked,
                                                String createdBy,
                                                String editedBy,
                                                LocalDate createdOn,
                                                LocalDate editedOn) {

        Specification<SubOperator> spec = SubOperatorSpecification.getFilter(
                id, subOperatorName, operatorCode, operatorAlias,category,
                operatorName, isBlocked, createdBy, editedBy,
                createdOn, editedOn
        );
        return subOperatorRepository.findAll(spec, pageable);
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
