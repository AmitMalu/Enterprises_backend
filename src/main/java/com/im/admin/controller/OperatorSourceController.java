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
@RequestMapping("/api/operators")
@Slf4j
public class OperatorSourceController {

    @Autowired
    private OperatorSourceService operatorSourceService;

    @PostMapping
    public ResponseEntity<OperatorSource> createOperatorSource(@Valid @RequestBody OperatorSource operatorSource) {
        log.info("Received request to create OperatorSource: {}", operatorSource);
        return new ResponseEntity<>(operatorSourceService.saveOperatorSource(operatorSource), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperatorSource> updateOperatorSource(@PathVariable Long id, @Valid @RequestBody OperatorSource operatorSource) {
        log.info("Received request to update OperatorSource with ID: {}", id);
        return new ResponseEntity<>(operatorSourceService.updateOperatorSource(id, operatorSource), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<OperatorSource>> getAllOperatorSources() {
        log.info("Received request to get all OperatorSources");
        return new ResponseEntity<>(operatorSourceService.getAllOperatorSources(), HttpStatus.OK);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<OperatorSource>> getAllOperatorSourcesWithPagination(Pageable pageable) {
        log.info("Received request to get paged OperatorSources: {}", pageable);
        return new ResponseEntity<>(operatorSourceService.getAllOperatorSourcesWithPagination(pageable), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<OperatorSource>> getOperatorSourceById(@PathVariable Long id) {
        log.info("Received request to get OperatorSource by ID: {}", id);
        return new ResponseEntity<>(operatorSourceService.getOperatorSourceById(id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperatorSource(@PathVariable Long id) {
        log.info("Received request to delete OperatorSource by ID: {}", id);
        operatorSourceService.deleteOperatorSource(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/dropdown")
    public ResponseEntity<List<Map<String, Object>>> getOperatorDropdown() {
        List<Map<String, Object>> dropdownList = operatorSourceService.getOperatorDropdown();
        return ResponseEntity.ok(dropdownList);
    }
}

