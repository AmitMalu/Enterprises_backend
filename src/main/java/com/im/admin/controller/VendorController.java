package com.im.admin.controller;

import com.im.admin.dto.SubProductDropdownResponseDto;
import com.im.admin.dto.VendorDropdownResponseDto;
import com.im.admin.entities.vendor.Vendors;
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
    public ResponseEntity<Vendors> createVendor(@RequestBody @Valid Vendors vendor) {
        Vendors savedVendor = vendorsService.saveVendor(vendor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVendor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendors> getVendorById(@PathVariable String id) {
        Vendors vendor = vendorsService.getVendorById(id);
        return ResponseEntity.ok(vendor);
    }

    @GetMapping
    public ResponseEntity<Page<Vendors>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Vendors> vendors = vendorsService.getAllVendors(page, size);
        return ResponseEntity.ok(vendors);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendors> updateVendor(@PathVariable String id, @RequestBody @Valid Vendors vendor) {
        Vendors updatedVendor = vendorsService.updateVendor(id, vendor);
        return ResponseEntity.ok(updatedVendor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable String id) {
        vendorsService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dropdown/{subProductId}")
    public ResponseEntity<?> getActiveVendors(@PathVariable("subProductId") String subProductId) {
        log.info("Fetching active vendors for subProductId: {}", subProductId);

        List<VendorDropdownResponseDto> vendorList = vendorsService.getActiveVendorBySubProductId(subProductId);

        if (vendorList.isEmpty()) {
            log.warn("No active vendors found for subProductId: {}", subProductId);
            return ResponseEntity.status(404).body("No active vendors found for the given subProductId.");
        }

        log.info("Successfully retrieved {} vendors for subProductId: {}", vendorList.size(), subProductId);
        return ResponseEntity.ok(vendorList);
    }
}

