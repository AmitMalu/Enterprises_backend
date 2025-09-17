package com.im.admin.controller;


import com.im.admin.dto.*;
import com.im.admin.entities.Scheme;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.VendorRouting;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GetAllSchemeResponse;
import com.im.admin.service.SchemeService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/scheme")
public class SchemeController {

    @Autowired
    private SchemeService schemeService;

    @GetMapping("/hierarchy-list")
    public ResponseEntity<CommonResponse> getAllSchemeHierarchySelections() {
        log.info("Fetching all scheme hierarchy selections");
        List<Map<String, Object>> allSchemeHierarchySelections = schemeService.getAllSchemeHierarchySelections();
        log.info("Fetched {} hierarchy selections", allSchemeHierarchySelections.size());
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", allSchemeHierarchySelections));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchemeHierarchy(@RequestParam String keyword) {
        log.info("Searching scheme hierarchy with keyword: {}", keyword);
        List<Map<String, Object>> result = schemeService.searchSchemeHierarchy(keyword);
        log.info("Found {} results for keyword: {}", result.size(), keyword);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<Map<String, Object>>> saveScheme(@RequestBody SchemeDTO schemeDTO) {
        log.info("Saving new scheme: {}", schemeDTO);
        Map<String, Object> data = schemeService.saveScheme(schemeDTO);
        log.info("Scheme saved successfully with data: {}", data);
        return ResponseEntity.ok(CommonResponse.created(data, "Scheme saved successfully."));
    }

    @PutMapping("/update/{schemeId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> updateScheme(@PathVariable String schemeId,
                                                                            @RequestBody SchemeDTO schemeDTO) {
        log.info("Updating scheme with ID: {}", schemeId);
        Map<String, Object> data = schemeService.updateScheme(schemeId, schemeDTO);
        log.info("Scheme updated successfully for ID: {}", schemeId);
        return ResponseEntity.ok(CommonResponse.success(data, "Scheme updated successfully."));
    }

    @GetMapping("/operators/{productCode}/{subProductCode}")
    public ResponseEntity<OperatorProductResponseDTO> getOperatorListBySubProductCode(
            @PathVariable String subProductCode,
            @PathVariable String productCode) {
        log.info("Fetching operators for productCode: {}, subProductCode: {}", productCode, subProductCode);
        OperatorProductResponseDTO response = schemeService.getOperatorsAndProduct(productCode, subProductCode);
        log.info("Operators fetched successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{operatorCode}")
    public ResponseEntity<List<SchemeRate>> getSchemeRatesByOperatorCode(@PathVariable String operatorCode) {
        log.info("Fetching scheme rates for operatorCode: {}", operatorCode);
        List<SchemeRate> rates = schemeService.getSchemeRatesByOperatorCode(operatorCode);
        log.info("Fetched {} scheme rates for operatorCode: {}", rates.size(), operatorCode);
        return ResponseEntity.ok(rates);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<GetAllSchemeResponse>>> getAllSchemes(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String schemeName,
            @RequestParam(required = false) String schemeCode,
            @RequestParam(required = false) String schemeCombinationHashCode,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String subProductId,
            @RequestParam(required = false) String subProductName,
            @RequestParam(required = false) String vendorId,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String schemeHierarchyName,
            @RequestParam(required = false) String schemeHierarchySelectionId,
            @RequestParam(required = false) String createdOn,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedOn,
            @RequestParam(required = false) String editedBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching all schemes with filters: schemeName: {}, schemeCode: {}, schemeCombinationHashCode: {}, productId: {}, productName: {}, subProductId: {}, subProductName: {}, vendorId: {}, vendorName: {}, schemeHierarchyName: {}, schemeHierarchySelectionId: {}, createdOn: {}, createdBy: {}, editedOn: {}, editedBy: {}, page: {}, size: {}",
                schemeName, schemeCode, schemeCombinationHashCode, productId, productName, subProductId, subProductName,
                vendorId, vendorName, schemeHierarchyName, schemeHierarchySelectionId, createdOn, createdBy, editedOn, editedBy,
                page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<GetAllSchemeResponse> schemes = schemeService.getAllSchemes(
                id, schemeName, schemeCode, schemeCombinationHashCode, productId, productName, subProductId,
                subProductName, vendorId, vendorName, schemeHierarchyName, schemeHierarchySelectionId,
                createdOn, createdBy, editedOn, editedBy, pageable
        );

        log.info("Fetched {} schemes", schemes.getTotalElements());
        return ResponseEntity.ok(new CommonResponse<>(200, "Schemes fetched successfully", schemes));
    }


    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<CommonResponse<GetAllSchemeResponse>> getSchemeById(@PathVariable Long id) {
        log.info("Fetching scheme by ID: {}", id);
        try {
            GetAllSchemeResponse responseDto = schemeService.getSchemeDtoById(id);
            log.info("Scheme fetched successfully for ID: {}", id);
            return ResponseEntity.ok(CommonResponse.success(responseDto, "Scheme fetched successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Scheme not found for ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.failure(e.getMessage()));
        }
    }

    @PutMapping("/{schemeCode}/status")
    public ResponseEntity<CommonResponse<String>> updateSchemeStatus(
            @PathVariable String schemeCode,
            @RequestParam boolean isActive) {
        log.info("Updating scheme status for schemeCode: {} to {}", schemeCode, isActive ? "ACTIVE" : "INACTIVE");
        schemeService.updateSchemeStatus(schemeCode, isActive);
        log.info("Scheme status updated successfully for schemeCode: {}", schemeCode);
        return ResponseEntity.ok(CommonResponse.success(
                null,
                "Scheme status updated successfully to " + (isActive ? "ACTIVE" : "INACTIVE")
        ));
    }

}
