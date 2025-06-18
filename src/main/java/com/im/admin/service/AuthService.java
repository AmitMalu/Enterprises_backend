package com.im.admin.service;

import com.im.admin.dto.LoginRequestDto;

public interface AuthService {
    boolean verifyOTP(LoginRequestDto loginRequestDto);
}
