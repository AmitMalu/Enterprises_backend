package com.im.admin.controller;

import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.SchemeAllocationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@Slf4j
@RequestMapping("/api/scheme-allocation")
public class SchemeAllocationController {


    @Autowired
    private SchemeAllocationService allocationService;

    @PostMapping("/assign-or-update")
    public ResponseEntity<CommonResponse<List<SchemeAllocation>>> assignOrUpdateSchemes(
            @RequestBody SchemeAllocationRequestDto request) {

        log.info("Received request to assign or update schemes: {}", request);

        List<SchemeAllocation> updatedSchemes = allocationService.assignOrUpdateSchemes(request);

        log.info("Scheme allocation processed successfully. Total updated schemes: {}", updatedSchemes.size());

        return ResponseEntity.ok(CommonResponse.success(updatedSchemes, "Scheme allocation processed successfully"));
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<String>> saveAllocation(
            @Valid @RequestBody SchemeAllocationRequest request) {

        log.info("Received request to save scheme allocation: {}", request);

        CommonResponse<String> response = allocationService.saveSchemeAllocation(request);

        log.info("Scheme allocation save response: {}", response);

        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/scheme-dropdown/{subProductName}")
    public ResponseEntity<CommonResponse<List<SchemeDropdownListBySubProductResponse>>> getDropdownList(
            @PathVariable("subProductName") String subProductName) {

        log.info("Fetching scheme dropdown list for sub-product: {}", subProductName);

        List<SchemeDropdownListBySubProductResponse> responses =
                allocationService.getSchemeDropdownListBySubProduct(subProductName);

        if (responses.isEmpty()) {
            log.warn("No schemes found for sub-product: {}", subProductName);
            return ResponseEntity.ok(CommonResponse.success(responses, "No schemes found for sub-product: " + subProductName));
        }

        log.info("Schemes loaded successfully for sub-product: {}", subProductName);
        return ResponseEntity.ok(CommonResponse.success(responses, "Schemes loaded successfully"));
    }

    @GetMapping("/retailer-scheme-allocation/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<RetailerSchemeAllocationResponse>>> getRetailerSchemeAllocation(
            @PathVariable String hierarchyId) {

        log.info("Fetching retailer scheme allocation for hierarchy ID: {}", hierarchyId);

        List<RetailerSchemeAllocationResponse> result =
                allocationService.getAllocatedSchemesForRetailer(hierarchyId);

        log.info("Loaded {} scheme allocations for retailer hierarchy ID: {}", result.size(), hierarchyId);

        return ResponseEntity.ok(CommonResponse.success(result, "Scheme allocation data loaded successfully"));
    }

    @GetMapping("/active-retailers")
    public ResponseEntity<CommonResponse<Page<UserSearchResponseDto>>> ActiveRetailers(
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String hierarchyCode,
            @RequestParam(required = false) String createdDate,
            @RequestParam(required = false) String lastLoginDate,
            @RequestParam(required = false) String emailId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        log.info("Searching active retailers with filters, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<UserSearchResponseDto> result = allocationService.searchActiveRetailers(
                mobileNumber, hierarchyId, hierarchyCode, createdDate, lastLoginDate, emailId, pageable, keyword
        );

        if (result.isEmpty()) {
            log.warn("No active retailers found for given filters");
            return ResponseEntity.ok(CommonResponse.success(result, "No active retailers found."));
        }

        log.info("Retailer search successful. Found {} retailers.", result.getTotalElements());

        return ResponseEntity.ok(CommonResponse.success(result, "Retailer search successful"));
    }



}
