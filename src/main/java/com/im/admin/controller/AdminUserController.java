package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.AdminUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AdminUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/admin-user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse<AdminUser>> createAgent(@RequestBody AdminUserRequestDTO dto) throws JsonProcessingException {
        log.info("Creating admin user with data: {}", dto);

        AdminUser savedAdminUser = adminUserService.createAdminUser(dto);
        log.debug("Created admin user: {}", savedAdminUser);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Admin user created successfully");
        response.setData(savedAdminUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<AdminUser>>> getAllAdminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate dob,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate createdOn,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate lastEditedOn,
            @RequestParam(required = false) String editedBy
    ) throws JsonProcessingException {

        log.info("Fetching admin users with filters - page: {}, size: {}", page, size);

        Page<AdminUser> adminUsers = adminUserService.getAllAdminUsers(
                page, size, id, name, mobileNumber, username,
                emailId, status, dob, createdOn, createdBy, lastEditedOn, editedBy
        );

        CommonResponse<Page<AdminUser>> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin users fetched successfully");
        response.setData(adminUsers);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }


    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<AdminUser>> updateAdminUser(
            @PathVariable String id,
            @RequestBody AdminUserRequestDTO dto) throws JsonProcessingException {

        log.info("Updating admin user with ID: {}", id);
        log.debug("Update data: {}", dto);

        AdminUser updatedAdminUser = adminUserService.updateAdminUser(id, dto);
        log.debug("Updated admin user: {}", updatedAdminUser);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin user updated successfully");
        response.setData(updatedAdminUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<AdminUser>> getAdminUserById(@PathVariable String  id) throws JsonProcessingException {
        log.info("Fetching admin user with ID: {}", id);

        AdminUser adminUser = adminUserService.getAdminUserById(id);
        log.debug("Fetched admin user: {}", adminUser);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin user fetched successfully");
        response.setData(adminUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

}
