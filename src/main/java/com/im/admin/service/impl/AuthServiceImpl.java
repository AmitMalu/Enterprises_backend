package com.im.admin.service.impl;

import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import com.im.admin.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public boolean verifyOTP(LoginRequestDto loginRequestDto) {
        String username = loginRequestDto.getUserName();

        Optional<UserDetails> optionalUser;

        if (username.matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new RuntimeException("No OTP generated for this user");
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtp().equals(loginRequestDto.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        return true; // Important: return true when OTP is valid
    }

    @Override
    public CommonResponse<?> changePassword(ChangePasswordRequestDto request) {
        String username = request.getUserName();

        Optional<UserDetails> optionalUser = username.matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(username)
                : userDetailsRepository.findByEmailId(username);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();

        // Check Active status
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            throw new RuntimeException("User account is not active");
        }

        if ("REQUEST_OTP".equalsIgnoreCase(request.getStep())) {
            //  Generate OTP
            String otp = OtpUtil.generateOtp();
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
            userDetailsRepository.save(user);

            Map<String, String> otpData = new HashMap<>();
            otpData.put("otp", otp);


            return new CommonResponse<>(200, "OTP sent successfully", otpData);
        }

        // Step 2: VERIFY_AND_CHANGE
        if (!"VERIFY_AND_CHANGE".equalsIgnoreCase(request.getStep())) {
            throw new RuntimeException("Invalid step value. Must be 'REQUEST_OTP' or 'VERIFY_AND_CHANGE'");
        }

        // OTP validation
        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new RuntimeException("No OTP generated");
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        //  Password checks
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getLoginPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getLoginPassword())) {
            throw new RuntimeException("New password cannot be same as old password");
        }

        //  Save new password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setLoginPassword(encodedPassword);
        userDetailsRepository.save(user);

        return new CommonResponse<>(200, "Password changed successfully", null);
    }


}
