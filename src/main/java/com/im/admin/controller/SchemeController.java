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
@RequestMapping("/api/scheme")
public class SchemeController {

    @Autowired
    private SchemeService schemeService;

    @GetMapping("/hierarchy-list")
    public ResponseEntity<CommonResponse> getAllSchemeHierarchySelections() {
        List<Map<String, Object>> allSchemeHierarchySelections = schemeService.getAllSchemeHierarchySelections();
        return ResponseEntity.ok(new CommonResponse(200, "Success", allSchemeHierarchySelections));
    }
//     return ResponseEntity.ok(new CommonResponse(201, result, null));

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchemeHierarchy(@RequestParam String keyword) {
        return ResponseEntity.ok(schemeService.searchSchemeHierarchy(keyword));
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<Map<String, Object>>> saveScheme(@RequestBody SchemeDTO schemeDTO) {
        Map<String, Object> data = schemeService.saveScheme(schemeDTO);
        return ResponseEntity.ok(CommonResponse.created(data,"Scheme saved successfully."));
    }

    @PutMapping("/update/{schemeId}")
    public ResponseEntity<CommonResponse<Map<String, Object>>> updateScheme(@PathVariable String schemeId, @RequestBody SchemeDTO schemeDTO) {
        Map<String, Object> data = schemeService.updateScheme(schemeId, schemeDTO);
        return ResponseEntity.ok(CommonResponse.success(data,"Scheme updated successfully."));
    }

    @GetMapping("/operators/{productCode}/{subProductCode}")
    public ResponseEntity<OperatorProductResponseDTO> getOperatorListBySubProductCode(@PathVariable("subProductCode") String subProductCode,
                                                               @PathVariable("productCode") String productCode) {
        OperatorProductResponseDTO response = schemeService.getOperatorsAndProduct(productCode, subProductCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{operatorCode}")
    public ResponseEntity<List<SchemeRate>> getSchemeRatesByOperatorCode(@PathVariable String operatorCode) {
        return ResponseEntity.ok(schemeService.getSchemeRatesByOperatorCode(operatorCode));
    }

    @PostMapping("/routing")
    public ResponseEntity<?> vendorRouting(@RequestBody VendorRoutingDto vendorRoutingDto) {
        VendorRouting savedVendorRouting = schemeService.saveVendorRouting(vendorRoutingDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVendorRouting);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<GetAllSchemeResponse>>> getAllSchemes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<GetAllSchemeResponse> schemes = schemeService.getAllSchemes(pageable);
        return ResponseEntity.ok(new CommonResponse<>(200, "Schemes fetched successfully", schemes));
    }

    @GetMapping("/get-by-id/{id}")
    public ResponseEntity<CommonResponse<GetAllSchemeResponse>> getSchemeById(@PathVariable Long id) {
        try {
            GetAllSchemeResponse responseDto = schemeService.getSchemeDtoById(id);
            return ResponseEntity.ok(CommonResponse.success(responseDto, "Scheme fetched successfully"));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.failure(e.getMessage()));
        }
    }

    @PutMapping("/{schemeCode}/status")
    public ResponseEntity<CommonResponse<String>> updateSchemeStatus(
            @PathVariable String schemeCode,
            @RequestParam boolean isActive
    ) {
        schemeService.updateSchemeStatus(schemeCode, isActive);
        return ResponseEntity.ok(CommonResponse.success(
                null,
                "Scheme status updated successfully to " + (isActive ? "ACTIVE" : "INACTIVE")
        ));
    }


}
