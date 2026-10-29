package com.scs.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.scs.admin.entities.Admin;
import com.scs.admin.entities.Enterprises;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AdminService;
import com.scs.admin.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admins")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * Save a new Admin.
     */
    @PostMapping("/save")
    public ResponseEntity<CommonResponse<Admin>> createAdmin(@RequestPart("data") String adminJson) throws IOException {
        log.info("Request received to create Admin with data: {}", adminJson);

        Admin admin = JsonUtil.parseJson(adminJson, Admin.class);
        log.debug("Parsed Admin object: {}", admin);

        Admin savedAdmin = adminService.saveAdmin(admin);
        log.info("Admin saved successfully with ID: {}", savedAdmin.getId());

        CommonResponse<Admin> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin created successfully");
        response.setData(savedAdmin);

        log.debug("Response for createAdmin: {}", response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an Admin.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Admin> updateAdmin(
            @PathVariable String id,
            @RequestBody Admin updatedAdmin) {
        log.info("Request received to update Admin with ID: {}", id);
        log.debug("Update data: {}", updatedAdmin);

        Admin admin = adminService.updateAdmin(id, updatedAdmin);
        log.info("Admin with ID: {} updated successfully", id);

        return ResponseEntity.ok(admin);
    }

    /**
     * Get all Admins with pagination.
     */
    @GetMapping()
    public ResponseEntity<List<Admin>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) throws JsonProcessingException {
        log.info("Request received to get all Admins with page: {}, size: {}", page, size);

        List<Admin> admins = adminService.getAllAdmins(page, size);
        log.info("Fetched {} Admin(s)", admins.size());

        return ResponseEntity.ok(admins);
    }

    /**
     * Get Admin by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Admin> getAdminById(@PathVariable String id) throws JsonProcessingException {
        log.info("Request received to fetch Admin with ID: {}", id);

        Admin admin = adminService.getAdminById(id);
        log.info("Admin with ID: {} fetched successfully", id);

        return ResponseEntity.ok(admin);
    }

    /**
     * Delete an Admin by ID.
     */
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<String> deleteAdmin(@PathVariable String id) {
        log.info("Request received to delete Admin with ID: {}", id);

        adminService.deleteAdmin(id);
        log.info("Admin with ID: {} deleted successfully", id);

        return ResponseEntity.ok("Admin with ID " + id + " deleted successfully.");
    }
}


