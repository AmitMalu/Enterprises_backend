package com.im.admin.service.impl;

import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;
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
}
