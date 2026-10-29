package com.scs.admin.dto;

import lombok.Data;

@Data
public class ChangePasswordRequestDto {
    private String userName;
    private String otp;
    private String oldPassword;
    private String newPassword;
    private String confirmPassword;
    private String step; // "REQUEST_OTP" or "VERIFY_AND_CHANGE"
}
