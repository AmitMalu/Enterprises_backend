package com.im.admin.service.impl;

import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import com.im.admin.util.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public boolean verifyOTP(LoginRequestDto loginRequestDto) {
        log.info("Verifying OTP for username={}", loginRequestDto.getUserName());

        String username = loginRequestDto.getUserName();
        Optional<UserDetails> optionalUser;

        if (username.matches("\\d{10}")) {
            log.info("Username is a mobile number. Searching by mobileNumber={}", username);
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            log.info("Username is an email. Searching by emailId={}", username);
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            log.error("User not found for username={}", username);
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();
        log.info("User found | MobileNumber={}, HierarchyId={}", user.getMobileNumber(), user.getHierarchyId());

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            log.error("No OTP generated for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("No OTP generated for this user");
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            log.error("OTP expired for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtp().equals(loginRequestDto.getOtp())) {
            log.error("Invalid OTP entered={} for MobileNumber={}", loginRequestDto.getOtp(), user.getMobileNumber());
            throw new RuntimeException("Invalid OTP");
        }

        log.info("OTP verified successfully for MobileNumber={}", user.getMobileNumber());
        return true; // Important: return true when OTP is valid
    }


    @Override
    public CommonResponse<?> changePassword(ChangePasswordRequestDto request) {
        String username = request.getUserName();
        log.info("Change password requested for username={}", username);

        Optional<UserDetails> optionalUser = username.matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(username)
                : userDetailsRepository.findByEmailId(username);

        if (optionalUser.isEmpty()) {
            log.error("User not found for username={}", username);
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();
        log.info("User found | MobileNumber={}, HierarchyId={}, Status={}", user.getMobileNumber(), user.getHierarchyId(), user.getUserStatus());

        // Check Active status
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.error("User account is not active | MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("User account is not active");
        }

        if ("REQUEST_OTP".equalsIgnoreCase(request.getStep())) {
            // Generate OTP
            String otp = OtpUtil.generateOtp();
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
            userDetailsRepository.save(user);
            log.info("OTP generated and saved for MobileNumber={} | OTP={}", user.getMobileNumber(), otp);

            Map<String, String> otpData = new HashMap<>();
            otpData.put("otp", otp);

            return new CommonResponse<>(200, "OTP sent successfully", otpData);
        }

        // Step 2: VERIFY_AND_CHANGE
        if (!"VERIFY_AND_CHANGE".equalsIgnoreCase(request.getStep())) {
            log.error("Invalid step value provided: {}", request.getStep());
            throw new RuntimeException("Invalid step value. Must be 'REQUEST_OTP' or 'VERIFY_AND_CHANGE'");
        }

        // OTP validation
        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            log.error("No OTP generated for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("No OTP generated");
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            log.error("OTP expired for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            log.error("Invalid OTP entered={} for MobileNumber={}", request.getOtp(), user.getMobileNumber());
            throw new RuntimeException("Invalid OTP");
        }
        log.info("OTP verified successfully for MobileNumber={}", user.getMobileNumber());

        // Password checks
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("New password and confirm password do not match for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getLoginPassword())) {
            log.error("Old password is incorrect for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getLoginPassword())) {
            log.error("New password cannot be same as old password for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password cannot be same as old password");
        }

        // Save new password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setLoginPassword(encodedPassword);
        userDetailsRepository.save(user);
        log.info("Password changed successfully for MobileNumber={}", user.getMobileNumber());

        return new CommonResponse<>(200, "Password changed successfully", null);
    }

    @Override
    public CommonResponse<?> changePasswordadmin(ChangePasswordRequestDto request) {
        String username = request.getUserName();
        log.info("Change password requested for username={}", username);

        Optional<UserDetails> optionalUser = username.matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(username)
                : userDetailsRepository.findByEmailId(username);

        if (optionalUser.isEmpty()) {
            log.error("User not found for username={}", username);
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();
        log.info("User found | MobileNumber={}, HierarchyId={}, Status={}",
                user.getMobileNumber(), user.getHierarchyId(), user.getUserStatus());

        // Check Active status
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.error("User account is not active | MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("User account is not active");
        }

        // Password checks
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("New password and confirm password do not match for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getLoginPassword())) {
            log.error("Old password is incorrect for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getLoginPassword())) {
            log.error("New password cannot be same as old password for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password cannot be same as old password");
        }

        // Save new password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setLoginPassword(encodedPassword);
        userDetailsRepository.save(user);
        log.info("Password changed successfully for MobileNumber={}", user.getMobileNumber());

        return new CommonResponse<>(200, "Password changed successfully", null);
    }



}
