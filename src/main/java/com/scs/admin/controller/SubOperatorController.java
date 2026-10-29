package com.scs.admin.controller;


import com.scs.admin.entities.SubOperator;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.SubOperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sub-operators")
@RequiredArgsConstructor
public class SubOperatorController {

    @Autowired
    private SubOperatorService subOperatorService;

    @PostMapping
    public ResponseEntity<CommonResponse<SubOperator>> saveSubOperator(@RequestBody SubOperator subOperator) {
        log.info("Received request to create SubOperator: {}", subOperator);

        try {
            SubOperator saved = subOperatorService.saveSubOperator(subOperator);
            log.info("SubOperator created successfully with ID: {}", saved.getId());
            return ResponseEntity.ok(new CommonResponse<>(200, "SubOperator created successfully", saved));
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed while creating SubOperator: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error while creating SubOperator", e);
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<SubOperator>> updateSubOperator(@PathVariable String id,
                                                                         @RequestBody SubOperator subOperator) {
        log.info("Received request to update SubOperator with ID: {}", id);

        try {
            SubOperator updated = subOperatorService.updateSubOperator(id, subOperator);
            log.info("SubOperator updated successfully for ID: {}", id);
            return ResponseEntity.ok(new CommonResponse<>(200, "SubOperator updated successfully", updated));
        } catch (IllegalArgumentException e) {
            log.warn("Validation failed while updating SubOperator ID {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            log.error("Unexpected error while updating SubOperator ID {}", id, e);
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubOperator(@PathVariable String id) {
        log.info("Received request to delete SubOperator with ID: {}", id);

        try {
            subOperatorService.deleteSubOperator(id);
            log.info("SubOperator deleted successfully with ID: {}", id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting SubOperator with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<SubOperator>>> getAllSubOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> id,
            @RequestParam(required = false) String subOperatorName,
            @RequestParam(required = false) String operatorCode,
            @RequestParam(required = false) String operatorAlias,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        log.info("Fetching SubOperators with filters - page: {}, size: {}, subOperatorName: {}, operatorCode: {}",
                page, size, subOperatorName, operatorCode);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));
            Page<SubOperator> result = subOperatorService.getAllSubOperators(
                    pageable, id, subOperatorName, operatorCode, operatorAlias, category,operatorName,
                    isBlocked, createdBy, editedBy, createdOn, editedOn
            );
            log.info("Fetched {} SubOperators", result.getTotalElements());
            return ResponseEntity.ok(new CommonResponse<>(200, "SubOperators fetched successfully", result));
        } catch (Exception e) {
            log.error("Error fetching SubOperators", e);
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<SubOperator>> getSubOperatorById(@PathVariable String id) {
        log.info("Fetching SubOperator by ID: {}", id);

        try {
            Optional<SubOperator> subOperatorById = subOperatorService.getSubOperatorById(id);

            if (subOperatorById.isPresent()) {
                log.info("SubOperator found for ID: {}", id);
                return ResponseEntity.ok(new CommonResponse<>(200, "Success", subOperatorById.get()));
            } else {
                log.warn("SubOperator not found for ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new CommonResponse<>(404, "SubOperator not found with id: " + id, null));
            }
        } catch (Exception e) {
            log.error("Error fetching SubOperator with ID: {}", id, e);
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

}
