package com.scs.admin.controller;

import com.scs.admin.dto.UserConfigurationRequestDto;
import com.scs.admin.dto.UserConfigurationResponseDto;
import com.scs.admin.entities.UserConfiguration;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.UserConfigurationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/user-config")
public class UserConfigurationController {
    @Autowired
    private UserConfigurationService userConfigurationService;

    @PostMapping
    public ResponseEntity<CommonResponse> addUserConfig(@RequestBody UserConfigurationRequestDto dto) {
        log.info("Received request to create user configuration for userId: {}", dto.getUserId());

        try {
            List<UserConfiguration> userConfigurations = userConfigurationService.addUserConfiguration(dto);
            log.info("User configuration created successfully for userId: {}. Total entries: {}",
                    dto.getUserId(), userConfigurations.size());
            return ResponseEntity.ok(new CommonResponse<>(201, "userConfigurations created successfully", userConfigurations));
        } catch (Exception e) {
            log.error("Error creating user configuration for userId: {}", dto.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to create user configuration", null));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse> getUserConfigById(@PathVariable Integer id) {
        log.info("Fetching user configuration by ID: {}", id);

        try {
            UserConfiguration config = userConfigurationService.getUserConfigurationById(id);
            log.info("User configuration fetched successfully for ID: {}", id);
            return ResponseEntity.ok(new CommonResponse<>(200, "Fetched successfully", config));
        } catch (Exception e) {
            log.error("Error fetching user configuration for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch user configuration", null));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<UserConfiguration>> updateUserConfigById(
            @PathVariable Integer id,
            @RequestBody UserConfigurationRequestDto dto) {

        log.info("Updating user configuration for ID: {}", id);

        try {
            UserConfiguration updatedConfig = userConfigurationService.updateUserConfiguration(id, dto);
            log.info("User configuration updated successfully for ID: {}", id);
            return ResponseEntity.ok(new CommonResponse<>(200, "User configuration updated successfully", updatedConfig));
        } catch (Exception e) {
            log.error("Error updating user configuration for ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to update user configuration", null));
        }
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
        log.info("Fetching user configurations with filters - page: {}, size: {}, userId: {}, fullName: {}, email: {}",
                page, size, userId, fullName, email);

        try {
            Page<UserConfiguration> result = userConfigurationService.getAllUserConfigurations(
                    page, size, userId, fullName, email, mobile, state, userRole, parentAccount, menuId, subMenuId
            );
            log.info("Fetched {} user configurations", result.getTotalElements());
            return ResponseEntity.ok(new CommonResponse<>(200, "Success", result));
        } catch (Exception e) {
            log.error("Error fetching user configurations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch user configurations", null));
        }
    }

}
