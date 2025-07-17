package com.im.admin.service;

import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;

public interface AuthService {
    boolean verifyOTP(LoginRequestDto loginRequestDto);

    void changePassword(ChangePasswordRequestDto request);
}
