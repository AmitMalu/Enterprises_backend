package com.scs.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserConfigurationResponseDto {
    private Integer id;
    private String fullName;
    private String email;
    private String mobile;
    private String state;
    private String userRole;
    private String parentAccount;
    private List<String> permissions;
}
