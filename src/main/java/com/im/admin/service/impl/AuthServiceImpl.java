package com.im.admin.service.impl;

import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
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
    public void changePassword(ChangePasswordRequestDto request) {
        String username = request.getUserName();

        // Fetch user by mobile or email
        Optional<UserDetails> optionalUser = username.matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(username)
                : userDetailsRepository.findByEmailId(username);

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();

        //  Check if user status is Active
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            throw new RuntimeException("User account is not active");
        }

        //  OTP validation
        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            throw new RuntimeException("No OTP generated");
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            throw new RuntimeException("OTP expired");
        }

        if (!user.getOtp().equals(request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        //  Password match validation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("New password and confirm password do not match");
        }

        //  Old password validation
        if (!passwordEncoder.matches(request.getOldPassword(), user.getLoginPassword())) {
            throw new RuntimeException("Old password is incorrect");
        }

        // s New password must be different from old
        if (passwordEncoder.matches(request.getNewPassword(), user.getLoginPassword())) {
            throw new RuntimeException("New password cannot be same as old password");
        }

        //  Update password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setLoginPassword(encodedPassword);
        userDetailsRepository.save(user);
    }


}
