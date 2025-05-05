package com.im.admin.controller;

import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.util.JwtUtil;
import com.im.admin.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam Long recordId, @RequestParam String otp) {

        Optional<UserDetails> optionalUser = userDetailsRepository.findById(recordId);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        UserDetails user = optionalUser.get();

        if (user.getOtp() == null || user.getOtpGenerationTime() == null) {
            return ResponseEntity.status(400).body("No OTP generated for this user");
        }

        if (LocalTime.now().isAfter(user.getOtpGenerationTime())) {
            return ResponseEntity.status(400).body("OTP expired");
        }

        if (!user.getOtp().equals(otp)) {
            return ResponseEntity.status(400).body("Invalid OTP");
        }

        // OTP is valid → generate token
        String token = jwtUtil.generateToken(user.getLoginUserName());

        // Optionally update login status
        user.setUserLoginStatus(true);
        user.setOtp(null);           // clear OTP after success
        user.setOtpGenerationTime(null);     // clear expiry
        userDetailsRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("token", token);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {

        Optional<UserDetails> optionalUser = userDetailsRepository.findByLoginUserName(username);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials: user not found");
        }

        UserDetails user = optionalUser.get();

        // Check user status
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            return ResponseEntity.status(403).body("User account is not active. Please contact support.");
        }

        if (user.getMobileNumber().equals(username) && user.getLoginPassword().equals(password)) {
            String otp = OtpUtil.generateOtp();

            user.setOtp(otp);
            user.setOtpGenerationTime(LocalTime.now());

            userDetailsRepository.save(user);

            Map<String, String> response = new HashMap<>();
            response.put("otp", otp);

            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(401).body("Invalid credentials");

    }

    @PostMapping("/add")
    public String addData(@RequestParam String id) {

        return "String";
    }
}

