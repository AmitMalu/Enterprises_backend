package com.scs.admin.service.impl;

import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.commons.ShivSmsService;
import com.scs.admin.dto.ChangePasswordRequestDto;
import com.scs.admin.dto.LoginRequestDto;
import com.scs.admin.entities.*;
import com.scs.admin.repositories.*;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AuthService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    /** Fixed OTP for demo accounts only; random OTP for everyone else (see DemoOtpProvider). */
    @Autowired
    private com.scs.admin.util.DemoOtpProvider demoOtpProvider;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ShivSmsService shivSmsService;

    @Autowired
    private SmsCredentialsRepository smsCredentialsRepository;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private WhiteLabelRepository whiteLabelsRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private EnterprisesRepository enterprisesRepository;

    @Autowired
    private WhiteLabelUserRepository whiteLabelUserRepository;

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
            String otp = demoOtpProvider.otpFor(user.getMobileNumber());
            user.setOtp(otp);
            user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
            userDetailsRepository.save(user);
            log.info("OTP generated and saved for MobileNumber={} | OTP={}", user.getMobileNumber(), otp);

            // Send OTP via SMS
            try {
                String fromId = resolveFromEmail(
                        user.getHierarchyCode(),
                        user.getHierarchyId()
                );

                String companyName = getCompanyName(fromId);

                SmsCredentialsMaster smsConfig = getSmsCredentials(
                        user.getHierarchyCode(),
                        user.getHierarchyId(),
                        "LOGIN_ALERT"
                );

                String smsResponse = shivSmsService.sendOtpSms(
                        smsConfig,
                        user.getMobileNumber(),
                        user.getLoginUserName(),
                        otp,
                        companyName
                );

                commonFunctions.saveSmsLogs(
                        LocalDateTime.now(),
                        "Send OTP to: " + user.getMobileNumber(),
                        LocalDateTime.now(),
                        smsResponse,
                        null
                );

                log.info("OTP sent via SMS to: {}", user.getMobileNumber());

            } catch (Exception e) {
                log.error("Failed to send OTP via SMS to {}", user.getMobileNumber(), e);
            }

            Map<String, String> otpData = new HashMap<>();
            //otpData.put("otp", otp);

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
        log.info("Change password Admin requested for username={}", username);

        Optional<UserDetails> optionalUser = username.matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(username)
                : userDetailsRepository.findByEmailId(username);

        if (optionalUser.isEmpty()) {
            log.error("[Change password Admin] User not found for username={}", username);
            throw new RuntimeException("User not found");
        }

        UserDetails user = optionalUser.get();
        log.info("[Change password Admin] User found | MobileNumber={}, HierarchyId={}, Status={}",
                user.getMobileNumber(), user.getHierarchyId(), user.getUserStatus());

        // Check Active status
        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.error("[Change password Admin] User account is not active | MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("User account is not active");
        }

        // Password checks
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.error("[Change password Admin] New password and confirm password do not match for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password and confirm password do not match");
        }

        if (!passwordEncoder.matches(request.getOldPassword(), user.getLoginPassword())) {
            log.error("[Change password Admin] Old password is incorrect for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("Old password is incorrect");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getLoginPassword())) {
            log.error("[Change password Admin] New password cannot be same as old password for MobileNumber={}", user.getMobileNumber());
            throw new RuntimeException("New password cannot be same as old password");
        }

        // Save new password
        String encodedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setLoginPassword(encodedPassword);
        userDetailsRepository.save(user);
        log.info("[Change password Admin] Password changed successfully for MobileNumber={}", user.getMobileNumber());

        return new CommonResponse<>(200, "Password changed successfully", null);
    }

    private String getCompanyName(String fromId) {
        Optional<WhiteLabels> wlOpt = whiteLabelsRepository.findById(fromId);
        if (wlOpt.isPresent()) {
            return wlOpt.get().getOutletAddress().getCompanyName();
        }

        Optional<Admin> adOpt = adminRepository.findById(fromId);
        if (adOpt.isPresent()) {
            return adOpt.get().getName();
        }

        return "SMART CREDIT SOLUTION..";
    }

    private SmsCredentialsMaster getSmsCredentials(String hierarchyType, String hierarchyId, String smsType) {
        String fromId = resolveFromEmail(hierarchyType, hierarchyId);

        return smsCredentialsRepository.findByWhitelabelIdAndType(fromId, smsType)
                .orElseGet(() -> smsCredentialsRepository.findByWhitelabelIdAndType("SCS-AD-001", smsType)
                        .orElseThrow(() -> new RuntimeException("SMS credentials not found for type: " + smsType)));
    }

    private String resolveFromEmail(String hierarchyType, String hierarchyId) {

        switch (hierarchyType) {

            case "RT" -> {
                Retailer rt = retailerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Retailer not found"));

                String parentId = rt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "DT" -> {
                Distributor dt = distributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Distributor not found"));

                String parentId = dt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "PT" -> {
                Partner pt = partnerRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Partner not found"));

                String parentId = pt.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "SD" -> {
                SuperDistributor sd = superDistributorRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("SuperDistributor not found"));

                String parentId = sd.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "WL" -> {
                WhiteLabels wl = whiteLabelsRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("WhiteLabel not found"));

                if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                    return wl.getWhiteLabelId();
                }

                String parentId = wl.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "WLAU" -> {
                Optional<WhiteLabelUser> wlauOpt =
                        whiteLabelUserRepository.findById(hierarchyId);

                if (wlauOpt.isEmpty()) {
                    throw new RuntimeException("WhiteLabelUser not found");
                }

                WhiteLabelUser wlau = wlauOpt.get();
                String parentId = wlau.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "ET" -> {
                Enterprises et = enterprisesRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Enterprise not found"));

                String parentId = et.getParentAccount();
                String parentType = commonMethods.extractHierarchyType(parentId);

                return resolveFromEmail(parentType, parentId);
            }

            case "AD", "SCS" -> {
                Admin ad = adminRepository.findById(hierarchyId)
                        .orElseThrow(() -> new RuntimeException("Admin not found"));

                return ad.getId();
            }

            default -> throw new RuntimeException("Invalid hierarchy type: " + hierarchyType);
        }
    }

}
