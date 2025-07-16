package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.VendorActiveResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) List<String> id,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String vendorCode,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String subProductId,
            @RequestParam(required = false) String subProductCode,
            @RequestParam(required = false) String modesSupported,
            @RequestParam(required = false) Integer priority,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String bankType,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String logo,
            @RequestParam(required = false) String parameter,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Vendors> result = vendorsService.getAllVendors(pageable, id, vendorName, vendorCode, productId, productCode,
                subProductId, subProductCode, modesSupported, priority, isActive, bankType, website, logo, parameter,
                createdBy, editedBy, createdOn, editedOn);

        return ResponseEntity.ok(
                new CommonResponse<>(200, "Vendors fetched successfully", result)
        );
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

    @GetMapping("/dropdown/by-product/{productId}")
    public ResponseEntity<CommonResponse<List<VendorDropdownResponseDto>>> getActiveVendorsByProduct(
            @PathVariable("productId") String productId) {

        log.info("Fetching active vendors for productId: {}", productId);

        List<VendorDropdownResponseDto> vendorList = vendorsService.getActiveVendorByProductId(productId);

        if (vendorList.isEmpty()) {
            log.warn("No active vendors found for productId: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResponse<>(404, "No active vendors found for the given productId.", null));
        }

        log.info("Successfully retrieved {} vendors for productId: {}", vendorList.size(), productId);
        return ResponseEntity.ok(new CommonResponse<>(200, "Active vendors fetched successfully", vendorList));
    }


    @GetMapping("/active")
    public ResponseEntity<CommonResponse<List<VendorActiveResponseDto>>> getActiveVendorsOnly() {
        List<VendorActiveResponseDto> result = vendorsService.getAllActiveVendors();

        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new CommonResponse<>(404, "No active vendors found", null));
        }

        return ResponseEntity.ok(new CommonResponse<>(200, "Active vendors fetched successfully", result));
    }

}

