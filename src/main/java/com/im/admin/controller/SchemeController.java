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
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
            SchemeFilter filter,
//            @PageableDefault(size = 10)
//            Pageable pageable
            @PageableDefault(
                    size = 10,
                    sort = "createdOn",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        log.info(
                "[GET ALL SCHEMES] Request received | filter={} | page={} | size={}",
                filter,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );

        CommonResponse<Page<GetAllSchemeResponse>> response =
                schemeService.getAllSchemes(
                        filter,
                        pageable
                );

        log.info(
                "[GET ALL SCHEMES] Completed | status={}",
                response.getStatusCode()
        );

        return ResponseEntity.ok(
                response
        );

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
