package com.im.admin.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Issues a fixed OTP for demo accounts so the sales team can log in without SMS.
 *
 * Only active when both properties are set (the demo profile sets them):
 *   app.demo.fixed-otp            the OTP to issue, e.g. 123456
 *   app.demo.otp-mobile-prefixes  comma-separated mobile prefixes of demo accounts, e.g. 90000000
 * Every other mobile number still gets a random OTP. Leave the properties empty in production.
 */
@Slf4j
@Component
public class DemoOtpProvider {

    @Value("${app.demo.fixed-otp:}")
    private String fixedOtp;

    @Value("${app.demo.otp-mobile-prefixes:}")
    private List<String> demoMobilePrefixes;

    @PostConstruct
    void logStatus() {
        if (isEnabled()) {
            log.warn("DEMO OTP ENABLED: mobiles starting with {} receive a fixed OTP. Never enable this in production.",
                    demoMobilePrefixes);
        }
    }

    /** Fixed OTP for demo mobiles, otherwise a normal random OTP. */
    public String otpFor(String mobileNumber) {
        if (isDemoMobile(mobileNumber)) {
            return fixedOtp;
        }
        return OtpUtil.generateOtp();
    }

    public boolean isDemoMobile(String mobileNumber) {
        return isEnabled() && mobileNumber != null && demoMobilePrefixes.stream()
                .map(String::trim)
                .anyMatch(prefix -> !prefix.isEmpty() && mobileNumber.startsWith(prefix));
    }

    private boolean isEnabled() {
        return fixedOtp != null && fixedOtp.matches("\\d{6}")
                && demoMobilePrefixes != null && demoMobilePrefixes.stream().anyMatch(p -> !p.isBlank());
    }
}
