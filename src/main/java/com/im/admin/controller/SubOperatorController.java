package com.im.admin.controller;


import com.im.admin.entities.SubOperator;
import com.im.admin.service.SubOperatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/sub-operators")
@RequiredArgsConstructor
public class SubOperatorController {

    private final SubOperatorService subOperatorService;

    @PostMapping
    public ResponseEntity<SubOperator> saveSubOperator(@RequestBody SubOperator subOperator,
                                                       @RequestParam String createdBy) {
        return ResponseEntity.ok(subOperatorService.saveSubOperator(subOperator, createdBy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubOperator> updateSubOperator(@PathVariable String id,
                                                         @RequestBody SubOperator subOperator,
                                                         @RequestParam String editedBy) {
        return ResponseEntity.ok(subOperatorService.updateSubOperator(id, subOperator, editedBy));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubOperator(@PathVariable String id) {
        subOperatorService.deleteSubOperator(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<SubOperator>> getAllSubOperators() {
        return ResponseEntity.ok(subOperatorService.getAllSubOperators());
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<SubOperator>> getAllSubOperatorsPaginated(Pageable pageable) {
        return ResponseEntity.ok(subOperatorService.getAllSubOperatorsPaginated(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<SubOperator>> getSubOperatorById(@PathVariable String id) {
        return ResponseEntity.ok(subOperatorService.getSubOperatorById(id));
    }
}

