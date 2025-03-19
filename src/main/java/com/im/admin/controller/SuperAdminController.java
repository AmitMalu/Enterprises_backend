package com.im.admin.controller;

import com.im.admin.entities.SuperAdmin;
import com.im.admin.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

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

    /**
     * Save a new SuperAdmin.
     */
    @PostMapping()
    public ResponseEntity<SuperAdmin> createSuperAdmin(@Valid @RequestBody SuperAdmin superAdmin) {
        SuperAdmin savedSuperAdmin = superAdminService.saveSuperAdmin(superAdmin);
        return ResponseEntity.ok(savedSuperAdmin);
    }

    /**
     * Update a SuperAdmin.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SuperAdmin> updateSuperAdmin(
            @PathVariable String id,
            @Valid @RequestBody SuperAdmin updatedSuperAdmin) {
        SuperAdmin superAdmin = superAdminService.updateSuperAdmin(id, updatedSuperAdmin);
        return ResponseEntity.ok(superAdmin);
    }

    /**
     * Get all SuperAdmins with pagination.
     */
    @GetMapping()
    public ResponseEntity<List<SuperAdmin>> getAllSuperAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<SuperAdmin> superAdmins = superAdminService.getAllSuperAdmins(page, size);
        return ResponseEntity.ok(superAdmins);
    }

    /**
     * Get SuperAdmin by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SuperAdmin> getSuperAdminById(@PathVariable String id) {
        SuperAdmin superAdmin = superAdminService.getSuperAdminById(id);
        return ResponseEntity.ok(superAdmin);
    }

    /**
     * Delete a SuperAdmin by ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSuperAdmin(@PathVariable String id) {
        superAdminService.deleteSuperAdmin(id);
        return ResponseEntity.ok("SuperAdmin with ID " + id + " deleted successfully.");
    }
}
