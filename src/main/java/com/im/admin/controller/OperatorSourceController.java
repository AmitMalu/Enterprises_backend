package com.im.admin.controller;


import com.im.admin.dto.OperatorSourceDropdownDto;
import com.im.admin.entities.OperatorSource;
import com.im.admin.service.OperatorSourceService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/operator-source")
@Slf4j
public class OperatorSourceController {

    @Autowired
    private OperatorSourceService operatorSourceService;

    @PostMapping
    public ResponseEntity<OperatorSource> createOperatorSource(@Valid @RequestBody OperatorSource operatorSource) {
        log.info("Received request to create OperatorSource: {}", operatorSource);
        OperatorSource saved = operatorSourceService.saveOperatorSource(operatorSource);
        log.info("OperatorSource created successfully with ID: {}", saved.getId());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperatorSource> updateOperatorSource(@PathVariable Long id, @Valid @RequestBody OperatorSource operatorSource) {
        log.info("Received request to update OperatorSource with ID: {}", id);
        OperatorSource updated = operatorSourceService.updateOperatorSource(id, operatorSource);
        log.info("OperatorSource updated successfully with ID: {}", id);
        return new ResponseEntity<>(updated, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<OperatorSource>> getAllOperatorSources() {
        log.info("Received request to get all OperatorSources");
        List<OperatorSource> sources = operatorSourceService.getAllOperatorSources();
        log.info("Fetched {} OperatorSources", sources.size());
        return new ResponseEntity<>(sources, HttpStatus.OK);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<OperatorSource>> getAllOperatorSourcesWithPagination(Pageable pageable) {
        log.info("Received request to get paged OperatorSources: {}", pageable);
        Page<OperatorSource> page = operatorSourceService.getAllOperatorSourcesWithPagination(pageable);
        log.info("Fetched {} OperatorSources in page {}", page.getNumberOfElements(), page.getNumber());
        return new ResponseEntity<>(page, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<OperatorSource>> getOperatorSourceById(@PathVariable Long id) {
        log.info("Received request to get OperatorSource by ID: {}", id);
        Optional<OperatorSource> source = operatorSourceService.getOperatorSourceById(id);
        if (source.isPresent()) {
            log.info("OperatorSource found with ID: {}", id);
        } else {
            log.warn("OperatorSource not found with ID: {}", id);
        }
        return new ResponseEntity<>(source, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperatorSource(@PathVariable Long id) {
        log.info("Received request to delete OperatorSource by ID: {}", id);
        operatorSourceService.deleteOperatorSource(id);
        log.info("OperatorSource deleted successfully with ID: {}", id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getOperatorDropdown() {
        log.info("Received request to get OperatorSource dropdown list");
        List<Map<String, Object>> dropdownList = operatorSourceService.getOperatorDropdown();
        log.info("Dropdown list fetched with {} items", dropdownList.size());
        return ResponseEntity.ok(dropdownList);
    }
}

