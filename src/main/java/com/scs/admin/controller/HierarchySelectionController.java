package com.scs.admin.controller;

import com.scs.admin.entities.SchemeHierarchySelection;
import com.scs.admin.exception.DuplicateEntryException;
import com.scs.admin.request.SchemeHierarchySelectionRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.SchemeHierarchySelectionResponseDTO;
import com.scs.admin.service.HierarchySelectionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/hierarchy")
@Slf4j
public class HierarchySelectionController {

    @Autowired
    private HierarchySelectionService hierarchyService;

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<Map<String, Object>>> saveHierarchySelection(@RequestBody SchemeHierarchySelectionRequest request) {
        log.info("API Call: saveHierarchySelection with request: {}", request);
        try {
            Map<String, Object> data = hierarchyService.saveHierarchySelection(request);
            log.info("Hierarchy selection saved successfully");
            return ResponseEntity.ok(CommonResponse.created(data, "Hierarchy selection saved successfully."));
        } catch (DuplicateEntryException e) {
            log.error("Duplicate entry error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.failure(e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving hierarchy selection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.failure(e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> updateHierarchySelection(
            @PathVariable String id,
            @RequestBody SchemeHierarchySelectionRequest requestDTO) {
        log.info("API Call: updateHierarchySelection for id: {} with request: {}", id, requestDTO);
        try {
            Map<String, Object> response = hierarchyService.updateHierarchySelection(id, requestDTO);
            log.info("Hierarchy selection updated successfully for id: {}", id);
            return ResponseEntity.ok(CommonResponse.success(response, "Hierarchy selection updated successfully."));
        } catch (DuplicateEntryException e) {
            log.error("Duplicate entry error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(CommonResponse.error(409, e.getMessage()));
        } catch (EntityNotFoundException e) {
            log.error("Entity not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(404, e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating hierarchy selection: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonResponse.error(400, e.getMessage()));
        }
    }

    @GetMapping("/getAll")
    public ResponseEntity<CommonResponse<Page<SchemeHierarchySelectionResponseDTO>>> getAllSelections(
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String schemeHierarchySelectionName,
            @RequestParam(required = false) String schemeHierarchyCombination,
            @RequestParam(required = false) String combinationHashCode,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String createdOn,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) String editedOn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("API Call: getAllSelections with filters. Page: {}, Size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));

        Page<SchemeHierarchySelectionResponseDTO> pageData = hierarchyService.getAllSelections(
                id, schemeHierarchySelectionName, schemeHierarchyCombination,
                combinationHashCode, createdBy, createdOn, editedBy, editedOn, pageable
        );

        log.info("Fetched {} hierarchy selections", pageData.getNumberOfElements());

        return ResponseEntity.ok(CommonResponse.success(pageData, "Selections fetched successfully."));
    }


    @GetMapping("/getById/{id}")
    public ResponseEntity<CommonResponse<SchemeHierarchySelectionResponseDTO>> getById(@PathVariable String id) {
        log.info("API Call: getById for id: {}", id);
        SchemeHierarchySelectionResponseDTO dto = hierarchyService.getSelectionById(id);
        log.info("Hierarchy selection fetched successfully for id: {}", id);
        return ResponseEntity.ok(CommonResponse.success(dto, "Hierarchy selection fetched successfully."));
    }


}

