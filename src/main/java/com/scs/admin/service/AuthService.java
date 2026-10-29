package com.scs.admin.service;

import com.scs.admin.dto.ChangePasswordRequestDto;
import com.scs.admin.dto.LoginRequestDto;
import com.scs.admin.response.CommonResponse;

public interface AuthService {
    boolean verifyOTP(LoginRequestDto loginRequestDto);

    CommonResponse<?> changePassword(ChangePasswordRequestDto request);

    CommonResponse<?> changePasswordadmin(ChangePasswordRequestDto request);

//    void changePassword(ChangePasswordRequestDto request);
}
