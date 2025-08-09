package com.im.admin.controller;

import com.im.admin.entities.AdminUser;
import com.im.admin.entities.RetailerAgent;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    @Autowired
    private AdminUserService adminUserService;

    @PostMapping("/create-user")
    public ResponseEntity<CommonResponse<AdminUser>> createAgent(@RequestBody AdminUser adminUser) {
        AdminUser saveAdminUser = adminUserService.createAdminUser(adminUser);


        CommonResponse<AdminUser> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Admin user created successfully");
        response.setData(saveAdminUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


}
