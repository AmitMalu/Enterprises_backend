package com.im.admin.service;

import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.response.CommonResponse;

public interface AuthService {
    boolean verifyOTP(LoginRequestDto loginRequestDto);

    CommonResponse<?> changePassword(ChangePasswordRequestDto request);

    CommonResponse<?> changePasswordadmin(ChangePasswordRequestDto request);

//    void changePassword(ChangePasswordRequestDto request);
}
