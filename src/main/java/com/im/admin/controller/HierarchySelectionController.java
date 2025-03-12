package com.im.admin.controller;

import com.im.admin.entities.SchemeHierarchySelection;
import com.im.admin.exception.DuplicateEntryException;
import com.im.admin.service.HierarchySelectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hierarchy")
@Slf4j
public class HierarchySelectionController {

    @Autowired
    private HierarchySelectionService hierarchyService;

    @PostMapping("/save")
    public ResponseEntity<Object> saveHierarchySelection(@RequestBody List<String> hierarchyNames) {
        try {
            Map<String, Object> response = hierarchyService.saveHierarchySelection(hierarchyNames);
            return ResponseEntity.ok(response);
        } catch (DuplicateEntryException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<String> updateHierarchySelection(@PathVariable Long id, @RequestBody List<String> hierarchyNames) {
        try {
            hierarchyService.updateHierarchySelection(id, hierarchyNames);
            return ResponseEntity.ok("Hierarchy selection updated successfully.");
        } catch (DuplicateEntryException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<SchemeHierarchySelection>> getAllSelections(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(hierarchyService.getAllSelections(pageable));
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<?> getSelectionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(hierarchyService.getSelectionById(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}

