package com.im.admin.controller;

import com.im.admin.dto.UserConfigurationRequestDto;
import com.im.admin.entities.UserConfiguration;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
