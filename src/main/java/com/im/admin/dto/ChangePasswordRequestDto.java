package com.im.admin.dto;

import lombok.Data;

@Data
public class ChangePasswordRequestDto {
    private String userName;
    private String otp;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
}
