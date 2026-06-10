package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.*;
import com.im.admin.service.UnifiedUserService;
import com.im.admin.util.IdGenerator;
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
     * id format from IdGenerator:  "MAY-AU-0001"  or  "MAY-WAU-0001"
     * "-WAU-" → WL,  "-AU-" → AD
     */
    private String typeFromId(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        // WAU check must come before AU because "WAU" contains "AU"
        if (id.contains("-WAU-")) return TYPE_WL;
        if (id.contains("-AU-"))  return TYPE_AD;
        throw new IllegalArgumentException(
                "Cannot determine user type from id '" + id
                + "'. Expected id containing '-AU-' (Admin User) or '-WAU-' (White Label User).");
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
            String userType    = typeFromParentAccount(dto.getParentAccount());
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
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }

    private UserResponseDTO createAdminUser(UserRequestDTO dto) {

        // Duplicate check — mobile
        // (AdminUserRepository has no existsBy methods; DB unique constraint handles it)
        // Add existsByMobileNumber to AdminUserRepository if you want early validation.

        dto.setId(idGenerator.generateHierarchyEntityId("Admin User", CODE_AD));
        log.debug("Generated AdminUser id: {}", dto.getId());

        // Validate parent admin exists
        adminRepository.findById(dto.getParentAccount())
                .orElseThrow(() -> new RuntimeException(
                        "Admin not found with id: " + dto.getParentAccount()));

        AdminUser entity = new AdminUser();
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

        AdminUser saved = adminUserRepository.save(entity);
        log.info("AdminUser saved: {}", saved.getId());

        saveUserDetails(saved.getId(), saved.getMobileNumber(), saved.getUsername(),
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

        saveUserDetails(saved.getId(), saved.getMobileNumber(), saved.getUsername(),
                saved.getPassword(), saved.getEmailId(), saved.getCreatedBy(),
                saved.getPermission(), CODE_WL);

        return mapWhiteLabelToResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET ALL
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public Page<UserResponseDTO> getAllUsers(
            int page, int size, String parentAccount,
            String id, String name, String mobileNumber, String username,
            String emailId, Boolean status, LocalDate dob,
            LocalDate createdOn, String createdBy,
            LocalDate lastEditedOn, String editedBy
    ) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request  = "getAllUsers parentAccount=" + parentAccount + " page=" + page + " size=" + size;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            String userType = typeFromParentAccount(parentAccount);
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());

            Page<UserResponseDTO> result;

            if (TYPE_AD.equals(userType)) {
                Specification<AdminUser> spec = buildAdminSpec(
                        id, name, mobileNumber, username, emailId,
                        status, dob, createdOn, createdBy, lastEditedOn, editedBy);
                result = adminUserRepository.findAll(spec, pageable)
                        .map(this::mapAdminToResponse);
            } else {
                Specification<WhiteLabelUser> spec = buildWLSpec(
                        id, name, mobileNumber, username, emailId,
                        status, dob, createdOn, createdBy, lastEditedOn, editedBy);
                result = whiteLabelUserRepository.findAll(spec, pageable)
                        .map(this::mapWhiteLabelToResponse);
            }

            response = objectMapper.writeValueAsString(result.getContent());
            responseDateTime = LocalDateTime.now();
            return result;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("getAllUsers failed: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET BY ID
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserResponseDTO getUserById(String id) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request  = "getUserById id=" + id;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            String userType = typeFromId(id);
            UserResponseDTO dto;

            if (TYPE_AD.equals(userType)) {
                AdminUser user = adminUserRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException(
                                "Admin User not found with id: " + id));
                dto = mapAdminToResponse(user);
            } else {
                WhiteLabelUser user = whiteLabelUserRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException(
                                "White Label User not found with id: " + id));
                dto = mapWhiteLabelToResponse(user);
            }

            response = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();
            return dto;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("getUserById failed id={}: {}", id, ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
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
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
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

    private void saveUserDetails(String hierarchyId, String mobile, String username,
                                  String password, String email, String createdBy,
                                  String permission, String hierarchyCode) {
        UserDetails u = new UserDetails();
        u.setHierarchyId(hierarchyId);
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
                .build();
    }

    private UserResponseDTO mapWhiteLabelToResponse(WhiteLabelUser u) {
        return UserResponseDTO.builder()
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
                .build();
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
