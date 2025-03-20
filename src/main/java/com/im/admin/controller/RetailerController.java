package com.im.admin.controller;

import com.im.admin.entities.Retailer;
import com.im.admin.service.RetailerService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/retailers")
public class RetailerController {

    @Autowired
    private RetailerService retailerService;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping
    public ResponseEntity<Retailer> createRetailer(@RequestBody Retailer retailer) {
        log.info("Received request to create retailer: {}", retailer.getName());
        phoneNumberValidationService.validatePhoneNumber(retailer.getMobile());
        return ResponseEntity.ok(retailerService.createRetailer(retailer));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Retailer> updateRetailer(@PathVariable String id, @RequestBody Retailer retailer) {
        log.info("Received request to update retailer with ID: {}", id);
        return ResponseEntity.ok(retailerService.updateRetailer(id, retailer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRetailer(@PathVariable String id) {
        log.info("Received request to delete retailer with ID: {}", id);
        retailerService.deleteRetailer(id);
        return ResponseEntity.ok("Retailer deleted successfully");
    }

    @GetMapping
    public ResponseEntity<List<Retailer>> getAllRetailers() {
        log.info("Received request to fetch all retailers");
        return ResponseEntity.ok(retailerService.getAllRetailers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Retailer> getRetailerById(@PathVariable String id) {
        log.info("Received request to fetch retailer by ID: {}", id);
        Optional<Retailer> retailer = retailerService.getRetailerById(id);
        return retailer.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}


