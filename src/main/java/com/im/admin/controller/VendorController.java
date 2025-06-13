package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@Slf4j
@Validated
public class VendorController {

    @Autowired
    private VendorService vendorsService;

    @PostMapping
    public ResponseEntity<CommonResponse<Vendors>> createVendor(@RequestBody @Valid Vendors vendor) {
        Vendors savedVendor = vendorsService.saveVendor(vendor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CommonResponse<>(201, "Vendor created successfully", savedVendor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<Vendors>> getVendorById(@PathVariable String id) {
        Vendors vendor = vendorsService.getVendorById(id);
        return ResponseEntity.ok(
                new CommonResponse<>(200, "Vendor fetched successfully", vendor));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<Vendors>>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<Vendors> vendors = vendorsService.getAllVendors(page, size);
        return ResponseEntity.ok(
                new CommonResponse<>(200, "Vendors fetched successfully", vendors));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Vendors>> updateVendor(
            @PathVariable String id,
            @RequestBody @Valid Vendors vendor) {

        Vendors updatedVendor = vendorsService.updateVendor(id, vendor);
        return ResponseEntity.ok(
                new CommonResponse<>(200, "Vendor updated successfully", updatedVendor));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable String id) {
        vendorsService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dropdown/{subProductId}")
    public ResponseEntity<CommonResponse<List<VendorDropdownResponseDto>>> getActiveVendors(
            @PathVariable("subProductId") String subProductId) {

        log.info("Fetching active vendors for subProductId: {}", subProductId);

        List<VendorDropdownResponseDto> vendorList = vendorsService.getActiveVendorBySubProductId(subProductId);

        if (vendorList.isEmpty()) {
            log.warn("No active vendors found for subProductId: {}", subProductId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResponse<>(404, "No active vendors found for the given subProductId.", null));
        }

        log.info("Successfully retrieved {} vendors for subProductId: {}", vendorList.size(), subProductId);
        return ResponseEntity.ok(new CommonResponse<>(200, "Active vendors fetched successfully", vendorList));
    }

}

