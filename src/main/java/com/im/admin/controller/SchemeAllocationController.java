package com.im.admin.controller;

import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;
import com.im.admin.service.SchemeAllocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/scheme-allocation")
public class SchemeAllocationController {


    @Autowired
    private SchemeAllocationService allocationService;

    @PostMapping("/assign-or-update")
    public ResponseEntity<CommonResponse<List<SchemeAllocation>>> assignOrUpdateSchemes(
            @RequestBody SchemeAllocationRequestDto request) {

        List<SchemeAllocation> updatedSchemes = allocationService.assignOrUpdateSchemes(request);

        return ResponseEntity.ok(CommonResponse.success(updatedSchemes, "Scheme allocation processed successfully"));
    }

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<String>> saveAllocation(
            @Valid @RequestBody SchemeAllocationRequest request) {
        CommonResponse<String> response = allocationService.saveSchemeAllocation(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }


    @PostMapping("/scheme-dropdown/{subProductName}")
    public ResponseEntity<CommonResponse<List<SchemeDropdownListBySubProductResponse>>> getDropdownList(
            @PathVariable("subProductName") String subProductName) {

        List<SchemeDropdownListBySubProductResponse> responses =
                allocationService.getSchemeDropdownListBySubProduct(subProductName);

        if (responses.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success(responses, "No schemes found for sub-product: " + subProductName));
        }

        return ResponseEntity.ok(CommonResponse.success(responses, "Schemes loaded successfully"));
    }

    @GetMapping("/retailer-scheme-allocation/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<RetailerSchemeAllocationResponse>>> getRetailerSchemeAllocation(
            @PathVariable String hierarchyId) {

        List<RetailerSchemeAllocationResponse> result =
                allocationService.getAllocatedSchemesForRetailer(hierarchyId);

        return ResponseEntity.ok(CommonResponse.success(result, "Scheme allocation data loaded successfully"));
    }


}
