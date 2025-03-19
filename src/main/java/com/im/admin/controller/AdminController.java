package com.im.admin.controller;

import com.im.admin.entities.Admin;
import com.im.admin.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Save a new Admin.
     */
    @PostMapping()
    public ResponseEntity<Admin> createAdmin(@RequestBody Admin admin) {
        Admin savedAdmin = adminService.saveAdmin(admin);
        return ResponseEntity.ok(savedAdmin);
    }

    /**
     * Update an Admin.
     */
    @PutMapping("/admin/{id}")
    public ResponseEntity<Admin> updateAdmin(
            @PathVariable String id,
            @RequestBody Admin updatedAdmin) {
        Admin admin = adminService.updateAdmin(id, updatedAdmin);
        return ResponseEntity.ok(admin);
    }

    /**
     * Get all Admins with pagination.
     */
    @GetMapping("/admins")
    public ResponseEntity<List<Admin>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Admin> admins = adminService.getAllAdmins(page, size);
        return ResponseEntity.ok(admins);
    }

    /**
     * Get Admin by ID.
     */
    @GetMapping("/admin/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable String id) {
        Admin admin = adminService.getAdminById(id);
        return ResponseEntity.ok(admin);
    }

    /**
     * Delete an Admin by ID.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteAdmin(@PathVariable String id) {
        adminService.deleteAdmin(id);
        return ResponseEntity.ok("Admin with ID " + id + " deleted successfully.");
    }
}


