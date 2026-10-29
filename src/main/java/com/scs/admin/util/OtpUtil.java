package com.scs.admin.util;

import org.springframework.stereotype.Service;


public class OtpUtil {

    public static String generateOtp() {
        return String.valueOf((int) ((Math.random() * 900000) + 100000)); // 6-digit OTP
    }

}

