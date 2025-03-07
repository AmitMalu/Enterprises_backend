package com.im.admin.controller;

import com.im.admin.entities.Operators;
import com.im.admin.service.OperatorsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators")
@Slf4j
@Validated
public class OperatorsController {

    @Autowired
    private OperatorsService operatorsService;

    @PostMapping
    public ResponseEntity<Operators> createOperator(@Valid @RequestBody Operators operator) {
        log.info("Received request to create operator: {}", operator);
        Operators savedOperator = operatorsService.saveOperator(operator);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedOperator);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Operators> updateOperator(@PathVariable String id, @Valid @RequestBody Operators operator) {
        log.info("Received request to update operator with ID: {}", id);
        Operators updatedOperator = operatorsService.updateOperator(id, operator);
        return ResponseEntity.ok(updatedOperator);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOperator(@PathVariable String id) {
        log.info("Received request to delete operator with ID: {}", id);
        operatorsService.deleteOperator(id);
        return ResponseEntity.ok("Operator deleted successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Operators> getOperatorById(@PathVariable String id) {
        log.info("Received request to fetch operator with ID: {}", id);
        Operators operator = operatorsService.getOperatorById(id);
        return ResponseEntity.ok(operator);
    }

    @GetMapping
    public ResponseEntity<Page<Operators>> getAllOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Received request to fetch all operators - Page: {}, Size: {}", page, size);
        Page<Operators> operators = operatorsService.getAllOperators(page, size);
        return ResponseEntity.ok(operators);
    }
}

