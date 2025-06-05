package com.im.admin.controller;

import com.im.admin.entities.Admin;
import com.im.admin.entities.Enterprises;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AdminService;
import com.im.admin.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

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

        Admin admin = JsonUtil.parseJson(adminJson, Admin.class);

        Admin savedAdmin = adminService.saveAdmin(admin);

        CommonResponse<Admin> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin created successfully");
        response.setData(savedAdmin);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an Admin.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Admin> updateAdmin(
            @PathVariable String id,
            @RequestBody Admin updatedAdmin) {
        Admin admin = adminService.updateAdmin(id, updatedAdmin);
        return ResponseEntity.ok(admin);
    }

    /**
     * Get all Admins with pagination.
     */
    @GetMapping()
    public ResponseEntity<List<Admin>> getAllAdmins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Admin> admins = adminService.getAllAdmins(page, size);
        return ResponseEntity.ok(admins);
    }

    /**
     * Get Admin by ID.
     */
    @GetMapping("/{id}")
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


