package com.scs.admin.service.impl;

import com.scs.admin.dto.OperatorSourceDropdownDto;
import com.scs.admin.entities.OperatorSource;
import com.scs.admin.exception.ResourceNotFoundException;
import com.scs.admin.repositories.OperatorSourceRepository;
import com.scs.admin.service.OperatorSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OperatorSourceServiceImpl implements OperatorSourceService {

    @Autowired
    private OperatorSourceRepository operatorSourceRepository;

    @Override
    public OperatorSource saveOperatorSource(OperatorSource operatorSource) {
        log.info("Saving new OperatorSource: {}", operatorSource);
        return operatorSourceRepository.save(operatorSource);
    }

    @Override
    public OperatorSource updateOperatorSource(Long id, OperatorSource operatorSource) {
        log.info("Updating OperatorSource with ID: {}", id);
        OperatorSource existing = operatorSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OperatorSource not found with ID: " + id));

        existing.setTitle(operatorSource.getTitle());
        existing.setValue(operatorSource.getValue());
        existing.setOperatorClass(operatorSource.getOperatorClass());
        existing.setBlocked(operatorSource.getBlocked());
        existing.setSort(operatorSource.getSort());
        existing.setRole(operatorSource.getRole());

        return operatorSourceRepository.save(existing);
    }

    @Override
    public List<OperatorSource> getAllOperatorSources() {
        log.info("Fetching all OperatorSources");
        return operatorSourceRepository.findAll();
    }

    @Override
    public Page<OperatorSource> getAllOperatorSourcesWithPagination(Pageable pageable) {
        log.info("Fetching OperatorSources with pagination: {}", pageable);
        return operatorSourceRepository.findAll(pageable);
    }

    @Override
    public Optional<OperatorSource> getOperatorSourceById(Long id) {
        log.info("Fetching OperatorSource by ID: {}", id);
        return operatorSourceRepository.findById(id);
    }

    @Override
    public void deleteOperatorSource(Long id) {
        log.info("Deleting OperatorSource with ID: {}", id);
        OperatorSource existing = operatorSourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OperatorSource not found with ID: " + id));
        operatorSourceRepository.delete(existing);
    }

    @Override
    public List<Map<String, Object>> getOperatorDropdown() {
        return operatorSourceRepository.findAll()
                .stream()
                .map(op -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", op.getId());
                    map.put("title", op.getTitle());
                    return map;
                })
                .collect(Collectors.toList());
    }

}

