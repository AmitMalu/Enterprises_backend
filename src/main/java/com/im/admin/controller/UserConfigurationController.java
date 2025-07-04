package com.im.admin.controller;

import com.im.admin.dto.UserConfigurationRequestDto;
import com.im.admin.dto.UserConfigurationResponseDto;
import com.im.admin.entities.UserConfiguration;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-config")
public class UserConfigurationController {
    @Autowired
    private UserConfigurationService userConfigurationService;

    @PostMapping
    public ResponseEntity<CommonResponse> addUserConfig(@RequestBody UserConfigurationRequestDto dto) {
        List<UserConfiguration> userConfigurations = userConfigurationService.addUserConfiguration(dto);
        return ResponseEntity.ok(new CommonResponse(201, "userConfigurations created successfully", userConfigurations));
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse> getUserConfigById(@PathVariable Integer id) {
        UserConfiguration config = userConfigurationService.getUserConfigurationById(id);
        return ResponseEntity.ok(new CommonResponse(200, "Fetched successfully", config));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<UserConfiguration>> updateUserConfigById(
            @PathVariable Integer id,
            @RequestBody UserConfigurationRequestDto dto) {

        UserConfiguration updatedConfig = userConfigurationService.updateUserConfiguration(id, dto);
        return ResponseEntity.ok(new CommonResponse<>(200, "User configuration updated successfully", updatedConfig));
    }


    @GetMapping
    public ResponseEntity<CommonResponse<Page<UserConfiguration>>> getAllUserConfigurations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String parentAccount,
            @RequestParam(required = false) Integer menuId,
            @RequestParam(required = false) Integer subMenuId
    ) {
        Page<UserConfiguration> result = userConfigurationService.getAllUserConfigurations(
                page, size, userId, fullName, email, mobile, state, userRole, parentAccount, menuId, subMenuId
        );
        return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
    }





}
