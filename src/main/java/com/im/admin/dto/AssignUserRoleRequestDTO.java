package com.im.admin.dto;

import lombok.Data;

@Data
public class AssignUserRoleRequestDTO {
    private String mobileNumber;
    private  String roleId;
    private String subRoleId;
}
