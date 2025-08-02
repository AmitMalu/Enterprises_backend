package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.repositories.*;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/auth")
@Slf4j
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

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private ProductsRepository productsRepository;

    @Autowired
    private SubProductsRepository subProductsRepository;

    @Autowired
    private AdminRepository adminRepository;


    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now(); // capture request time
        String requestJson = "";
        String responseJson = "";

        try {
            requestJson = objectMapper.writeValueAsString(loginRequestDto);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String username = loginRequestDto.getUserName();
        Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(username);

        if (optionalUser.isEmpty()) {
            optionalUser = userDetailsRepository.findByEmailId(username);
        }

        if (optionalUser.isEmpty()) {
            CommonResponse<?> response = new CommonResponse<>(401, "Invalid credentials: user not found", null);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs( requestJson, requestTime, responseJson,LocalDateTime.now(),null);            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }

        UserDetails user = optionalUser.get();

        if (!isUrlValidForUserHierarchy(user.getHierarchyId(), loginRequestDto.getUrl())) {
            CommonResponse<?> response = new CommonResponse<>(401, "Invalid login domain for this user", null);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }


        if (isUserLocked(user)) {
            CommonResponse<?> response = new CommonResponse<>(403, "Account is locked due to multiple failed attempts. Try again later.", null);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs( requestJson, requestTime, responseJson,LocalDateTime.now(),null);            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            CommonResponse<?> response = new CommonResponse<>(403, "User account is not active. Please contact support.", null);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }

        if (Boolean.TRUE.equals(user.getIsBlocked())) {
            CommonResponse<?> response = new CommonResponse<>(403, "User is blocked. Please contact support.", null);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }

        // Step 1: Try encrypted match
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getLoginPassword())) {

            // Step 2: If encrypted check fails, try plain-text match (fallback)
            if (loginRequestDto.getPassword().equals(user.getLoginPassword())) {

                // Successful login (via plain-text match)
                user.setFailedAttempts(0);
                user.setLockTime(null);

                String otp = OtpUtil.generateOtp();
                user.setOtp(otp);
                user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));

                userDetailsRepository.save(user);

                Map<String, String> otpData = new HashMap<>();
                otpData.put("otp", otp);

                CommonResponse<?> response = new CommonResponse<>(200, "OTP sent successfully", otpData);
                try {
                    responseJson = objectMapper.writeValueAsString(response);
                    commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ResponseEntity.ok(response);

            } else {
                // Password mismatch - increment failed attempts
                int attempts = user.getFailedAttempts() != null ? user.getFailedAttempts() : 0;
                attempts += 1;
                user.setFailedAttempts(attempts);

                if (attempts >= 5) {
                    user.setLockTime(LocalDateTime.now());
                }

                userDetailsRepository.save(user);

                String message = attempts >= 5 ?
                        "Account locked due to 5 failed attempts. Try again in 5 minutes." :
                        "Invalid credentials. Attempt " + attempts + " of 5.";

                CommonResponse<?> response = new CommonResponse<>(401, message, null);
                try {
                    responseJson = objectMapper.writeValueAsString(response);
                    commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return ResponseEntity.ok(response);
            }

        } else {
            // Successful login via encoded match
            user.setFailedAttempts(0);
            user.setLockTime(null);

            String otp = OtpUtil.generateOtp();
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));

            userDetailsRepository.save(user);

            Map<String, String> otpData = new HashMap<>();
            otpData.put("otp", otp);

            CommonResponse<?> response = new CommonResponse<>(200, "OTP sent successfully", otpData);
            try {
                responseJson = objectMapper.writeValueAsString(response);
                commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ResponseEntity.ok(response);
        }
    }

    public boolean isUrlValidForUserHierarchy(String hierarchyId, String requestUrl) {
        String currentId = hierarchyId;

        while (true) {
            String currentType = commonMethods.extractHierarchyType(currentId);

            if ("WL".equals(currentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelRepository.findById(currentId);
                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        String expectedUrl = wl.getDomainURL();
                        return requestUrl.contains(expectedUrl);
                    }
                }
            }

            if ("IM".equals(currentType) || "AD".equals(currentType)) {
                Optional<Admin> adminOpt = adminRepository.findById(currentId);
                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();
                    return requestUrl.equals(admin.getLoginURL());
                }
            }

            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) break;

            currentId = parentOpt.get();
        }

        return false;
    }


    private boolean isUserLocked(UserDetails user) {
        if (user.getLockTime() == null) return false;

        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(user.getLockTime().plusMinutes(5));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() + "&otp=" + loginRequestDto.getOtp();
        String responseJson = "";

        Optional<UserDetails> optionalUser;

        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName());
        } else {
            optionalUser = userDetailsRepository.findByEmailId(loginRequestDto.getUserName());
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

        if (!user.getOtp().equals(loginRequestDto.getOtp())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "Invalid OTP", null));
        }

        ResponseEntity<?> responseEntity = aesController.generateToken(loginRequestDto.getUserName());
        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            Map<String, Object> tokenData = (Map<String, Object>) responseEntity.getBody();
            String token = (String) tokenData.get("token");

            user.setUserLoginStatus(true);
            user.setOtp(null);
            user.setOtpExpiryTime(null);
            userDetailsRepository.save(user);
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("token", token);
            responseMap.put("hierarchyId", user.getHierarchyId());
            responseMap.put("mobileNumber", user.getMobileNumber());
            responseMap.put("emailId",user.getEmailId());

            String hierarchyCode = user.getHierarchyCode();

            if ("AD".equalsIgnoreCase(hierarchyCode)) {
                responseMap.put("role_id", user.getRole_id());

                ImSubRole subRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                if (subRole != null) {
                    responseMap.put("sub_role_id", subRole.getSubRoleId());
                    responseMap.put("role", hierarchyCode);
                }

            } else if ("WL".equalsIgnoreCase(hierarchyCode)) {
                WhiteLabels whiteLabel = whiteLabelRepository.findByMobileNumber(user.getMobileNumber());

                if (whiteLabel != null && Boolean.TRUE.equals(whiteLabel.getIsAdmin())) {
                    responseMap.put("role_id", user.getRole_id());

                    ImSubRole subRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                    if (subRole != null) {
                        responseMap.put("sub_role_id", subRole.getSubRoleId());
                    }

                    responseMap.put("role", "WL-AD"); // Treat as WL Admin
                } else {
                    responseMap.put("role", "WL"); // Normal WhiteLabel user
                }
            }

            if ("RT".equalsIgnoreCase(hierarchyCode)) {
                List<SchemeAllocation> schemes = schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());

                Map<String, List<Map<String, String>>> productSubMap = new LinkedHashMap<>();
                Map<String, String> productIdToName = new HashMap<>();

                for (SchemeAllocation sa : schemes) {
                    Optional<Products> productOpt = productsRepository.findByProductCode(sa.getProductCode());
                    Optional<SubProducts> subProductOpt = subProductsRepository.findBySubProductCode(sa.getSubProductCode());

                    if (productOpt.isEmpty() || subProductOpt.isEmpty()) {
                        log.warn("Skipping scheme due to missing mapping for productCode={} or subProductCode={}", sa.getProductCode(), sa.getSubProductCode());
                        continue;
                    }

                    String productId = String.valueOf(productOpt.get().getId());
                    String productName = productOpt.get().getName();

                    String subProductId = String.valueOf(subProductOpt.get().getId());
                    String subProductName = subProductOpt.get().getSubProductTitle();

                    // Save product name once
                    productIdToName.putIfAbsent(productId, productName);

                    Map<String, String> subProductMap = new HashMap<>();
                    subProductMap.put("subProductId", subProductId);
                    subProductMap.put("subProductName", subProductName);
                    subProductMap.put("schemeCode", sa.getSchemeCode());

                    productSubMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(subProductMap);
                }

                List<Map<String, Object>> productList = new ArrayList<>();

                for (Map.Entry<String, List<Map<String, String>>> entry : productSubMap.entrySet()) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("productId", entry.getKey());
                    product.put("productName", productIdToName.get(entry.getKey()));
                    product.put("subProducts", entry.getValue());
                    productList.add(product);
                }

                responseMap.put("products", productList);
                responseMap.put("role", hierarchyCode);
            }

            responseMap.put("role", hierarchyCode);
            try {
                responseJson = objectMapper.writeValueAsString(
                        new CommonResponse<>(200, "OTP verified successfully", responseMap)
                );

                commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            } catch (JsonProcessingException e) {
                e.printStackTrace(); // Optional: replace with logger
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new CommonResponse<>(500, "Error logging OTP verification", null));
            }

            return ResponseEntity.ok(new CommonResponse<>(200, "OTP verified successfully", responseMap));

        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error generating token: " + responseEntity.getBody());
        }
    }


    @PostMapping("/forget-verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() + "&otp=" + loginRequestDto.getOtp();
        String responseJson = "";

        try {
            boolean isVerified = authService.verifyOTP(loginRequestDto);
            CommonResponse<?> commonResponse;

            if (isVerified) {
                commonResponse = new CommonResponse<>(200, "OTP verified successfully", null);
            } else {
                commonResponse = new CommonResponse<>(401, "OTP verification failed", null);
            }

            // Convert response object to JSON
            responseJson = objectMapper.writeValueAsString(commonResponse);
            // Save login log
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);

            return ResponseEntity.status(commonResponse.getStatusCode()).body(commonResponse);

        } catch (RuntimeException | JsonProcessingException ex) {
            ex.printStackTrace(); // Optionally log it properly
            CommonResponse<?> errorResponse = new CommonResponse<>(500, ex.getMessage(), null);
            try {
                responseJson = objectMapper.writeValueAsString(errorResponse);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }



    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody LoginRequestDto  loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName();
        String responseJson = "";

        Optional<UserDetails> optionalUser;

        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName());
        } else {
            optionalUser = userDetailsRepository.findByEmailId(loginRequestDto.getUserName());
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
        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            // smsService.sendOtp(user.getMobileNumber(), newOtp);
        } else {
            // emailService.sendOtp(user.getEmail(), newOtp);
        }

        Map<String, String> otpData = new HashMap<>();
        otpData.put("otp", newOtp);  // remove in production if you don’t want to expose OTP
        
        CommonResponse<Map<String, String>> commonResponse =
                new CommonResponse<>(200, "OTP resent successfully", otpData);

        try {
            responseJson = objectMapper.writeValueAsString(commonResponse);

            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
        } catch (JsonProcessingException e) {
            // Optional: log the exception
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Internal error during logging", null));
        }
        return ResponseEntity.ok(commonResponse);
    }


    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() + "&newPassword=" + loginRequestDto.getNewPassword() + "&reEnteredPassword=" + loginRequestDto.getReEnteredPassword();
        String responseJson = "";

        if (!loginRequestDto.getNewPassword().equals(loginRequestDto.getReEnteredPassword())) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "New password and confirm password do not match", null));
        }

        if (loginRequestDto.getNewPassword().length() < 8) {
            return ResponseEntity.ok()
                    .body(new CommonResponse<>(400, "Password must be at least 8 characters long", null));
        }

        Optional<UserDetails> optionalUser;
        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName());
        } else {
            optionalUser = userDetailsRepository.findByEmailId(loginRequestDto.getUserName());
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

        String hashedPassword = passwordEncoder.encode(loginRequestDto.getNewPassword());
        user.setLoginPassword(hashedPassword);
        userDetailsRepository.save(user);

        CommonResponse<?> response = new CommonResponse<>(200, "Password updated successfully", null);
        try {
            responseJson = objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            responseJson = "{\"statusCode\":200,\"message\":\"Password updated successfully\",\"data\":null}";
        }

        commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName();
        String responseJson = "";

        Optional<UserDetails> optionalUser;
        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            optionalUser = userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName());
        } else {
            optionalUser = userDetailsRepository.findByEmailId(loginRequestDto.getUserName());
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
        data.put("OTP",otp);

        CommonResponse<Map<String, String>> commonResponse = new CommonResponse<>(200, "OTP generated successfully", data);
        try {
            responseJson = objectMapper.writeValueAsString(commonResponse);
        } catch (JsonProcessingException e) {
            responseJson = "{\"statusCode\":200,\"message\":\"OTP generated successfully\",\"data\":{\"message\":\"OTP has been sent to your registered mobile number\"}}";
        }

        commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
        return ResponseEntity.ok(commonResponse);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDto request) {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";

        try {
            requestJson = objectMapper.writeValueAsString(request);

            CommonResponse<?> response = authService.changePassword(request);  // updated to return response
            responseJson = objectMapper.writeValueAsString(response);
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            return ResponseEntity.ok(response);

        } catch (RuntimeException | JsonProcessingException ex) {
            ex.printStackTrace();
            CommonResponse<?> error = new CommonResponse<>(500, ex.getMessage(), null);
            try {
                responseJson = objectMapper.writeValueAsString(error);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    @PostMapping("/add")
    public String addData(@RequestParam String id) {

        return "String";
    }
}

