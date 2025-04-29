package com.im.admin.util;

public class OtpUtil {

    public static String generateOtp() {
        return String.valueOf((int) ((Math.random() * 900000) + 100000)); // 6-digit OTP
    }

}

