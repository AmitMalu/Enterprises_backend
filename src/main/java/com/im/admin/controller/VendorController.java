package com.im.admin.controller;

import org.springframework.data.domain.Page;
import com.im.admin.entities.Vendor;
import com.im.admin.service.VendorService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Optional;

@RestController
@RequestMapping("/api/vendor")
@Slf4j
public class VendorController {

    @Autowired
    private VendorService vendorService;

    @PostMapping
    public ResponseEntity<Vendor> createVendor(@Valid @RequestBody Vendor vendor) {
        log.info("Received request to create vendor: {}", vendor.getApiVendorName());
        Vendor savedVendor = vendorService.saveVendor(vendor);
        log.info("Vendor created successfully with ID: {}", savedVendor.getId());
        return ResponseEntity.ok(savedVendor);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vendor> updateVendor(@PathVariable Long id, @Valid @RequestBody Vendor vendor) {
        log.info("Received request to update vendor with ID: {}", id);
        Vendor updatedVendor = vendorService.updateVendor(id, vendor);
        log.info("Vendor updated successfully with ID: {}", updatedVendor.getId());
        return ResponseEntity.ok(updatedVendor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        log.info("Received request to delete vendor with ID: {}", id);
        vendorService.deleteVendor(id);
        log.info("Vendor with ID {} deleted successfully", id);
        return ResponseEntity.ok("Vendor deleted successfully");
    }


    @GetMapping("/{id}")
    public ResponseEntity<Optional<Vendor>> getVendorById(@PathVariable Long id) {
        log.info("Received request to fetch vendor with ID: {}", id);
        Optional<Vendor> vendor = vendorService.getVendorById(id);
        if (vendor.isPresent()) {
            log.info("Vendor found with ID: {}", id);
        } else {
            log.warn("Vendor with ID {} not found", id);
        }
        return ResponseEntity.ok(vendor);
    }


    @GetMapping
    public ResponseEntity<Page<Vendor>> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to fetch all vendors with page: {} and size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);

        Page<Vendor> vendors = vendorService.getAllVendors(page, size);
        log.info("Fetched {} vendors", vendors.getTotalElements());
        return ResponseEntity.ok(vendors);
    }
}
