package com.scs.admin.controller;

import com.scs.admin.dto.SchemeAllocationRequestDto;
import com.scs.admin.entities.SchemeAllocation;
import com.scs.admin.request.SchemeAllocationRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.RetailerSchemeAllocationResponse;
import com.scs.admin.response.SchemeDropdownListBySubProductResponse;
import com.scs.admin.response.UserSearchResponseDto;
import com.scs.admin.service.SchemeAllocationService;
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

    @GetMapping({
            "/active-hierarchies",
            "/active-retailers"
    })
    public ResponseEntity<CommonResponse<Page<UserSearchResponseDto>>> getActiveHierarchies(

            @RequestParam(required = false) String mobileNumber,

            @RequestParam(required = false) String hierarchyId,

            @RequestParam(required = false) String hierarchyCode,

            @RequestParam(required = false) String createdDate,

            @RequestParam(required = false) String lastLoginDate,

            @RequestParam(required = false) String emailId,

            @RequestParam(required = false, defaultValue = "") String keyword,

            @RequestParam(defaultValue = "0") int page,

            @RequestParam(defaultValue = "10") int size
    ) {

        log.info(
                "[ACTIVE HIERARCHY CONTROLLER] Search request. " +
                        "keyword={}, hierarchyCode={}, page={}, size={}",
                keyword,
                hierarchyCode,
                page,
                size
        );

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Direction.DESC, "createdDate")
        );

        Page<UserSearchResponseDto> result =
                allocationService.searchActiveHierarchies(
                        mobileNumber,
                        hierarchyId,
                        hierarchyCode,
                        createdDate,
                        lastLoginDate,
                        emailId,
                        keyword,
                        pageable
                );

        String message = result.isEmpty()
                ? "No active hierarchy users found."
                : "Active hierarchy users fetched successfully.";

        return ResponseEntity.ok(
                CommonResponse.success(result, message)
        );
    }

}
