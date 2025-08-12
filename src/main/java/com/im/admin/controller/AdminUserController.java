package com.im.admin.controller;

import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.AdminUser;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin-user")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse<AdminUser>> createAgent(@RequestBody  AdminUserRequestDTO dto) {
        AdminUser savedAdminUser = adminUserService.createAdminUser(dto);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Admin user created successfully");
        response.setData(savedAdminUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<AdminUser>>> getAllAdminUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<AdminUser> adminUsers = adminUserService.getAllAdminUsers(page, size);

        CommonResponse<Page<AdminUser>> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin users fetched successfully");
        response.setData(adminUsers);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON) // inbuilt content type
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<AdminUser>> updateAdminUser(
            @PathVariable Long id,
            @RequestBody AdminUserRequestDTO dto) {

        AdminUser updatedAdminUser = adminUserService.updateAdminUser(id, dto);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin user updated successfully");
        response.setData(updatedAdminUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<AdminUser>> getAdminUserById(@PathVariable Long id) {

        AdminUser adminUser = adminUserService.getAdminUserById(id);

        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Admin user fetched successfully");
        response.setData(adminUser);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }



}
