package com.im.admin.controller;


import com.im.admin.entities.SubOperator;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SubOperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    private final SubOperatorService subOperatorService;

    @PostMapping
    public ResponseEntity<CommonResponse<SubOperator>> saveSubOperator(@RequestBody SubOperator subOperator) {
        try {
            SubOperator saved = subOperatorService.saveSubOperator(subOperator);
            return ResponseEntity.ok(new CommonResponse<>(200, "SubOperator created successfully", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<SubOperator>> updateSubOperator(@PathVariable String id,
                                                                         @RequestBody SubOperator subOperator) {
        try {
            SubOperator updated = subOperatorService.updateSubOperator(id, subOperator);
            return ResponseEntity.ok(new CommonResponse<>(200, "SubOperator updated successfully", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new CommonResponse<>(400, e.getMessage(), null));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new CommonResponse<>(500, "Internal error", null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubOperator(@PathVariable String id) {
        subOperatorService.deleteSubOperator(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping
    public ResponseEntity<CommonResponse<Page<SubOperator>>> getAllSubOperators(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> id,
            @RequestParam(required = false) String subOperatorName,
            @RequestParam(required = false) String operatorCode,
            @RequestParam(required = false) String operatorAlias,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SubOperator> result = subOperatorService.getAllSubOperators(
                pageable, id, subOperatorName, operatorCode, operatorAlias, operatorName,
                isBlocked, createdBy, editedBy, createdOn, editedOn
        );
        return ResponseEntity.ok(new CommonResponse<>(200, "SubOperators fetched successfully", result));
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<SubOperator>> getSubOperatorById(@PathVariable String id) {
        Optional<SubOperator> subOperatorById = subOperatorService.getSubOperatorById(id);

        if (subOperatorById.isPresent()) {
            return ResponseEntity.ok(new CommonResponse<>(200, "Success", subOperatorById.get()));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResponse<>(404, "SubOperator not found with id: " + id, null));
        }
    }


}

