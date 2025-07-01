package com.im.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserConfigurationRequestDto {
    private String hierarchyId;
    private String userId;
    private String fullName;
    private String email;
    private String mobile;
    private String state;
    private String userRole;
    private List<UserPermissionDto> permissions;
}
