package com.im.admin.controller;

import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.ImSubRole;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.ImSubRoleRepository;
import com.im.admin.repositories.SchemeAllocationRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.response.CommonResponse;
import com.im.admin.util.JwtUtil;
import com.im.admin.util.OtpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AESController aesController;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ImSubRoleRepository imSubRoleRepository;

    @Autowired
    private SchemeAllocationRepository schemeAllocationRepository;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        String username = loginRequestDto.getUserName();
        Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(username);

        if (optionalUser.isEmpty()) {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok(new CommonResponse<>(401, "Invalid credentials: user not found", null));
        }

        UserDetails user = optionalUser.get();

        if (isUserLocked(user)) {
            return ResponseEntity.ok(new CommonResponse<>(403, "Account is locked due to multiple failed attempts. Try again later.", null));
        }

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            return ResponseEntity.ok(new CommonResponse<>(403, "User account is not active. Please contact support.", null));
        }

        if (passwordEncoder.matches(loginRequestDto.getPassword(), user.getLoginPassword())) {
            // Reset failed attempts on successful login
            user.setFailedAttempts(0);
            user.setLockTime(null);

            String otp = OtpUtil.generateOtp();
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));

            userDetailsRepository.save(user);

            Map<String, String> otpData = new HashMap<>();
            otpData.put("otp", otp);

            return ResponseEntity.ok(new CommonResponse<>(200, "OTP sent successfully", otpData));
        } else {
            // Increase failed attempts
            int attempts = user.getFailedAttempts() != null ? user.getFailedAttempts() : 0;
            attempts += 1; // Increment before setting
            user.setFailedAttempts(attempts);

            if (attempts >= 5) {
                user.setLockTime(LocalDateTime.now());
            }

            userDetailsRepository.save(user);

            String message = attempts >= 5 ?
                    "Account locked due to 5 failed attempts. Try again in 5 minutes." :
                    "Invalid credentials. Attempt " + attempts + " of 5.";

            return ResponseEntity.ok(new CommonResponse<>(401, message, null));
        }
    }

    private boolean isUserLocked(UserDetails user) {
        if (user.getLockTime() == null) return false;

        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(user.getLockTime().plusMinutes(5));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String username, @RequestParam String otp) {

        Optional<UserDetails> optionalUser;

        if (username.matches("\\d{10}")) {
            // User entered mobile number
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            // User entered email
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();

        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "No OTP generated for this user", null));
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "OTP expired", null));
        }


        if (!user.getOtp().equals(otp)) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "Invalid OTP", null));
        }

        ResponseEntity<?> responseEntity = aesController.generateToken(username);
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> tokenData = (Map<String, Object>) responseEntity.getBody();
            String token = (String) tokenData.get("token");

            // Update login status and clear OTP
            user.setUserLoginStatus(true);
            user.setOtp(null);
            user.setOtpExpiryTime(null);
            userDetailsRepository.save(user);
            // Prepare final login response
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("token", token);

            String hierarchyCode = user.getHierarchyCode();

            if ("AD".equalsIgnoreCase(hierarchyCode) || "WL-AD".equalsIgnoreCase(hierarchyCode)) {
                responseMap.put("role_id", user.getRole_id());

                ImSubRole subRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                if (subRole != null) {
                    responseMap.put("sub_role_id", subRole.getSubRoleId());
                }
            }

            if ("RT".equalsIgnoreCase(hierarchyCode)) {
                List<SchemeAllocation> schemes = schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());

                // Group by product_code and collect list of sub_product_codes
                Map<String, List<String>> productToSubProductsMap = schemes.stream()
                        .collect(Collectors.groupingBy(
                                SchemeAllocation::getProductCode,
                                Collectors.mapping(SchemeAllocation::getSubProductCode, Collectors.toList())
                        ));

                responseMap.put("product_sub_product_codes", productToSubProductsMap);
            }
            
            return ResponseEntity.ok(new CommonResponse<>(200, "OTP verified successfully", responseMap));
        } else {
            // Handle error response if status is not OK
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating token: " + responseEntity.getBody());
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestParam String username) {
        Optional<UserDetails> optionalUser;

        if (username.matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(403, "User account is not active", null));
        }

        String newOtp = OtpUtil.generateOtp();
        user.setOtp(newOtp);
        user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
        userDetailsRepository.save(user);

        // Hook: Send OTP via SMS or Email
        if (username.matches("\\d{10}")) {
            // smsService.sendOtp(user.getMobileNumber(), newOtp);
        } else {
            // emailService.sendOtp(user.getEmail(), newOtp);
        }

        Map<String, String> otpData = new HashMap<>();
        otpData.put("otp", newOtp);  // remove in production if you don’t want to expose OTP

        return ResponseEntity.ok(new CommonResponse<>(200, "OTP resent successfully", otpData));
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(
            @RequestParam String username,
            @RequestParam String newPassword,
            @RequestParam String reEnteredPassword) {

        if (!newPassword.equals(reEnteredPassword)) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "New password and confirm password do not match", null));
        }

        if (newPassword.length() < 8) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "Password must be at least 8 characters long", null));
        }

        Optional<UserDetails> optionalUser;
        if (username.matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(403, "User account is not active", null));
        }

        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setLoginPassword(hashedPassword);
        userDetailsRepository.save(user);

        return ResponseEntity.ok(new CommonResponse<>(200, "Password updated successfully", null));
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String username) {

        Optional<UserDetails> optionalUser;
        if (username.matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(username);
        } else {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(403, "User account is not active", null));
        }

        String otp = OtpUtil.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
        userDetailsRepository.save(user);

        // Optional: Hook for sending OTP
        // smsService.sendOtp(user.getMobileNumber(), otp);
        // emailService.sendOtp(user.getEmail(), otp);

        Map<String, String> data = new HashMap<>();
        data.put("message", "OTP has been sent to your registered mobile number");

        return ResponseEntity.ok(new CommonResponse<>(200, "OTP generated successfully", data));
    }


    @PostMapping("/add")
    public String addData(@RequestParam String id) {

        return "String";
    }
}

