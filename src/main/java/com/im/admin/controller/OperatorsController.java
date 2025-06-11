package com.im.admin.controller;

import com.im.admin.entities.Operators;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.OperatorsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/operators")
@Slf4j
@Validated
public class OperatorsController {

    @Autowired
    private OperatorsService operatorsService;

    @PostMapping
    public ResponseEntity<CommonResponse<Operators>> createOperator(@Valid @RequestBody Operators operator) {
        log.info("Received request to create operator: {}", operator);
        Operators savedOperator = operatorsService.saveOperator(operator);
        CommonResponse<Operators> response = new CommonResponse<>(
                HttpStatus.CREATED.value(),
                "Operator created successfully",
                savedOperator
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Operators>> updateOperator(
            @PathVariable String id,
            @Valid @RequestBody Operators operator) {
        log.info("Received request to update operator with ID: {}", id);
        Operators updatedOperator = operatorsService.updateOperator(id, operator);
        CommonResponse<Operators> response = new CommonResponse<>(
                HttpStatus.OK.value(),
                "Operator updated successfully",
                updatedOperator
        );
        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteOperator(@PathVariable String id) {
        log.info("Received request to delete operator with ID: {}", id);
        operatorsService.deleteOperator(id);
        return ResponseEntity.ok("Operator deleted successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Operators>> getOperatorById(@PathVariable String id) {
        log.info("Received request to fetch operator with ID: {}", id);
        Operators operator = operatorsService.getOperatorById(id);
        return ResponseEntity.ok(
                new CommonResponse<>(HttpStatus.OK.value(), "Operator fetched successfully", operator)
        );
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<Operators>>> getAllOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Received request to fetch all operators - Page: {}, Size: {}", page, size);
        Page<Operators> operators = operatorsService.getAllOperators(page, size);

        return ResponseEntity.ok(
                new CommonResponse<>(HttpStatus.OK.value(), "Operators fetched successfully", operators)
        );
    }

    @GetMapping("/active/{subProductId}")
    public ResponseEntity<?> getActiveOperators(@PathVariable String subProductId) {

        if (subProductId == null || subProductId.isBlank()) {
            return ResponseEntity.badRequest().body(Collections.singletonList(
                    Map.of("error", "subProductId is required")
            ));
        }
        return operatorsService.getActiveOperatorsBySubProductId(subProductId);
    }

}

