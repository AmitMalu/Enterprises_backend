package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.VendorActiveResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.dto.VendorsDTO;
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
    public ResponseEntity<CommonResponse<Vendors>> createVendor(@RequestBody @Valid VendorsDTO dto) {
        log.info("Received request to create vendor: {}", dto);
        Vendors savedVendor = vendorsService.saveVendor(dto);
        log.debug("Vendor created successfully: {}", savedVendor);
        return ResponseEntity.ok(CommonResponse.success(savedVendor, "Vendor saved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorsDTO>> getVendorById(@PathVariable String id) {
        log.info("Fetching vendor by ID: {}", id);
        VendorsDTO dto = vendorsService.getVendorById(id);
        log.debug("Vendor fetched successfully: {}", dto);
        return ResponseEntity.ok(CommonResponse.success(dto, "Vendor fetched successfully"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<VendorsDTO>>> getAllVendors(
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
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String bankType,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String logo,
            @RequestParam(required = false) String parameter,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {
        log.info("Fetching vendors with filters - page: {}, size: {}, vendorName: {}, vendorCode: {}, productId: {}, subProductId: {}, isActive: {}",
                page, size, vendorName, vendorCode, productId, subProductId, active);

        Pageable pageable = PageRequest.of(page, size);
        Page<VendorsDTO> dtoPage = vendorsService.getAllVendors(
                pageable, id, vendorName, vendorCode, productId, productCode,
                subProductId, subProductCode, modesSupported, active,
                bankType, website, logo, parameter, createdBy, editedBy, createdOn, editedOn
        );

        log.debug("Fetched {} vendors", dtoPage.getTotalElements());
        return ResponseEntity.ok(CommonResponse.success(dtoPage, "Vendors fetched successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Vendors>> updateVendor(
            @PathVariable String id,
            @RequestBody @Valid VendorsDTO vendorDto) {

        log.info("Updating vendor with ID: {}", id);
        log.debug("Update payload: {}", vendorDto);

        Vendors updatedVendor = vendorsService.updateVendor(id, vendorDto);
        log.debug("Vendor updated: {}", updatedVendor);

        return ResponseEntity.ok(CommonResponse.success(updatedVendor, "Vendor updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable String id) {
        log.info("Deleting vendor with ID: {}", id);
        vendorsService.deleteVendor(id);
        log.info("Vendor deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dropdown/by-product/{productId}")
    public ResponseEntity<CommonResponse<List<VendorDropdownResponseDto>>> getActiveVendorsByProduct(
            @PathVariable("productId") Short productId) {

        log.info("Fetching active vendors for productId: {}", productId);

        List<VendorDropdownResponseDto> vendorList = vendorsService.getActiveVendorByProductId(productId);

        if (vendorList.isEmpty()) {
            log.warn("No active vendors found for productId: {}", productId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(404, "No active vendors found for the given productId."));
        }

        log.debug("Fetched {} active vendors for productId: {}", vendorList.size(), productId);
        return ResponseEntity.ok(CommonResponse.success(vendorList, "Active vendors fetched successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<CommonResponse<List<VendorActiveResponseDto>>> getActiveVendorsOnly() {
        log.info("Fetching all active vendors");

        List<VendorActiveResponseDto> result = vendorsService.getAllActiveVendors();

        if (result.isEmpty()) {
            log.warn("No active vendors found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.error(404, "No active vendors found"));
        }

        log.debug("Fetched {} active vendors", result.size());
        return ResponseEntity.ok(CommonResponse.success(result, "Active vendors fetched successfully"));
    }

}

