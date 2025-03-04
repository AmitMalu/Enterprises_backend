package com.im.admin.controller;

import com.im.admin.entities.vendor.Vendors;
import com.im.admin.service.VendorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/vendors")
@Slf4j
@Validated
public class VendorController {

    @Autowired
    private VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    public ResponseEntity<Vendors> createVendor(@Valid @RequestBody Vendors vendor) {
        log.info("Creating a new vendor: {}", vendor);
        Vendors savedVendor = vendorService.saveVendor(vendor);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedVendor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendors> updateVendor(@PathVariable Long id, @Valid @RequestBody Vendors vendor) {
        log.info("Updating vendor with ID: {}", id);
        Vendors updatedVendor = vendorService.updateVendor(id, vendor);
        return ResponseEntity.ok(updatedVendor);
    }

    @GetMapping
    public ResponseEntity<Page<Vendors>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching all vendors with pagination: page {}, size {}", page, size);
        Page<Vendors> vendors = vendorService.getAllVendors(PageRequest.of(page, size));
        return ResponseEntity.ok(vendors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendors> getVendorById(@PathVariable Long id) {
        log.info("Fetching vendor with ID: {}", id);
        Vendors vendor = vendorService.getVendorById(id);
        return ResponseEntity.ok(vendor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        log.info("Deleting vendor with ID: {}", id);
        vendorService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }
}
