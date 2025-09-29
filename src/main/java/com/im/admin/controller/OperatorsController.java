package com.im.admin.controller;

import com.im.admin.dto.OperatorDropdownResponseDTO;
import com.im.admin.entities.Operators;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.OperatorsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
        log.info("Operator created successfully with ID: {}", savedOperator.getId());
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
        log.info("Operator updated successfully with ID: {}", id);
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
        log.info("Operator deleted successfully with ID: {}", id);
        return ResponseEntity.ok("Operator deleted successfully.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Operators>> getOperatorById(@PathVariable String id) {
        log.info("Received request to fetch operator with ID: {}", id);
        Operators operator = operatorsService.getOperatorById(id);
        log.info("Fetched operator successfully with ID: {}", id);
        return ResponseEntity.ok(
                new CommonResponse<>(HttpStatus.OK.value(), "Operator fetched successfully", operator)
        );
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<Operators>>> getAllOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> id,
            @RequestParam(required = false) String operatorCategory,
            @RequestParam(required = false) String operatorCode,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String operatorAlias,
            @RequestParam(required = false) String alias,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String subProductCode,
            @RequestParam(required = false) String subProductId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        log.info("Fetching operators with filters - page: {}, size: {}, operatorCategory: {}, operatorCode: {}",
                page, size, operatorCategory, operatorCode);
        Pageable pageable = PageRequest.of(page, size);

        Page<Operators> operatorsPage = operatorsService.getAllOperators(
                pageable, id, operatorCategory, operatorCode, operatorName,
                operatorAlias, alias, productCode, productId, subProductCode,
                subProductId, isActive, createdBy, editedBy, createdOn, editedOn
        );

        log.info("Fetched {} operators", operatorsPage.getTotalElements());
        CommonResponse<Page<Operators>> response = new CommonResponse<>(
                200, "Operators fetched successfully", operatorsPage
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/active/{subProductId}")
    public ResponseEntity<CommonResponse<List<OperatorDropdownResponseDTO>>> getActiveOperators(
            @PathVariable String subProductId) {

        log.info("Fetching active operators for subProductId: {}", subProductId);
        if (subProductId == null || subProductId.isBlank()) {
            log.warn("subProductId is missing or blank");
            return ResponseEntity.badRequest().body(
                    new CommonResponse<>(400, "subProductId is required", null)
            );
        }

        List<OperatorDropdownResponseDTO> operators = operatorsService.getActiveOperatorsBySubProductId(subProductId);

        if (operators.isEmpty()) {
            log.warn("No active operators found for subProductId: {}", subProductId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new CommonResponse<>(404, "No active operators found for subProductId: " + subProductId, null)
            );
        }

        log.info("Fetched {} active operators for subProductId: {}", operators.size(), subProductId);
        return ResponseEntity.ok(
                new CommonResponse<>(200, "Active operators fetched successfully", operators)
        );
    }

    @GetMapping("/active")
    public ResponseEntity<CommonResponse<List<OperatorDropdownResponseDTO>>> getActiveOperators() {
        log.info("Fetching all active operators");
        List<OperatorDropdownResponseDTO> operators = operatorsService.getActiveOperators();
        log.info("Fetched {} active operators", operators.size());
        CommonResponse<List<OperatorDropdownResponseDTO>> response = new CommonResponse<>(
                200,
                "Active operators fetched successfully",
                operators
        );
        return ResponseEntity.ok(response);
    }
}

