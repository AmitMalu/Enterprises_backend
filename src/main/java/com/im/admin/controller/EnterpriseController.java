package com.im.admin.controller;

import com.im.admin.entities.Enterprises;
import com.im.admin.service.EnterprisesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/enterprises")
@Slf4j
public class EnterpriseController {

    @Autowired
    private EnterprisesService enterpriseService;

    @PostMapping
    public ResponseEntity<Enterprises> createEnterprise(@RequestBody Enterprises enterprise) {
        log.info("Received request to create an enterprise: {}", enterprise.getCompanyName());
        Enterprises savedEnterprise = enterpriseService.saveEnterprise(enterprise);
        log.info("Enterprise created successfully with ID: {}", savedEnterprise.getId());
        return ResponseEntity.ok(savedEnterprise);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Enterprises> updateEnterprise(@PathVariable Long id, @RequestBody Enterprises enterprise) {
        log.info("Received request to update enterprise with ID: {}", id);
        Enterprises updatedEnterprise = enterpriseService.updateEnterprise(id, enterprise);
        log.info("Enterprise updated successfully with ID: {}", updatedEnterprise.getId());
        return ResponseEntity.ok(updatedEnterprise);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEnterprise(@PathVariable Long id) {
        log.info("Received request to delete enterprise with ID: {}", id);
        enterpriseService.deleteEnterprise(id);
        log.info("Enterprise with ID {} deleted successfully", id);
        return ResponseEntity.ok("Enterprise deleted successfully");
    }

    @GetMapping
    public ResponseEntity<Page<Enterprises>> getAllEnterprises(Pageable pageable) {
        log.info("Received request to fetch all enterprises with pagination");
        Page<Enterprises> enterprises = enterpriseService.getAllEnterprises(pageable);
        log.info("Fetched {} enterprises", enterprises.getTotalElements());
        return ResponseEntity.ok(enterprises);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<Enterprises>> getEnterpriseById(@PathVariable Long id) {
        log.info("Received request to fetch enterprise with ID: {}", id);
        Optional<Enterprises> enterprise = enterpriseService.getEnterpriseById(id);
        if (enterprise.isPresent()) {
            log.info("Enterprise found with ID: {}", id);
        } else {
            log.warn("Enterprise with ID {} not found", id);
        }
        return ResponseEntity.ok(enterprise);
    }
}

