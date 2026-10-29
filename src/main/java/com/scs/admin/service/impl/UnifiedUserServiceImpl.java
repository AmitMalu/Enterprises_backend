package com.scs.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.dto.*;
import com.scs.admin.entities.*;
import com.scs.admin.exception.CustomValidationException;
import com.scs.admin.repositories.*;
import com.scs.admin.request.HierarchyRegistrationRequest;
import com.scs.admin.service.HierarchyAccessService;
import com.scs.admin.service.UnifiedUserService;
import com.scs.admin.util.CommonMethods;
import com.scs.admin.util.IdGenerator;
import com.scs.admin.util.TokenUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Unified service — routes every operation to admin_users or white_label_users
 * by extracting the type from the parentAccount or the record id.
 *
 * Extraction rules
 * ─────────────────
 * parentAccount  "MAY-AD-001"  → split("-")[1] = "AD" → admin_users
 * parentAccount  "MAY-WL-001"  → split("-")[1] = "WL" → white_label_users
 *
 * record id      "MAY-AU-0001"  → contains "-AU-"  → admin_users
 * record id      "MAY-WAU-0001" → contains "-WAU-" → white_label_users
 *
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class UnifiedUserServiceImpl implements UnifiedUserService {

    private static final String TYPE_AD  = "AD";
    private static final String TYPE_WL  = "WL";
    private static final String TYPE_AU = "AU";

    // hierarchy codes stored in UserDetails
    private static final String CODE_AD  = "AU";
    private static final String CODE_WL  = "WLAU";

    @Autowired private IdGenerator idGenerator;
    @Autowired private AdminRepository adminRepository;
    @Autowired private AdminUserRepository adminUserRepository;
    @Autowired private WhiteLabelRepository whiteLabelRepository;
    @Autowired private WhiteLabelUserRepository whiteLabelUserRepository;
    @Autowired private UserDetailsRepository userDetailsRepository;
    @Autowired private CommonFunctions commonFunctions;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PhoneNumberValidationService phoneNumberValidationService;
    @Autowired private Validator validator;
    @Autowired private TokenUtil tokenUtil;
    @Autowired private CommonMethods commonMethods;
    @Autowired private AccessHierarchyPathRepository accessHierarchyPathRepository;
    @Autowired private HierarchyAccessService hierarchyAccessService;

    // ─────────────────────────────────────────────────────────────────────────
    // ROUTING HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts "AD" or "WL" from parentAccount e.g. "MAY-WL-001" → "WL"
     */
    private String typeFromParentAccount(String parentAccount) {
        if (parentAccount == null || !parentAccount.contains("-")) {
            throw new IllegalArgumentException(
                    "Invalid parentAccount format. Expected format like 'MAY-AD-001' or 'MAY-WL-001'");
        }
        String[] parts = parentAccount.split("-");
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    "Invalid parentAccount format. Expected format like 'MAY-AD-001' or 'MAY-WL-001'");
        }
        String segment = parts[1].toUpperCase();
        if (TYPE_AD.equals(segment) || TYPE_WL.equals(segment)) {
            return segment;
        }
        throw new IllegalArgumentException(
                "Cannot determine user type from parentAccount '" + parentAccount
                + "'. Segment[1] must be 'AD' or 'WL'.");
    }

    /**
     * Extracts "AD" or "WL" from a record id.
     * id format from IdGenerator:  "MAY-AU-0001"  or  "MAY-WLAU-0001"
     * "-WLAU-" → WL,  "-AU-" → AD
     */
    private String typeFromId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        // WAU check must come before AU because "WAU" contains "AU"
        if (id.contains("-WAU-") || id.contains("-WLAU-")) return TYPE_WL;
        if (id.contains("-AU-"))  return TYPE_AD;
        throw new IllegalArgumentException(
                "Cannot determine user type from id '" + id
                + "'. Expected id containing '-AU-' (Admin User) or '-WLAU-' (White Label User).");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request  = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            // Bean validation
            Set<ConstraintViolation<UserRequestDTO>> violations = validator.validate(dto);
            List<FieldErrorDetailsDTO> errors = violations.stream()
                    .map(v -> new FieldErrorDetailsDTO(
                            v.getPropertyPath().toString(), v.getMessage()))
                    .collect(Collectors.toList());

            // Phone validation
            try {
                phoneNumberValidationService.validatePhoneNumber(dto.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetailsDTO("mobileNumber", ex.getMessage()));
            }

            if (!errors.isEmpty()) {
                throw new CustomValidationException(errors);
            }

            request = objectMapper.writeValueAsString(dto);
            log.info("createUser request: {}", request);

            // Derive type and abbreviation from parentAccount
            String userType = typeFromParentAccount(dto.getParentAccount());
            String abbreviation = dto.getParentAccount().split("-")[0];
            dto.setAbbreviation(abbreviation);

            log.info("Derived userType={} from parentAccount={}", userType, dto.getParentAccount());

            UserResponseDTO result;
            if (TYPE_AD.equals(userType)) {
                result = createAdminUser(dto);
            } else {
                result = createWhiteLabelUser(dto);
            }

            response = objectMapper.writeValueAsString(result);
            responseDateTime = LocalDateTime.now();
            return result;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("createUser failed: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveScsLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }

    private UserResponseDTO createAdminUser(UserRequestDTO dto) {

        // Duplicate check — mobile
        // (AdminUserRepository has no existsBy methods; DB unique constraint handles it)
        // Add existsByMobileNumber to AdminUserRepository if you want early validation.

        dto.setId(idGenerator.generateHierarchyEntityId("Admin User", TYPE_AD));
        log.debug("Generated AdminUser id: {}", dto.getId());

        // Validate parent admin exists
        adminRepository.findById(dto.getParentAccount())
                .orElseThrow(() -> new RuntimeException(
                        "Admin not found with id: " + dto.getParentAccount()));

        AdminUser entity = new AdminUser();
        entity.setId(dto.getId());
        entity.setParentAccount(dto.getParentAccount());
        entity.setAbbreviation(TYPE_AU);
        entity.setName(dto.getName());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setEmailId(dto.getEmailId());
        entity.setEmailVerified(dto.getEmailVerified());
        entity.setDob(dto.getDob());
        entity.setAdd(mapAddress(dto.getAdd()));
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedByIPAddress(dto.getCreatedByIPAddress());
        entity.setPermission(joinPermissions(dto.getPermissions()));

        AdminUser saved = adminUserRepository.save(entity);
        log.info("AdminUser saved: {}", saved.getId());

        hierarchyAccessService.registerHierarchyNode(
                HierarchyRegistrationRequest
                        .builder()
                        .hierarchyId(
                                saved.getId()
                        )
                        .hierarchyType(
                                CODE_AD
                        )
                        .parentHierarchyId(
                                saved.getParentAccount()
                        )
                        .build()
        );

        saveUserDetails(saved.getId(), saved.getParentAccount(), saved.getMobileNumber(), saved.getUsername(),
                saved.getPassword(), saved.getEmailId(), saved.getCreatedBy(),
                saved.getPermission(), CODE_AD);

        return mapAdminToResponse(saved);
    }

    private UserResponseDTO createWhiteLabelUser(UserRequestDTO dto) {

        // Duplicate checks
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        if (whiteLabelUserRepository.existsByMobileNumber(dto.getMobileNumber()))
            errors.add(new FieldErrorDetailsDTO("mobileNumber", "Mobile number already registered"));
        if (dto.getUsername() != null && whiteLabelUserRepository.existsByUsername(dto.getUsername()))
            errors.add(new FieldErrorDetailsDTO("username", "Username already taken"));
        if (dto.getEmailId() != null && whiteLabelUserRepository.existsByEmailId(dto.getEmailId()))
            errors.add(new FieldErrorDetailsDTO("emailId", "Email already registered"));
        if (!errors.isEmpty()) throw new CustomValidationException(errors);

        dto.setId(idGenerator.generateHierarchyEntityId("White Label User", dto.getAbbreviation()));
        log.debug("Generated WhiteLabelUser id: {}", dto.getId());

        // Validate parent white label exists and is admin
        WhiteLabels wl = whiteLabelRepository.findById(dto.getParentAccount())
                .orElseThrow(() -> new RuntimeException(
                        "White Label not found: " + dto.getParentAccount()));
        if (!Boolean.TRUE.equals(wl.getIsAdmin())) {
            throw new RuntimeException("Only a WL Admin account can create White Label Users");
        }

        WhiteLabelUser entity = new WhiteLabelUser();
        entity.setId(dto.getId());
        entity.setParentAccount(dto.getParentAccount());
        entity.setAbbreviation(dto.getAbbreviation());
        entity.setName(dto.getName());
        entity.setMobileNumber(dto.getMobileNumber());
        entity.setUsername(dto.getUsername());
        entity.setPassword(dto.getPassword());
        entity.setEmailId(dto.getEmailId());
        entity.setEmailVerified(dto.getEmailVerified());
        entity.setDob(dto.getDob());
        entity.setAdd(mapAddress(dto.getAdd()));
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setCreatedByIPAddress(dto.getCreatedByIPAddress());
        entity.setPermission(joinPermissions(dto.getPermissions()));

        WhiteLabelUser saved = whiteLabelUserRepository.save(entity);
        log.info("WhiteLabelUser saved: {}", saved.getId());

        hierarchyAccessService.registerHierarchyNode(
                HierarchyRegistrationRequest
                        .builder()
                        .hierarchyId(
                                saved.getId()
                        )
                        .hierarchyType(
                                CODE_WL
                        )
                        .parentHierarchyId(
                                saved.getParentAccount()
                        )
                        .build()
        );

        saveUserDetails(saved.getId(), saved.getParentAccount(), saved.getMobileNumber(), saved.getUsername(),
                saved.getPassword(), saved.getEmailId(), saved.getCreatedBy(),
                saved.getPermission(), CODE_WL);

        return mapWhiteLabelToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<UserResponseDTO> getAllUsers(
            int page,
            int size,
            String id,
            String name,
            String mobileNumber,
            String username,
            String emailId,
            Boolean status,
            LocalDate dob,
            LocalDate createdOn,
            String createdBy,
            LocalDate lastEditedOn,
            String editedBy
    ) throws JsonProcessingException {

        long startTime = System.currentTimeMillis();
        LocalDateTime requestDateTime = LocalDateTime.now();

        String loginHierarchyId = tokenUtil.extractHierarchyId();

        log.info("[GET ALL USERS] START | loginHierarchy={} | page={} | size={} | filters=[id={}, name={}, mobile={}, username={}, email={}, status={}, dob={}, createdOn={}, createdBy={}, lastEditedOn={}, editedBy={}]",
                loginHierarchyId, page, size, id, name, mobileNumber, username, emailId,
                status, dob, createdOn, createdBy, lastEditedOn, editedBy);

        String request = "getAllUsers hierarchy=" + loginHierarchyId + " page=" + page + " size=" + size;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            String loginType = commonMethods.extractHierarchyType(loginHierarchyId);
            log.debug("[GET ALL USERS] Login hierarchy type: {}", loginType);

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
            log.debug("[GET ALL USERS] Pageable created | page={} | size={} | sort=createdOn DESC", page, size);

            Page<UserResponseDTO> result;

            /*
             * ADMIN ACCESS (AD)
             */
            if ("AD".equalsIgnoreCase(loginType)) {
                log.info("[GET ALL USERS] ADMIN ACCESS | loginHierarchy={}", loginHierarchyId);

                log.debug("[GET ALL USERS] Building Admin specification with filters: [id={}, name={}, mobile={}, username={}, email={}, status={}, dob={}, createdOn={}, createdBy={}, lastEditedOn={}, editedBy={}]",
                        id, name, mobileNumber, username, emailId, status, dob, createdOn, createdBy, lastEditedOn, editedBy);

                long specStartTime = System.currentTimeMillis();
                Specification<AdminUser> spec = buildAdminSpec(
                        id, name, mobileNumber, username, emailId,
                        status, dob, createdOn, createdBy, lastEditedOn, editedBy
                );
                log.debug("[GET ALL USERS] Admin specification built in {}ms", System.currentTimeMillis() - specStartTime);

                long dbStartTime = System.currentTimeMillis();
                result = adminUserRepository
                        .findAll(spec, pageable)
                        .map(this::mapAdminToResponse);
                log.info("[GET ALL USERS] Admin query executed | totalElements={} | totalPages={} | time={}ms",
                        result.getTotalElements(), result.getTotalPages(), System.currentTimeMillis() - dbStartTime);

            }
            /*
             * SUBTREE ACCESS (WL, WLAU, etc.)
             */
            else {
                log.info("[GET ALL USERS] SUBTREE ACCESS | loginHierarchy={} | loginType={}",
                        loginHierarchyId, loginType);

                long dbStartTime = System.currentTimeMillis();
                List<String> allowedHierarchyIds = accessHierarchyPathRepository
                        .findDescendantsByAncestor(loginHierarchyId);
                log.info("[GET ALL USERS] Fetching descendants | loginHierarchy={} | found={} | time={}ms",
                        loginHierarchyId,
                        allowedHierarchyIds != null ? allowedHierarchyIds.size() : 0,
                        System.currentTimeMillis() - dbStartTime);

                if (allowedHierarchyIds == null || allowedHierarchyIds.isEmpty()) {
                    log.warn("[GET ALL USERS] No descendants found for loginHierarchy={} | returning empty page",
                            loginHierarchyId);
                    Page<UserResponseDTO> emptyPage = Page.empty(pageable);
                    log.info("[GET ALL USERS] RESULT | hierarchy={} | totalElements=0 | page={} | size={} | time={}ms",
                            loginHierarchyId, page, size, System.currentTimeMillis() - startTime);
                    return emptyPage;
                }

                log.debug("[GET ALL USERS] Allowed hierarchy IDs (first 10): {}",
                        allowedHierarchyIds.stream().limit(10).collect(Collectors.toList()));

                log.debug("[GET ALL USERS] Building WhiteLabel specification with filters: [id={}, name={}, mobile={}, username={}, email={}, status={}, dob={}, createdOn={}, createdBy={}, lastEditedOn={}, editedBy={}]",
                        id, name, mobileNumber, username, emailId, status, dob, createdOn, createdBy, lastEditedOn, editedBy);

                long specStartTime = System.currentTimeMillis();
                Specification<WhiteLabelUser> spec = buildWLSpec(
                        id, name, mobileNumber, username, emailId,
                        status, dob, createdOn, createdBy, lastEditedOn, editedBy
                )
                        .and((root, query, cb) -> root.get("id").in(allowedHierarchyIds));
                log.debug("[GET ALL USERS] WhiteLabel specification built in {}ms", System.currentTimeMillis() - specStartTime);

                dbStartTime = System.currentTimeMillis();
                result = whiteLabelUserRepository
                        .findAll(spec, pageable)
                        .map(this::mapWhiteLabelToResponse);
                log.info("[GET ALL USERS] WhiteLabel query executed | totalElements={} | totalPages={} | time={}ms",
                        result.getTotalElements(), result.getTotalPages(), System.currentTimeMillis() - dbStartTime);
            }

            // Serialize response
            long serializeStartTime = System.currentTimeMillis();
            response = objectMapper.writeValueAsString(result.getContent());
            log.debug("[GET ALL USERS] Response serialized | size={} | time={}ms",
                    result.getContent().size(), System.currentTimeMillis() - serializeStartTime);

            responseDateTime = LocalDateTime.now();

            log.info("[GET ALL USERS] SUCCESS | loginHierarchy={} | totalElements={} | totalPages={} | currentPage={} | pageSize={} | time={}ms",
                    loginHierarchyId, result.getTotalElements(), result.getTotalPages(),
                    result.getNumber(), result.getSize(), System.currentTimeMillis() - startTime);

            return result;

        } catch (Exception ex) {
            log.error("[GET ALL USERS] ERROR | loginHierarchy={} | page={} | size={} | filters=[id={}, name={}, mobile={}] | error={} | time={}ms",
                    loginHierarchyId, page, size, id, name, mobileNumber,
                    ex.getMessage(), System.currentTimeMillis() - startTime, ex);

            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error("[GET ALL USERS] Exception details", ex);
            throw ex;

        } finally {
            // Save audit logs
            try {
                long auditStartTime = System.currentTimeMillis();
                commonFunctions.saveScsLogs(request, requestDateTime, null, response, responseDateTime);
                log.debug("[GET ALL USERS] Audit logs saved | time={}ms", System.currentTimeMillis() - auditStartTime);
            } catch (Exception e) {
                log.error("[GET ALL USERS] Failed to save audit logs", e);
            }

            log.info("[GET ALL USERS] COMPLETE | loginHierarchy={} | totalTime={}ms",
                    loginHierarchyId, System.currentTimeMillis() - startTime);
        }
    }

// ==================== PRIVATE HELPER METHODS FOR LOGGING ====================

    /**
     * Logs the filter parameters for debugging
     */
    private void logFilterParameters(String method, String id, String name, String mobileNumber,
                                     String username, String emailId, Boolean status, LocalDate dob,
                                     LocalDate createdOn, String createdBy, LocalDate lastEditedOn, String editedBy) {

        if (log.isDebugEnabled()) {
            log.debug("[{}] Filters: id={}, name={}, mobile={}, username={}, email={}, status={}, dob={}, createdOn={}, createdBy={}, lastEditedOn={}, editedBy={}",
                    method, id, name, mobileNumber, username, emailId, status,
                    dob, createdOn, createdBy, lastEditedOn, editedBy);
        }
    }

    /**
     * Logs query execution details
     */
    private void logQueryExecution(String method, String repositoryType, long totalElements,
                                   int totalPages, long executionTime) {
        log.info("[{}] {} query executed | totalElements={} | totalPages={} | time={}ms",
                method, repositoryType, totalElements, totalPages, executionTime);
    }

    /**
     * Logs pagination details
     */
    private void logPaginationDetails(String method, int page, int size, String sort) {
        log.debug("[{}] Pagination: page={}, size={}, sort={}", method, page, size, sort);
    }

    /**
     * Logs empty result scenarios
     */
    private void logEmptyResult(String method, String hierarchyId, String reason) {
        log.warn("[{}] Empty result | hierarchyId={} | reason={}", method, hierarchyId, reason);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserResponseDTO getUserById(String id) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "getUserById id=" + id;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("======= START: getUserById =======");
        log.info("Request: {}", request);
        log.info("Request timestamp: {}", requestDateTime);

        try {
            // Log the ID being searched
            log.debug("Searching for user with ID: {}", id);

            String userType = typeFromId(id);
            log.info("User type determined: {} for ID: {}", userType, id);

            UserResponseDTO dto;

            if (TYPE_AD.equals(userType)) {
                log.debug("Fetching Admin User from repository with ID: {}", id);

                AdminUser user = adminUserRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("Admin User not found with ID: {}", id);
                            return new RuntimeException("Admin User not found with id: " + id);
                        });

                log.debug("Admin User found: {}", user.getUsername());
                log.info("Mapping Admin User to response DTO");
                dto = mapAdminToResponse(user);

            } else {
                log.debug("Fetching White Label User from repository with ID: {}", id);

                WhiteLabelUser user = whiteLabelUserRepository.findById(id)
                        .orElseThrow(() -> {
                            log.error("White Label User not found with ID: {}", id);
                            return new RuntimeException("White Label User not found with id: " + id);
                        });

                log.debug("White Label User found: {}", user.getUsername());
                log.info("Mapping White Label User to response DTO");
                dto = mapWhiteLabelToResponse(user);
            }

            // Log successful retrieval
            log.info("User retrieved successfully - ID: {}, Type: {}, Name: {}",
                    dto.getId(), dto.getUserType(), dto.getName());

            response = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();

            log.debug("Response JSON: {}", response);
            log.info("Response timestamp: {}", responseDateTime);
            log.info("Total processing time: {} ms",
                    java.time.Duration.between(requestDateTime, responseDateTime).toMillis());
            log.info("======= END: getUserById - SUCCESS =======");

            return dto;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error("======= getUserById FAILED =======");
            log.error("User ID: {}", id);
            log.error("Error Type: {}", ex.getClass().getSimpleName());
            log.error("Error Message: {}", ex.getMessage());
            log.error("Timestamp: {}", responseDateTime);
            log.error("Stack trace: ", ex);

            throw ex;

        } finally {
            // Always log the request/response details
            log.debug("Saving SCS logs for request: {}", request);
            commonFunctions.saveScsLogs(request, requestDateTime, null, response, responseDateTime);
            log.debug("SCS logs saved successfully");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public UserResponseDTO updateUser(String id, UserRequestDTO dto) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request  = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            request = objectMapper.writeValueAsString(dto);
            String userType = typeFromId(id);
            log.info("updateUser id={} userType={}", id, userType);

            UserResponseDTO result;
            if (TYPE_AD.equals(userType)) {
                result = updateAdminUser(id, dto);
            } else {
                result = updateWhiteLabelUser(id, dto);
            }

            response = objectMapper.writeValueAsString(result);
            responseDateTime = LocalDateTime.now();
            return result;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("updateUser failed id={}: {}", id, ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveScsLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }

    private UserResponseDTO updateAdminUser(String id, UserRequestDTO dto) {
        AdminUser existing = adminUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Admin User not found with id: " + id));

        existing.setName(dto.getName());
        existing.setMobileNumber(dto.getMobileNumber());
        existing.setUsername(dto.getUsername());
        existing.setPassword(dto.getPassword());
        existing.setEmailId(dto.getEmailId());
        existing.setEmailVerified(dto.getEmailVerified());
        existing.setDob(dto.getDob());
        existing.setAdd(mapAddress(dto.getAdd()));
        existing.setEditedBy(dto.getEditedBy());
        existing.setCreatedByIPAddress(dto.getCreatedByIPAddress());
        if (dto.getPermissions() != null)
            existing.setPermission(joinPermissions(dto.getPermissions()));

        AdminUser saved = adminUserRepository.save(existing);
        log.info("AdminUser updated: {}", saved.getId());

        updateUserDetails(saved.getId(), saved.getMobileNumber(), saved.getUsername(),
                saved.getPassword(), saved.getEmailId(), saved.getEditedBy(), saved.getPermission());

        return mapAdminToResponse(saved);
    }

    private UserResponseDTO updateWhiteLabelUser(String id, UserRequestDTO dto) {
        WhiteLabelUser existing = whiteLabelUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "White Label User not found with id: " + id));

        existing.setName(dto.getName());
        existing.setMobileNumber(dto.getMobileNumber());
        existing.setUsername(dto.getUsername());
        existing.setPassword(dto.getPassword());
        existing.setEmailId(dto.getEmailId());
        existing.setEmailVerified(dto.getEmailVerified());
        existing.setDob(dto.getDob());
        existing.setAdd(mapAddress(dto.getAdd()));
        existing.setEditedBy(dto.getEditedBy());
        existing.setCreatedByIPAddress(dto.getCreatedByIPAddress());
        if (dto.getPermissions() != null)
            existing.setPermission(joinPermissions(dto.getPermissions()));

        WhiteLabelUser saved = whiteLabelUserRepository.save(existing);
        log.info("WhiteLabelUser updated: {}", saved.getId());

        updateUserDetails(saved.getId(), saved.getMobileNumber(), saved.getUsername(),
                saved.getPassword(), saved.getEmailId(), saved.getEditedBy(), saved.getPermission());

        return mapWhiteLabelToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // USERDETAILS HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private void saveUserDetails(String hierarchyId, String parentAccount, String mobile, String username,
                                  String password, String email, String createdBy,
                                  String permission, String hierarchyCode) {
        UserDetails u = new UserDetails();
        u.setHierarchyId(hierarchyId);
        u.setParentAccount(parentAccount);
        u.setHierarchyCode(hierarchyCode);
        u.setMobileNumber(mobile);
        u.setLoginUserName(username);
        u.setLoginPassword(password);
        u.setEmailId(email);
        u.setCreatedBy(createdBy);
        u.setEditedBy(createdBy);
        u.setUserStatus("Active");
        u.setUserLoginStatus(false);
        u.setChannel("web");
        u.setPermission(permission);
        userDetailsRepository.save(u);
        log.info("UserDetails saved for hierarchyId={}", hierarchyId);
    }

    private void updateUserDetails(String hierarchyId, String mobile, String username,
                                    String password, String email, String editedBy,
                                    String permission) {
        UserDetails u = userDetailsRepository.findByHierarchyId(hierarchyId)
                .orElseThrow(() -> new RuntimeException(
                        "UserDetails not found for hierarchyId: " + hierarchyId));
        u.setMobileNumber(mobile);
        u.setLoginUserName(username);
        u.setLoginPassword(password);
        u.setEmailId(email);
        u.setEditedBy(editedBy);
        u.setPermission(permission);
        userDetailsRepository.save(u);
        log.info("UserDetails updated for hierarchyId={}", hierarchyId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SPECIFICATIONS
    // ─────────────────────────────────────────────────────────────────────────

    private Specification<AdminUser> buildAdminSpec(
            String id, String name, String mobileNumber, String username,
            String emailId, Boolean status, LocalDate dob,
            LocalDate createdOn, String createdBy, LocalDate lastEditedOn, String editedBy) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (id != null && !id.isBlank())
                p.add(cb.like(cb.lower(root.get("id")), "%" + id.toLowerCase() + "%"));
            if (name != null && !name.isBlank())
                p.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (mobileNumber != null && !mobileNumber.isBlank())
                p.add(cb.like(root.get("mobileNumber"), "%" + mobileNumber + "%"));
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            if (emailId != null && !emailId.isBlank())
                p.add(cb.like(cb.lower(root.get("emailId")), "%" + emailId.toLowerCase() + "%"));
            if (status != null)
                p.add(cb.equal(root.get("status"), status));
            if (dob != null)
                p.add(cb.equal(root.get("dob"), dob));
            if (createdOn != null)
                p.add(cb.equal(root.get("createdOn").as(LocalDate.class), createdOn));
            if (createdBy != null && !createdBy.isBlank())
                p.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            if (lastEditedOn != null)
                p.add(cb.equal(root.get("lastEditedOn").as(LocalDate.class), lastEditedOn));
            if (editedBy != null && !editedBy.isBlank())
                p.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    private Specification<WhiteLabelUser> buildWLSpec(
            String id, String name, String mobileNumber, String username,
            String emailId, Boolean status, LocalDate dob,
            LocalDate createdOn, String createdBy, LocalDate lastEditedOn, String editedBy) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (id != null && !id.isBlank())
                p.add(cb.like(cb.lower(root.get("id")), "%" + id.toLowerCase() + "%"));
            if (name != null && !name.isBlank())
                p.add(cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            if (mobileNumber != null && !mobileNumber.isBlank())
                p.add(cb.like(root.get("mobileNumber"), "%" + mobileNumber + "%"));
            if (username != null && !username.isBlank())
                p.add(cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%"));
            if (emailId != null && !emailId.isBlank())
                p.add(cb.like(cb.lower(root.get("emailId")), "%" + emailId.toLowerCase() + "%"));
            if (status != null)
                p.add(cb.equal(root.get("status"), status));
            if (dob != null)
                p.add(cb.equal(root.get("dob"), dob));
            if (createdOn != null)
                p.add(cb.equal(root.get("createdOn").as(LocalDate.class), createdOn));
            if (createdBy != null && !createdBy.isBlank())
                p.add(cb.like(cb.lower(root.get("createdBy")), "%" + createdBy.toLowerCase() + "%"));
            if (lastEditedOn != null)
                p.add(cb.equal(root.get("lastEditedOn").as(LocalDate.class), lastEditedOn));
            if (editedBy != null && !editedBy.isBlank())
                p.add(cb.like(cb.lower(root.get("editedBy")), "%" + editedBy.toLowerCase() + "%"));
            return cb.and(p.toArray(new Predicate[0]));
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────────────────────────────────

    private UserResponseDTO mapAdminToResponse(AdminUser u) {

        Optional<UserDetails> userDetails = userDetailsRepository.findByHierarchyId(u.getId());

        return UserResponseDTO.builder()
                .userType(TYPE_AD)
                .id(u.getId())
                .parentAccount(u.getParentAccount())
                .abbreviation(u.getAbbreviation())
                .name(u.getName())
                .mobileNumber(u.getMobileNumber())
                .username(u.getUsername())
                .emailId(u.getEmailId())
                .emailVerified(u.getEmailVerified())
                .dob(u.getDob())
                .add(mapToAddressDTO(u.getAdd()))
                .status(u.getStatus())
                .createdOn(u.getCreatedOn())
                .createdBy(u.getCreatedBy())
                .lastEditedOn(u.getLastEditedOn())
                .editedBy(u.getEditedBy())
                .permission(userDetails.get().getPermission())
                .build();
    }

    private UserResponseDTO mapWhiteLabelToResponse(WhiteLabelUser u) {

        log.debug("======= START: mapWhiteLabelToResponse =======");
        log.debug("Mapping WhiteLabelUser with ID: {}", u.getId());

        try {
            // Log user details before mapping
            log.debug("WhiteLabelUser details - ID: {}, Name: {}, Username: {}, Status: {}",
                    u.getId(), u.getName(), u.getUsername(), u.getStatus());

            Optional<UserDetails> userDetails = userDetailsRepository.findByHierarchyId(u.getId());

            if (userDetails.isPresent()) {
                log.debug("UserDetails found for hierarchy ID: {}", u.getId());
                log.debug("User permissions: {}", userDetails.get().getPermission());
            } else {
                log.warn("No UserDetails found for hierarchy ID: {}", u.getId());
            }

            UserResponseDTO dto = UserResponseDTO.builder()
                    .userType(TYPE_WL)
                    .id(u.getId())
                    .parentAccount(u.getParentAccount())
                    .abbreviation(u.getAbbreviation())
                    .name(u.getName())
                    .mobileNumber(u.getMobileNumber())
                    .username(u.getUsername())
                    .emailId(u.getEmailId())
                    .emailVerified(u.getEmailVerified())
                    .dob(u.getDob())
                    .add(mapToAddressDTO(u.getAdd()))
                    .status(u.getStatus())
                    .createdOn(u.getCreatedOn())
                    .createdBy(u.getCreatedBy())
                    .lastEditedOn(u.getLastEditedOn())
                    .editedBy(u.getEditedBy())
                    .permission(userDetails.map(UserDetails::getPermission).orElse(null))
                    .build();

            log.debug("DTO built successfully for user: {}", u.getId());
            log.debug("DTO details - UserType: {}, Name: {}, Email: {}",
                    dto.getUserType(), dto.getName(), dto.getEmailId());

            // Log if any fields are null
            if (dto.getPermission() == null) {
                log.warn("Permission is null for user ID: {}", u.getId());
            }
            if (dto.getAdd() == null) {
                log.warn("Address is null for user ID: {}", u.getId());
            }

            log.debug("======= END: mapWhiteLabelToResponse - SUCCESS =======");
            return dto;

        } catch (Exception ex) {
            log.error("======= mapWhiteLabelToResponse FAILED =======");
            log.error("User ID: {}", u.getId());
            log.error("Error: ", ex);
            throw ex;
        }
    }
    private AddressDTO mapToAddressDTO(Address a) {
        if (a == null) return null;
        AddressDTO dto = new AddressDTO();
        dto.setAddress(a.getAddress());
        dto.setCity(a.getCity());
        dto.setState(a.getState());
        dto.setZipCode(a.getZipCode());
        return dto;
    }

    private Address mapAddress(AddressDTO dto) {
        if (dto == null) return null;
        Address a = new Address();
        a.setAddress(dto.getAddress());
        a.setCity(dto.getCity());
        a.setState(dto.getState());
        a.setZipCode(dto.getZipCode());
        return a;
    }

    private String joinPermissions(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) return "";
        return String.join(",", permissions);
    }
}
