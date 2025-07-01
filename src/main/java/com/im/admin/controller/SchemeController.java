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
    public ResponseEntity<List<Map<String, Object>>> getAllSchemeHierarchySelections() {
        return ResponseEntity.ok(schemeService.getAllSchemeHierarchySelections());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchSchemeHierarchy(@RequestParam String keyword) {
        return ResponseEntity.ok(schemeService.searchSchemeHierarchy(keyword));
    }

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveScheme(@RequestBody SchemeDTO schemeDTO) {
        return ResponseEntity.ok(schemeService.saveScheme(schemeDTO));
    }

    @GetMapping("/operators/{productCode}/{subProductCode}")
    public ResponseEntity<OperatorProductResponseDTO> getOperatorListBySubProductCode(@PathVariable("subProductCode") String subProductCode,
                                                               @PathVariable("productCode") String productCode) {
        OperatorProductResponseDTO response = schemeService.getOperatorsAndProduct(productCode, subProductCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rate")
    public ResponseEntity<List<SchemeRate>> createSchemeRates(@RequestBody SchemeRateRequestDTO requestDTO) {
        return ResponseEntity.ok(schemeService.createSchemeRates(requestDTO));
    }

    @GetMapping("/{operatorCode}")
    public ResponseEntity<List<SchemeRate>> getSchemeRatesByOperatorCode(@PathVariable String operatorCode) {
        return ResponseEntity.ok(schemeService.getSchemeRatesByOperatorCode(operatorCode));
    }

    @PostMapping("/allocateSchemes")
    public ResponseEntity<CommonResponse<List<SchemeAllocation>>> allocateSchemes(@RequestBody SchemeAllocationRequestDto request) {
        CommonResponse<List<SchemeAllocation>> response = schemeService.allocateSchemes(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
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


}
