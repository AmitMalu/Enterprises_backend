package com.scs.admin.controller;

import com.scs.admin.entities.SuperAdmin;
import com.scs.admin.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/super-admin")
@Validated
public class SuperAdminController {

    @Autowired
    private SuperAdminService superAdminService;


    @PostMapping
    public ResponseEntity<SuperAdmin> createSuperAdmin(@Valid @RequestBody SuperAdmin superAdmin) {
        log.info("Received request to create SuperAdmin: {}", superAdmin);

        try {
            SuperAdmin savedSuperAdmin = superAdminService.saveSuperAdmin(superAdmin);
            log.info("SuperAdmin created successfully with ID: {}", savedSuperAdmin.getId());
            return ResponseEntity.ok(savedSuperAdmin);
        } catch (Exception e) {
            log.error("Error creating SuperAdmin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuperAdmin> updateSuperAdmin(
            @PathVariable String id,
            @Valid @RequestBody SuperAdmin updatedSuperAdmin) {

        log.info("Received request to update SuperAdmin with ID: {}", id);

        try {
            SuperAdmin superAdmin = superAdminService.updateSuperAdmin(id, updatedSuperAdmin);
            log.info("SuperAdmin updated successfully for ID: {}", id);
            return ResponseEntity.ok(superAdmin);
        } catch (Exception e) {
            log.error("Error updating SuperAdmin with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<SuperAdmin>> getAllSuperAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching all SuperAdmins. Page: {}, Size: {}", page, size);

        try {
            List<SuperAdmin> superAdmins = superAdminService.getAllSuperAdmins(page, size);
            log.info("Fetched {} SuperAdmins", superAdmins.size());
            return ResponseEntity.ok(superAdmins);
        } catch (Exception e) {
            log.error("Error fetching SuperAdmins", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperAdmin> getSuperAdminById(@PathVariable String id) {
        log.info("Fetching SuperAdmin by ID: {}", id);

        try {
            SuperAdmin superAdmin = superAdminService.getSuperAdminById(id);
            log.info("Fetched SuperAdmin successfully for ID: {}", id);
            return ResponseEntity.ok(superAdmin);
        } catch (Exception e) {
            log.error("Error fetching SuperAdmin with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSuperAdmin(@PathVariable String id) {
        log.info("Deleting SuperAdmin with ID: {}", id);

        try {
            superAdminService.deleteSuperAdmin(id);
            log.info("SuperAdmin deleted successfully with ID: {}", id);
            return ResponseEntity.ok("SuperAdmin with ID " + id + " deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting SuperAdmin with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete SuperAdmin with ID: " + id);
        }
    }

}
