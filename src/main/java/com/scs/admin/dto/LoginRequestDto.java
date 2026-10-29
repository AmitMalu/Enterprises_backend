package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    private String userName;
    private String password;
    private String otp;
    private String newPassword;
    private String reEnteredPassword;
    private String url;
    private String salt;
    private String iv;

    //for MobileApp
    private String mpin;
    private String oldMpin;
    private String newMpin;
    private String deviceId;
    private String confirmMpin;

}
