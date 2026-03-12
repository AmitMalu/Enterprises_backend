package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.commons.SMSGatewayAPI;
import com.im.admin.dto.ChangePasswordRequestDto;
import com.im.admin.dto.LoginRequestDto;
import com.im.admin.entities.*;
import com.im.admin.entities.product.Products;
import com.im.admin.entities.vendor.Vendors;
import com.im.admin.repositories.*;
import com.im.admin.request.TokenGenerationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AuthService;
import com.im.admin.util.AESEncryptionUtil;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.OtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import javax.crypto.spec.IvParameterSpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Value("${app.security.base-secret}")
    private String baseSecret;

    @Value("${app.security.pbkdf2-iterations}")
    private int iterations;

    @Value("${app.security.pbkdf2-keylength}")
    private int keyLength;

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

    @Autowired
    private AESEncryptionUtil aesEncryptionUtil;

    @Autowired
    private SMSGatewayAPI smsGatewayAPI;

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private VendorRepository vendorRepository;

    @Autowired
    private SchemeRepository schemeRepository;


    @Autowired
    private VendorRoutingRepository vendorRoutingRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDto loginRequestDto) {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = serializeRequest(loginRequestDto);

        try {
            // 1️ IV
            byte[] ivBytes = Base64.getDecoder().decode(loginRequestDto.getIv());
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            byte[] saltBytes = Base64.getDecoder().decode(loginRequestDto.getSalt());

            // 2️ Derive AES key using the Base64 salt string directly
            byte[] secretKey = aesEncryptionUtil.deriveKey(baseSecret,
                    saltBytes, // <--- use the Base64 string directly
                    iterations, keyLength);

            // 3️ Decrypt username & password
            String decryptedUsername = aesEncryptionUtil.decrypt(loginRequestDto.getUserName(), secretKey, ivSpec);
            String decryptedPassword = aesEncryptionUtil.decrypt(loginRequestDto.getPassword(), secretKey, ivSpec);

            log.info("Login attempt started for username: {} and password: {}", decryptedUsername, decryptedPassword);

            // 4️ Find user by decrypted username
            Optional<UserDetails> optionalUser = findUserByUsername(decryptedUsername);
            if (optionalUser.isEmpty()) {
                log.warn("User not found for username: {}", decryptedUsername);
                return buildErrorResponse(requestJson, requestTime, 400, "Invalid credentials: Incorrect Username");
            }

            UserDetails user = optionalUser.get();
            log.debug("User found: ID={}, Status={}, Hierarchy={}", user.getHierarchyId(),
                    user.getUserStatus(), user.getHierarchyId());

            // 5 Domain / status checks
            if (!isUrlValidForUserHierarchy(user.getHierarchyId(), loginRequestDto.getUrl())) {
                log.warn("Invalid login domain for user ID: {}", user.getHierarchyId());
                return buildErrorResponse(requestJson, requestTime, 401, "Invalid login domain for this user");
            }

            if (isUserLocked(user)) {
                log.warn("User account locked due to failed attempts: ID={}", user.getHierarchyId());
                return buildErrorResponse(requestJson, requestTime, 403,
                        "Account is locked due to multiple failed attempts. Try again later.");
            }

            if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
                log.warn("User account is not active: ID={}", user.getHierarchyId());
                return buildErrorResponse(requestJson, requestTime, 403,
                        "User account is not active. Please contact support.");
            }

            if (Boolean.TRUE.equals(user.getIsBlocked())) {
                log.warn("User is blocked: ID={}", user.getHierarchyId());
                return buildErrorResponse(requestJson, requestTime, 403,
                        "User is blocked. Please contact support.");
            }

            // 6️ Hand off to your password validation using decrypted password
            return handlePasswordValidation(user, decryptedPassword, requestJson, requestTime);

        } catch (Exception e) {
            log.error("Error processing encrypted login credentials", e);
            return buildErrorResponse(requestJson, requestTime, 400, "Invalid encrypted credentials");
        }
    }


    private Optional<UserDetails> findUserByUsername(String username) {
        Optional<UserDetails> user = userDetailsRepository.findByMobileNumber(username);
        return user.isPresent() ? user : userDetailsRepository.findByEmailId(username);
    }

    private String serializeRequest(Object request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            log.error("Failed to serialize request", e);
            return "{}";
        }
    }

    private ResponseEntity<?> buildErrorResponse(String requestJson, LocalDateTime requestTime, int status, String message) {
        CommonResponse<?> response = new CommonResponse<>(status, message, null);
        logLoginAttempt(requestJson, requestTime, response);
        return ResponseEntity.status(status).body(response);
    }

    private void logLoginAttempt(String requestJson, LocalDateTime requestTime, CommonResponse<?> response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.info("Login log saved for status {}: {}", response.getStatusCode(), response.getMessage());
        } catch (Exception e) {
            log.error("Failed to save login log", e);
        }
    }

    private ResponseEntity<?> handlePasswordValidation(UserDetails user, String inputPassword, String requestJson, LocalDateTime requestTime) {
        boolean passwordMatches = passwordEncoder.matches(inputPassword, user.getLoginPassword());
        boolean fallbackMatches = inputPassword.equals(user.getLoginPassword());

        if (!passwordMatches && !fallbackMatches) {
            log.warn("Password mismatch for user ID: {}", user.getHierarchyId());
            return handleFailedAttempt(user, requestJson, requestTime);
        }

        resetFailedAttempts(user);
        String otp = generateOtpForUser(user);

        String hierarchyCode = user.getHierarchyCode();

        Map<String, String> otpData = Map.of("otp", otp);
        // create a new map only for response
        Map<String, String> responseData = new HashMap<>(otpData);
        responseData.put("role", hierarchyCode);

//        CommonResponse<?> response = new CommonResponse<>(200, "OTP sent successfully", otpData); // old
        CommonResponse<?> response = new CommonResponse<>(200, "OTP sent successfully", responseData);
        logLoginAttempt(requestJson, requestTime, response);
        log.info("OTP sent successfully for user ID: {}", user.getHierarchyId());
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> handleFailedAttempt(UserDetails user, String requestJson, LocalDateTime requestTime) {
        int attempts = Optional.ofNullable(user.getFailedAttempts()).orElse(0) + 1;
        user.setFailedAttempts(attempts);

        if (attempts >= 5) {
            user.setLockTime(LocalDateTime.now());
            log.warn("User account locked after {} failed attempts: ID={}", attempts, user.getHierarchyId());
        } else {
            log.warn("Failed login attempt {} for user ID: {}", attempts, user.getHierarchyId());
        }

        userDetailsRepository.save(user);

        String message = attempts >= 5 ?
                "Account locked due to 5 failed attempts. Try again in 5 minutes." :
                "Password Incorrect. Attempt " + attempts + " of 5.";

        return buildErrorResponse(requestJson, requestTime, 400, message);
    }

    private void resetFailedAttempts(UserDetails user) {
        user.setFailedAttempts(0);
        user.setLockTime(null);
        log.debug("Resetting failed attempts for user ID: {}", user.getHierarchyId());
    }

    private String generateOtpForUser(UserDetails user) {
        String otp = OtpUtil.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
        userDetailsRepository.save(user);
        log.info("OTP generated for user ID: {}", user.getHierarchyId());

        try {
            //  Send OTP via SMS
//            String smsResponse = smsGatewayAPI.sendOTPSms(user.getMobileNumber(), otp);

//            //Save SMS logs
//            commonFunctions.saveSmsLogs(
//                    LocalDateTime.now(),
//                    "Send OTP to: " + user.getMobileNumber() + ", OTP: " + otp,
//                    LocalDateTime.now(),
//                    smsResponse,
//                    null // You can replace null with real IP if available
//            );

            log.info("OTP sent via SMS to: {}", user.getMobileNumber());
        } catch (Exception e) {
            log.error("Failed to send OTP via SMS to {}", user.getMobileNumber(), e);
        }

        return otp;
    }


    public boolean isUrlValidForUserHierarchy(String hierarchyId, String requestUrl) {
        String currentId = hierarchyId;
        log.info("Validating URL '{}' for hierarchy ID '{}'", requestUrl, hierarchyId);

        while (true) {
            String currentType = commonMethods.extractHierarchyType(currentId);
            log.debug("Current hierarchy ID: {}, Type: {}", currentId, currentType);

            if ("WL".equals(currentType)) {
                Optional<WhiteLabels> wlOpt = whiteLabelRepository.findById(currentId);
                if (wlOpt.isPresent()) {
                    WhiteLabels wl = wlOpt.get();
                    log.debug("WhiteLabel found: ID={}, isAdmin={}, domainURL={}", currentId, wl.getIsAdmin(), wl.getDomainURL());

                    if (Boolean.TRUE.equals(wl.getIsAdmin())) {
                        boolean match = requestUrl.contains(wl.getDomainURL());
                        log.info("URL match for WhiteLabel admin: {}", match);
                        return match;
                    }
                } else {
                    log.warn("WhiteLabel not found for ID: {}", currentId);
                }
            }

            if ("IM".equals(currentType) || "AD".equals(currentType)) {
                Optional<Admin> adminOpt = adminRepository.findById(currentId);
                if (adminOpt.isPresent()) {
                    Admin admin = adminOpt.get();
                    boolean match = requestUrl.equals(admin.getLoginURL());
                    log.info("URL match for Admin/IM: {}", match);
                    return match;
                } else {
                    log.warn("Admin not found for ID: {}", currentId);
                }
            }

            Optional<String> parentOpt = commonMethods.findParentHierarchy(currentId);
            if (parentOpt.isEmpty()) {
                log.info("No parent found for hierarchy ID: {}. URL validation failed.", currentId);
                break;
            }

            currentId = parentOpt.get();
            log.debug("Traversing to parent hierarchy ID: {}", currentId);
        }

        log.info("URL '{}' is not valid for hierarchy ID '{}'", requestUrl, hierarchyId);
        return false;
    }

    private boolean isUserLocked(UserDetails user) {
        if (user.getLockTime() == null) {
            log.debug("User ID {} is not locked (no lockTime set)", user.getHierarchyId());
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        boolean locked = now.isBefore(user.getLockTime().plusMinutes(5));

        log.info("User ID {} lock status: {} (locked until {})", user.getHierarchyId(), locked, user.getLockTime().plusMinutes(5));
        return locked;
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody LoginRequestDto loginRequestDto) throws Exception {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() + "&otp=" + loginRequestDto.getOtp();
        String responseJson = "";

        log.info("OTP verification attempt for username: {}", loginRequestDto.getUserName());

        // 1. Find user
        Optional<UserDetails> optionalUser = loginRequestDto.getUserName().matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName())
                : userDetailsRepository.findByEmailId(loginRequestDto.getUserName());

        if (optionalUser.isEmpty()) {
            log.warn("User not found for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();

        // 2. Validate OTP
        if (user.getOtp() == null || user.getOtpExpiryTime() == null) {
            log.warn("No OTP generated for user ID: {}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(400, "No OTP generated for this user", null));
        }

        if (LocalTime.now().isAfter(user.getOtpExpiryTime())) {
            log.warn("OTP expired for user ID: {}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(400, "OTP expired", null));
        }

        if (!user.getOtp().equals(loginRequestDto.getOtp())) {
            log.warn("Invalid OTP entered for user ID: {}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(400, "Invalid OTP", null));
        }

        log.info("OTP verified successfully for user ID: {}", user.getHierarchyId());

        // 3. Generate salt + IV
        String saltBase64 = aesEncryptionUtil.generateRandomSalt();
        IvParameterSpec ivSpec = aesEncryptionUtil.generateIv();
        String ivBase64 = Base64.getEncoder().encodeToString(ivSpec.getIV());

        // 4. Derive key
        byte[] saltBytes = Base64.getDecoder().decode(saltBase64);
        byte[] secretKey = aesEncryptionUtil.deriveKey(baseSecret, saltBytes, iterations, keyLength);

        // create token payload (whatever you want inside)
        long now = System.currentTimeMillis();
        long exp = now + 3600_000L; // 1 hour expiry
        String payload = now + "/" + exp + "/" + user.getMobileNumber();

        // encrypt payload
        String encryptedToken = aesEncryptionUtil.encrypt(payload, secretKey, ivSpec);

        // final token string: hierarchyIds|salt|iv|ciphertext
        String fullToken = user.getHierarchyId() + "|" + saltBase64 + "|" + ivBase64 + "|" + encryptedToken;  // current token

        // 7. Update user login status
        user.setUserLoginStatus(true);
        user.setOtp(null);
        user.setOtpExpiryTime(null);
        userDetailsRepository.save(user);

        // 8. Build responseMap
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("token", fullToken);
        responseMap.put("hierarchyId", user.getHierarchyId());
        responseMap.put("mobileNumber", user.getMobileNumber());
        responseMap.put("emailId", user.getEmailId());
//        responseMap.put("logoUrl","https://www.instantmudra.net/apps/home/media/logos/logo-white.png");

        String hierarchyCode = user.getHierarchyCode();
        log.debug("Resolving role for hierarchyCode: {}", hierarchyCode);

        switch (hierarchyCode.toUpperCase()) {
            case "AD":
                responseMap.put("role_id", user.getRole_id());
                ImSubRole adSubRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                if (adSubRole != null) {
                    responseMap.put("sub_role_id", adSubRole.getSubRoleId());
                    responseMap.put("role", hierarchyCode);
                }

                // Build product/subProduct list for AD
                List<SchemeAllocation> adSchemes = schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());
                Map<String, List<Map<String, String>>> adProductSubMap = new LinkedHashMap<>();
                Map<String, String> adProductIdToName = new HashMap<>();
                Map<String, Products> adProductIdToEntity = new HashMap<>();

                for (SchemeAllocation sa : adSchemes) {
                    Optional<Products> productOpt = productsRepository.findByProductCode(sa.getProductCode());
                    Optional<SubProducts> subProductOpt = subProductsRepository.findBySubProductCodeAndIsVisibleTrue(sa.getSubProductCode());

                    if (productOpt.isEmpty() || subProductOpt.isEmpty()) continue;

                    Products productEntity = productOpt.get();
                    String productId = String.valueOf(productEntity.getId());
                    String productName = productEntity.getName();
                    String subProductId = String.valueOf(subProductOpt.get().getId());
                    String subProductName = subProductOpt.get().getSubProductTitle();

                    adProductIdToName.putIfAbsent(productId, productName);
                    adProductIdToEntity.putIfAbsent(productId, productEntity);

                    Map<String, String> subProductMap = new HashMap<>();
                    subProductMap.put("subProductId", subProductId);
                    subProductMap.put("subProductName", subProductName);
                    subProductMap.put("schemeCode", sa.getSchemeCode());

                    adProductSubMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(subProductMap);
                }

                List<Map<String, Object>> adProductList = new ArrayList<>();
                for (Map.Entry<String, List<Map<String, String>>> entry : adProductSubMap.entrySet()) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("productId", entry.getKey());
                    product.put("productName", adProductIdToName.get(entry.getKey()));

                    //  Add active and blocked fields from product table
                    Products productEntity = adProductIdToEntity.get(entry.getKey());
                    if (productEntity != null) {
                        product.put("active", productEntity.getActive());
                        product.put("blocked", productEntity.getBlocked());
                    }

                    product.put("subProducts", entry.getValue());
                    adProductList.add(product);
                }

                responseMap.put("products", adProductList);
                break;


            case "WL":
                WhiteLabels whiteLabel = whiteLabelRepository.findByMobileNumber(user.getMobileNumber());
                if (whiteLabel != null && Boolean.TRUE.equals(whiteLabel.getIsAdmin())) {
                    responseMap.put("role_id", user.getRole_id());
                    ImSubRole wlSubRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                    if (wlSubRole != null) {
                        responseMap.put("sub_role_id", wlSubRole.getSubRoleId());
                    }
                    responseMap.put("role", "WL-AD");
                    responseMap.put("logoUrl",whiteLabel.getBusinessProofImage());
                } else {
                    responseMap.put("role", "WL");
                }
                break;

            case "RT":
                responseMap.put("role_id", user.getRole_id());

                ImSubRole rtSubRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                if (rtSubRole != null) {
                    responseMap.put("sub_role_id", rtSubRole.getSubRoleId());
                }

                // Fetch outlet name
                Retailer retailer = retailerRepository.findByMobileNumber(user.getMobileNumber());
                if (retailer != null
                        && retailer.getOutletAddress() != null
                        && retailer.getOutletAddress().getOutletName() != null) {
                    responseMap.put("outletName", retailer.getOutletAddress().getOutletName());
                    responseMap.put("logoUrl",retailer.getBusinessProofImage());
                } else {
                    responseMap.put("outletName", "N/A");
                }

//                 Build product / subProduct list
//                List<SchemeAllocation> schemes =
//                        schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());
//
//                //  FIXED GENERICS
//                Map<String, List<Map<String, Object>>> productSubMap = new LinkedHashMap<>();
//                Map<String, String> productIdToName = new HashMap<>();
//                Map<String, Products> productIdToEntity = new HashMap<>();
//
//                for (SchemeAllocation sa : schemes) {
//
//                    Optional<Products> productOpt =
//                            productsRepository.findByProductCode(sa.getProductCode());
//
//                    Optional<SubProducts> subProductOpt =
//                            subProductsRepository.findBySubProductCodeAndIsVisibleTrue(
//                                    sa.getSubProductCode());
//
//                    if (productOpt.isEmpty() || subProductOpt.isEmpty()) {
//                        log.warn(
//                                "Skipping scheme due to missing mapping for productCode={} or subProductCode={}",
//                                sa.getProductCode(),
//                                sa.getSubProductCode()
//                        );
//                        continue;
//                    }
//
//                    Products productEntity = productOpt.get();
//                    SubProducts subProductEntity = subProductOpt.get();
//
//                    String productId = String.valueOf(productEntity.getId());
//                    String productName = productEntity.getName();
//                    String subProductId = String.valueOf(subProductEntity.getId());
//                    String subProductName = subProductEntity.getSubProductTitle();
//
//                    VendorRouting routing =
//                            vendorRoutingRepository.findBySubProduct(subProductEntity);
//
//                    String vendorName = "N/A";
//                    if (routing != null && routing.getRoutingPriority1() != null) {
//                        vendorName = routing.getRoutingPriority1().getVendorName();
//                    }
//
//                    productIdToName.putIfAbsent(productId, productName);
//                    productIdToEntity.putIfAbsent(productId, productEntity);
//
//                    //  FIXED MAP TYPE
//                    Map<String, Object> subProductMap = new HashMap<>();
//                    subProductMap.put("subProductId", subProductId);
//                    subProductMap.put("subProductName", subProductName);
//                    subProductMap.put("schemeCode", sa.getSchemeCode());
//                    subProductMap.put("vendorName", vendorName);
//
//                    //  Sub-Product flags
//                    subProductMap.put("active", subProductEntity.getIsActive());
//                    subProductMap.put("blocked", subProductEntity.getIsBlocked());
//                    subProductMap.put("isVisible", subProductEntity.getIsVisible());
//
//                    productSubMap
//                            .computeIfAbsent(productId, k -> new ArrayList<>())
//                            .add(subProductMap);
//                }
//
//                // Build final product list
//                List<Map<String, Object>> productList = new ArrayList<>();
//
//                //  FIXED GENERICS
//                for (Map.Entry<String, List<Map<String, Object>>> entry : productSubMap.entrySet()) {
//
//                    Map<String, Object> product = new HashMap<>();
//                    product.put("productId", entry.getKey());
//                    product.put("productName", productIdToName.get(entry.getKey()));
//
//                    Products productEntity = productIdToEntity.get(entry.getKey());
//                    if (productEntity != null) {
//                        product.put("active", productEntity.getActive());
//                        product.put("blocked", productEntity.getBlocked());
//                        product.put("isVisible", productEntity.getIsVisible());
//                    }
//
//                    product.put("subProducts", entry.getValue());
//                    productList.add(product);
//                }
//
//                responseMap.put("products", productList);
//                responseMap.put("role", hierarchyCode);
//                break;

                // Build product / subProduct list
                List<SchemeAllocation> schemeAllocationList =
                        schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());

                // Fetch ALL products
                List<Products> allProducts = productsRepository.findAll();

                Map<String, List<Map<String, Object>>> productSubMap = new LinkedHashMap<>();
                Map<String, Products> productIdToEntity = new HashMap<>();

                // Initialize all products first (so even products without scheme appear)
                for (Products product : allProducts) {
                    String productId = String.valueOf(product.getId());

                    productSubMap.putIfAbsent(productId, new ArrayList<>());
                    productIdToEntity.put(productId, product);
                }

                // Now loop schemes only for subProducts
                for (SchemeAllocation sa : schemeAllocationList) {

                    if (sa.getSchemeCode() == null ||
                            sa.getSchemeCode().equalsIgnoreCase("No scheme assigned")) {
                        continue;
                    }

                    // Check scheme exists in scheme table
                    boolean schemeExists = schemeRepository.existsBySchemeCode(sa.getSchemeCode());

                    if (!schemeExists) {
                        log.warn("Skipping allocation because schemeCode {} does not exist in scheme table",
                                sa.getSchemeCode());
                        continue;
                    }

                    Optional<SubProducts> subProductOpt =
                            subProductsRepository.findBySubProductCodeAndIsVisibleTrue(
                                    sa.getSubProductCode());

                    if (subProductOpt.isEmpty()) {
                        log.warn(
                                "Skipping scheme due to missing subProductCode={}",
                                sa.getSubProductCode()
                        );
                        continue;
                    }

                    SubProducts subProductEntity = subProductOpt.get();

                    // Find matching product from allProducts using productCode
                    Products productEntity = allProducts.stream()
                            .filter(p -> p.getProductCode().equals(sa.getProductCode()))
                            .findFirst()
                            .orElse(null);

                    if (productEntity == null) {
                        continue;
                    }

                    String productId = String.valueOf(productEntity.getId());
                    String subProductId = String.valueOf(subProductEntity.getId());
                    String subProductName = subProductEntity.getSubProductTitle();

                    VendorRouting routing =
                            vendorRoutingRepository.findBySubProduct(subProductEntity);

                    String vendorName = "N/A";
                    if (routing != null && routing.getRoutingPriority1() != null) {
                        vendorName = routing.getRoutingPriority1().getVendorName();
                    }

                    Map<String, Object> subProductMap = new HashMap<>();
                    subProductMap.put("subProductId", subProductId);
                    subProductMap.put("subProductName", subProductName);
                    subProductMap.put("schemeCode", sa.getSchemeCode());
                    subProductMap.put("vendorName", vendorName);

                    // Sub-Product flags
                    subProductMap.put("active", subProductEntity.getIsActive());
                    subProductMap.put("blocked", subProductEntity.getIsBlocked());
                    subProductMap.put("isVisible", subProductEntity.getIsVisible());

                    productSubMap.get(productId).add(subProductMap);
                }

                // Build final product list
                List<Map<String, Object>> productList = new ArrayList<>();

                for (Map.Entry<String, List<Map<String, Object>>> entry : productSubMap.entrySet()) {

                    Products productEntity = productIdToEntity.get(entry.getKey());

                    Map<String, Object> product = new HashMap<>();
                    product.put("productId", entry.getKey());
                    product.put("productName", productEntity.getName());

                    product.put("active", productEntity.getActive());
                    product.put("blocked", productEntity.getBlocked());
                    product.put("isVisible", productEntity.getIsVisible());

                    product.put("subProducts", entry.getValue());

                    productList.add(product);
                }

                responseMap.put("products", productList);
                responseMap.put("role", hierarchyCode);
                break;



            case "RU":
                responseMap.put("role_id", user.getRole_id());
                ImSubRole ruSubRole = imSubRoleRepository.findByMobileNumber(user.getMobileNumber());
                if (ruSubRole != null) {
                    responseMap.put("sub_role_id", ruSubRole.getSubRoleId());
                }
                responseMap.put("role", "RU");

                //  Outlet Name - same as RT
                Retailer ruRetailer = retailerRepository.findByMobileNumber(user.getMobileNumber());
                if (ruRetailer != null && ruRetailer.getOutletAddress() != null && ruRetailer.getOutletAddress().getOutletName() != null) {
                    responseMap.put("outletName", ruRetailer.getOutletAddress().getOutletName());
                } else {
                    responseMap.put("outletName", "N/A");
                }

                //  Products & SubProducts Allocation (Same logic as RT)
                List<SchemeAllocation> ruSchemes = schemeAllocationRepository.findByHierarchyId(user.getHierarchyId());
                Map<String, List<Map<String, String>>> ruProductSubMap = new LinkedHashMap<>();
                Map<String, String> ruProductIdToName = new HashMap<>();

                for (SchemeAllocation sa : ruSchemes) {
                    Optional<Products> productOpt = productsRepository.findByProductCode(sa.getProductCode());
                    Optional<SubProducts> subProductOpt = subProductsRepository.findBySubProductCodeAndIsVisibleTrue(sa.getSubProductCode());

                    if (productOpt.isEmpty() || subProductOpt.isEmpty()) {
                        log.warn("Skipping scheme for RU due to missing product/subproduct");
                        continue;
                    }

                    String productId = String.valueOf(productOpt.get().getId());
                    String productName = productOpt.get().getName();
                    String subProductId = String.valueOf(subProductOpt.get().getId());
                    String subProductName = subProductOpt.get().getSubProductTitle();

                    ruProductIdToName.putIfAbsent(productId, productName);

                    Map<String, String> subProductMap = new HashMap<>();
                    subProductMap.put("subProductId", subProductId);
                    subProductMap.put("subProductName", subProductName);
                    subProductMap.put("schemeCode", sa.getSchemeCode());

                    ruProductSubMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(subProductMap);
                }

                List<Map<String, Object>> ruProductList = new ArrayList<>();
                for (Map.Entry<String, List<Map<String, String>>> entry : ruProductSubMap.entrySet()) {
                    Map<String, Object> product = new HashMap<>();
                    product.put("productId", entry.getKey());
                    product.put("productName", ruProductIdToName.get(entry.getKey()));
                    product.put("subProducts", entry.getValue());
                    ruProductList.add(product);
                }

                responseMap.put("products", ruProductList);

                break;

            case "AU":
                responseMap.put("permissions", user.getPermission());
                responseMap.put("role", "AU");
                break;

            default:
                responseMap.put("role", hierarchyCode);
                break;
        }

        // 9. Log login attempt
        try {
            responseJson = objectMapper.writeValueAsString(
                    new CommonResponse<>(200, "OTP verified successfully", responseMap)
            );
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.info("OTP verification log saved for user ID: {}", user.getHierarchyId());
        } catch (JsonProcessingException e) {
            log.error("Error logging OTP verification for user ID: {}", user.getHierarchyId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Error logging OTP verification", null));
        }

        log.info("OTP verification completed successfully for user ID: {}", user.getHierarchyId());
        return ResponseEntity.ok(new CommonResponse<>(200, "OTP verified successfully", responseMap));
    }

    @PostMapping("/forget-verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() + "&otp=" + loginRequestDto.getOtp();
        String responseJson = "";

        log.info("Forgot-password OTP verification attempt for username: {}", loginRequestDto.getUserName());

        try {
            boolean isVerified = authService.verifyOTP(loginRequestDto);
            CommonResponse<?> commonResponse;

            if (isVerified) {
                log.info("OTP verified successfully for username: {}", loginRequestDto.getUserName());
                commonResponse = new CommonResponse<>(200, "OTP verified successfully", null);
            } else {
                log.warn("OTP verification failed for username: {}", loginRequestDto.getUserName());
                commonResponse = new CommonResponse<>(401, "OTP verification failed", null);
            }

            responseJson = objectMapper.writeValueAsString(commonResponse);
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.debug("Login log saved for username: {}", loginRequestDto.getUserName());

            return ResponseEntity.status(commonResponse.getStatusCode()).body(commonResponse);

        } catch (RuntimeException | JsonProcessingException ex) {
            log.error("Error during OTP verification for username: {}", loginRequestDto.getUserName(), ex);

            CommonResponse<?> errorResponse = new CommonResponse<>(500, ex.getMessage(), null);
            try {
                responseJson = objectMapper.writeValueAsString(errorResponse);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize error response for username: {}", loginRequestDto.getUserName(), e);
            }

            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), ex.getMessage());
            log.debug("Error log saved for username: {}", loginRequestDto.getUserName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName();
        String responseJson = "";

        log.info("OTP resend request received for username: {}", loginRequestDto.getUserName());

        Optional<UserDetails> optionalUser = loginRequestDto.getUserName().matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName())
                : userDetailsRepository.findByEmailId(loginRequestDto.getUserName());

        if (optionalUser.isEmpty()) {
            log.warn("User not found for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();
        log.debug("User found: ID={}, Status={}", user.getHierarchyId(), user.getUserStatus());

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.warn("User account is not active: ID={}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(403, "User account is not active", null));
        }

        String newOtp = OtpUtil.generateOtp();
        user.setOtp(newOtp);
        user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
        userDetailsRepository.save(user);

        log.info("New OTP generated and saved for user ID: {}", user.getHierarchyId());

        // Hook: Send OTP via SMS or Email
        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            log.debug("Sending OTP via SMS to mobile number: {}", user.getMobileNumber());
//            String smsResponse = smsGatewayAPI.sendOTPSms(user.getMobileNumber(), newOtp);
//
//            // Save SMS logs
//            commonFunctions.saveSmsLogs(
//                    LocalDateTime.now(),
//                    "Send OTP to: " + user.getMobileNumber() + ", OTP: " + newOtp,
//                    LocalDateTime.now(),
//                    smsResponse,
//                    null // You can replace null with real IP if available
//            );

        } else {
            log.debug("Sending OTP via Email to email ID: {}", user.getEmailId());
            // emailService.sendOtp(user.getEmail(), newOtp);
        }

        Map<String, String> otpData = new HashMap<>();
        otpData.put("otp", newOtp); // ⚠️ Consider removing this in production

        CommonResponse<Map<String, String>> commonResponse =
                new CommonResponse<>(200, "OTP resent successfully", otpData);

        try {
            responseJson = objectMapper.writeValueAsString(commonResponse);
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.info("OTP resend log saved for user ID: {}", user.getHierarchyId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing OTP resend response for user ID: {}", user.getHierarchyId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Internal error during logging", null));
        }

        log.info("OTP resend completed successfully for user ID: {}", user.getHierarchyId());
        return ResponseEntity.ok(commonResponse);
    }


    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName() +
                "&newPassword=" + loginRequestDto.getNewPassword() +
                "&reEnteredPassword=" + loginRequestDto.getReEnteredPassword();
        String responseJson = "";

        log.info("Password reset request received for username: {}", loginRequestDto.getUserName());

        if (!loginRequestDto.getNewPassword().equals(loginRequestDto.getReEnteredPassword())) {
            log.warn("Password mismatch for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(400, "New password and confirm password do not match", null));
        }

        if (loginRequestDto.getNewPassword().length() < 8) {
            log.warn("Password too short for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(400, "Password must be at least 8 characters long", null));
        }

        Optional<UserDetails> optionalUser = loginRequestDto.getUserName().matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName())
                : userDetailsRepository.findByEmailId(loginRequestDto.getUserName());

        if (optionalUser.isEmpty()) {
            log.warn("User not found for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();
        log.debug("User found: ID={}, Status={}", user.getHierarchyId(), user.getUserStatus());

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.warn("Inactive user attempted password reset: ID={}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(403, "User account is not active", null));
        }

        String hashedPassword = passwordEncoder.encode(loginRequestDto.getNewPassword());
        user.setLoginPassword(hashedPassword);
        userDetailsRepository.save(user);

        log.info("Password updated successfully for user ID: {}", user.getHierarchyId());

        CommonResponse<?> response = new CommonResponse<>(200, "Password updated successfully", null);
        try {
            responseJson = objectMapper.writeValueAsString(response);
            log.debug("Password update response serialized for user ID: {}", user.getHierarchyId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing password update response for user ID: {}", user.getHierarchyId(), e);
            responseJson = "{\"statusCode\":200,\"message\":\"Password updated successfully\",\"data\":null}";
        }

        commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
        log.info("Password reset log saved for user ID: {}", user.getHierarchyId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody LoginRequestDto loginRequestDto) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "username=" + loginRequestDto.getUserName();
        String responseJson = "";

        log.info("Forgot password request received for username: {}", loginRequestDto.getUserName());

        Optional<UserDetails> optionalUser = loginRequestDto.getUserName().matches("\\d{10}")
                ? userDetailsRepository.findByMobileNumber(loginRequestDto.getUserName())
                : userDetailsRepository.findByEmailId(loginRequestDto.getUserName());

        if (optionalUser.isEmpty()) {
            log.warn("User not found for username: {}", loginRequestDto.getUserName());
            return ResponseEntity.ok(new CommonResponse<>(404, "User not found", null));
        }

        UserDetails user = optionalUser.get();
        log.debug("User found: ID={}, Status={}", user.getHierarchyId(), user.getUserStatus());

        if (!"Active".equalsIgnoreCase(user.getUserStatus())) {
            log.warn("Inactive user attempted password recovery: ID={}", user.getHierarchyId());
            return ResponseEntity.ok(new CommonResponse<>(403, "User account is not active", null));
        }

        String otp = OtpUtil.generateOtp();
        user.setOtp(otp);
        user.setOtpExpiryTime(LocalTime.now().plusMinutes(5));
        userDetailsRepository.save(user);

        log.info("OTP generated and saved for user ID: {}", user.getHierarchyId());

        // Optional: Hook for sending OTP
        if (loginRequestDto.getUserName().matches("\\d{10}")) {
            log.debug("Sending OTP via SMS to mobile number: {}", user.getMobileNumber());
//            String smsResponse = smsGatewayAPI.sendOTPSms(user.getMobileNumber(), otp);
//
//            // Save SMS logs
//            commonFunctions.saveSmsLogs(
//                    LocalDateTime.now(),
//                    "Send OTP to: " + user.getMobileNumber() + ", OTP: " + otp,
//                    LocalDateTime.now(),
//                    smsResponse,
//                    null // You can replace null with real IP if available
//            );

        } else {
            log.debug("Sending OTP via Email to email ID: {}", user.getEmailId());
            // emailService.sendOtp(user.getEmail(), otp);
        }

        Map<String, String> data = new HashMap<>();
        data.put("message", "OTP has been sent to your registered mobile number");
        data.put("OTP", otp); // ⚠️ Consider removing this in production

        CommonResponse<Map<String, String>> commonResponse = new CommonResponse<>(200, "OTP generated successfully", data);

        try {
            responseJson = objectMapper.writeValueAsString(commonResponse);
            log.debug("OTP response serialized for user ID: {}", user.getHierarchyId());
        } catch (JsonProcessingException e) {
            log.error("Error serializing OTP response for user ID: {}", user.getHierarchyId(), e);
            responseJson = "{\"statusCode\":200,\"message\":\"OTP generated successfully\",\"data\":{\"message\":\"OTP has been sent to your registered mobile number\"}}";
        }

        commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
        log.info("Forgot password log saved for user ID: {}", user.getHierarchyId());

        return ResponseEntity.ok(commonResponse);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequestDto request) {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";

        log.info("Password change request received for username: {}", request.getUserName());

        try {
            requestJson = objectMapper.writeValueAsString(request);
            log.debug("Request payload: {}", requestJson);

            CommonResponse<?> response = authService.changePassword(request);
            responseJson = objectMapper.writeValueAsString(response);

            log.info("Password change response for username {}: {}", request.getUserName(), response.getMessage());
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.debug("Password change log saved for username: {}", request.getUserName());

            return ResponseEntity.ok(response);

        } catch (RuntimeException | JsonProcessingException ex) {
            log.error("Error during password change for username: {}", request.getUserName(), ex);

            CommonResponse<?> error = new CommonResponse<>(500, ex.getMessage(), null);
            try {
                responseJson = objectMapper.writeValueAsString(error);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize error response for username: {}", request.getUserName(), e);
                responseJson = "{\"statusCode\":500,\"message\":\"Internal error\",\"data\":null}";
            }

            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), ex.getMessage());
            log.debug("Error log saved for username: {}", request.getUserName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    @PostMapping("/change-password-admin")
    public ResponseEntity<?> changePasswordAdmin(@RequestBody ChangePasswordRequestDto request) {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestJson = "";
        String responseJson = "";

        log.info("Password change request received for username: {}", request.getUserName());

        try {
            requestJson = objectMapper.writeValueAsString(request);
            log.debug("Request payload: {}", requestJson);

            CommonResponse<?> response = authService.changePasswordadmin(request);
            responseJson = objectMapper.writeValueAsString(response);

            log.info("Password change response for username {}: {}", request.getUserName(), response.getMessage());
            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), null);
            log.debug("Password change log saved for username: {}", request.getUserName());

            return ResponseEntity.ok(response);

        } catch (RuntimeException | JsonProcessingException ex) {
            log.error("Error during password change for username: {}", request.getUserName(), ex);

            CommonResponse<?> error = new CommonResponse<>(500, ex.getMessage(), null);
            try {
                responseJson = objectMapper.writeValueAsString(error);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize error response for username: {}", request.getUserName(), e);
                responseJson = "{\"statusCode\":500,\"message\":\"Internal error\",\"data\":null}";
            }

            commonFunctions.saveLoginLogs(requestJson, requestTime, responseJson, LocalDateTime.now(), ex.getMessage());
            log.debug("Error log saved for username: {}", request.getUserName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }


    @PostMapping("/add")
    public String addData(@RequestParam String id) {

        return "String";
    }
}

