package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.*;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.AdminUserRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.repositories.WhiteLabelUserRepository;
import com.im.admin.service.WhiteLabelUserService;
import com.im.admin.util.IdGenerator;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class WhiteLabelUserServiceImpl implements WhiteLabelUserService {

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private Validator validator;

    @Autowired
    private WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private WhiteLabelUserRepository whiteLabelUserRepository;

    @Override
    @Transactional
    public WhiteLabelUserResponseDTO createWhiteLabelUser(
            WhiteLabelUserRequestDTO dto
    ) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {

            // Field Validation
            Set<ConstraintViolation<WhiteLabelUserRequestDTO>> violations = validator.validate(dto);
            List<FieldErrorDetailsDTO> errors = violations.stream()
                    .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                    .collect(Collectors.toList());

            // Phone validation
            try {
                phoneNumberValidationService.validatePhoneNumber(dto.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
            }

            if (!errors.isEmpty()) {
                throw new CustomValidationException(errors);
            }
            request = objectMapper.writeValueAsString(dto);
            log.info("Received AdminUser creation request: {}", request);

            String parentAccount = dto.getParentAccount();

            if (parentAccount == null || !parentAccount.contains("-")) {
                log.error("Invalid parentAccount format: {}", parentAccount);
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            String abbreviation = parentAccount.split("-")[0];

            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                log.error("Invalid abbreviation derived from parentAccount: {}", abbreviation);
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }

            dto.setAbbreviation(abbreviation);
            log.debug("Abbreviation set to: {}", abbreviation);

            String folder = "WhiteLabelUser";
            dto.setId(idGenerator.generateHierarchyEntityId(folder, "WAU"));
            log.debug("Generated AdminUser ID: {}", dto.getId());

            WhiteLabels whiteLabel = whiteLabelRepository
                    .findById(parentAccount)
                    .orElseThrow(() ->
                            new RuntimeException(
                                    "White Label not found : "
                                            + parentAccount));

            if (!Boolean.TRUE.equals(whiteLabel.getIsAdmin())) {
                throw new RuntimeException(
                        "Only WL Admin can create White Label Users");
            }

            String permissionString = String.join(",", dto.getPermissions());
            log.debug("Permissions joined as: {}", permissionString);

            WhiteLabelUser adminUser = new WhiteLabelUser();
            adminUser.setId(dto.getId());
            adminUser.setParentAccount(dto.getParentAccount());
            adminUser.setName(dto.getName());
            adminUser.setMobileNumber(dto.getMobileNumber());
            adminUser.setUsername(dto.getUsername());
            adminUser.setPassword(dto.getPassword());
            adminUser.setEmailId(dto.getEmailId());
            adminUser.setEmailVerified(dto.getEmailVerified());
            adminUser.setDob(dto.getDob());
            adminUser.setAdd(mapAddress(dto.getAdd()));
            adminUser.setCreatedBy(dto.getCreatedBy());
            adminUser.setCreatedByIPAddress(dto.getCreatedByIPAddress());
            adminUser.setPermission(permissionString);

            WhiteLabelUser savedAdmin = whiteLabelUserRepository.save(adminUser);
            log.info("AdminUser saved successfully with ID: {}", savedAdmin.getId());

            WhiteLabelUser savedUser = whiteLabelUserRepository.save(adminUser);

            log.info("WhiteLabelUser saved successfully with ID: {}", savedUser.getId());

            UserDetails user = new UserDetails();
            user.setMobileNumber(savedUser.getMobileNumber());
            user.setHierarchyId(savedUser.getId());
            user.setHierarchyCode("WAU");
            user.setLoginUserName(savedUser.getUsername());
            user.setLoginPassword(savedUser.getPassword());
            user.setEmailId(savedUser.getEmailId());
            user.setCreatedBy(savedUser.getCreatedBy());
            user.setEditedBy(savedUser.getCreatedBy());
            user.setUserStatus("Active");
            user.setUserLoginStatus(false);
            user.setChannel("web");
            user.setPermission(savedUser.getPermission());

            userDetailsRepository.save(user);

            log.info("UserDetails saved for WhiteLabelUser ID: {}", savedUser.getId());

            response = objectMapper.writeValueAsString(savedUser);
            responseDateTime = LocalDateTime.now();

            return mapToResponse(savedUser);

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("Exception occurred during AdminUser creation: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
            log.info("IM logs saved for AdminUser creation.");
        }
    }

    private Address mapAddress(AddressDTO dto) {
        if (dto == null) {
            return null;
        }

        Address address = new Address();
        address.setAddress(dto.getAddress());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());

        return address;
    }

    private WhiteLabelUserResponseDTO mapToResponse(
            WhiteLabelUser user) {

        AddressDTO addressDTO = null;

        if (user.getAdd() != null) {

            addressDTO = new AddressDTO();

            addressDTO.setAddress(
                    user.getAdd().getAddress());

            addressDTO.setCity(
                    user.getAdd().getCity());

            addressDTO.setState(
                    user.getAdd().getState());

            addressDTO.setZipCode(
                    user.getAdd().getZipCode());
        }

        return WhiteLabelUserResponseDTO.builder()
                .id(user.getId())
                .parentAccount(user.getParentAccount())
                .abbreviation(user.getAbbreviation())
                .name(user.getName())
                .mobileNumber(user.getMobileNumber())
                .username(user.getUsername())
                .emailId(user.getEmailId())
                .emailVerified(user.getEmailVerified())
                .dob(user.getDob())
                .add(addressDTO)
                .status(user.getStatus())
                .createdOn(user.getCreatedOn())
                .createdBy(user.getCreatedBy())
                .lastEditedOn(user.getLastEditedOn())
                .editedBy(user.getEditedBy())
                .build();
    }

    @Override
    public Page<WhiteLabelUserResponseDTO> getAllWhiteLabelUsers(
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

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching White Label Users - Page: " + page + ", Size: " + size;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {

            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.by("createdOn").descending()
            );

            Specification<WhiteLabelUser> spec = (root, query, cb) -> {

                List<Predicate> predicates = new ArrayList<>();

                if (id != null && !id.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("id")),
                                    "%" + id.toLowerCase() + "%"
                            )
                    );
                }

                if (name != null && !name.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("name")),
                                    "%" + name.toLowerCase() + "%"
                            )
                    );
                }

                if (mobileNumber != null && !mobileNumber.isBlank()) {
                    predicates.add(
                            cb.like(
                                    root.get("mobileNumber"),
                                    "%" + mobileNumber + "%"
                            )
                    );
                }

                if (username != null && !username.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("username")),
                                    "%" + username.toLowerCase() + "%"
                            )
                    );
                }

                if (emailId != null && !emailId.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("emailId")),
                                    "%" + emailId.toLowerCase() + "%"
                            )
                    );
                }

                if (status != null) {
                    predicates.add(
                            cb.equal(root.get("status"), status)
                    );
                }

                if (dob != null) {
                    predicates.add(
                            cb.equal(root.get("dob"), dob)
                    );
                }

                if (createdOn != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("createdOn").as(LocalDate.class),
                                    createdOn
                            )
                    );
                }

                if (createdBy != null && !createdBy.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("createdBy")),
                                    "%" + createdBy.toLowerCase() + "%"
                            )
                    );
                }

                if (lastEditedOn != null) {
                    predicates.add(
                            cb.equal(
                                    root.get("lastEditedOn").as(LocalDate.class),
                                    lastEditedOn
                            )
                    );
                }

                if (editedBy != null && !editedBy.isBlank()) {
                    predicates.add(
                            cb.like(
                                    cb.lower(root.get("editedBy")),
                                    "%" + editedBy.toLowerCase() + "%"
                            )
                    );
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<WhiteLabelUser> pageResult =
                    whiteLabelUserRepository.findAll(spec, pageable);

            Page<WhiteLabelUserResponseDTO> responsePage =
                    pageResult.map(this::mapToResponse);

            response = objectMapper.writeValueAsString(
                    responsePage.getContent()
            );

            responseDateTime = LocalDateTime.now();

            return responsePage;

        } catch (Exception ex) {

            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error(
                    "Error occurred while fetching WhiteLabelUsers",
                    ex
            );

            throw ex;

        } finally {

            commonFunctions.saveImLogs(
                    request,
                    requestDateTime,
                    null,
                    response,
                    responseDateTime
            );
        }
    }

    @Override
    public WhiteLabelUserResponseDTO getWhiteLabelUserById(String id)
            throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching WhiteLabelUser with ID: " + id;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("Initiating getWhiteLabelUserById for ID: {}", id);

        try {

            WhiteLabelUser whiteLabelUser = whiteLabelUserRepository
                    .findById(id)
                    .orElseThrow(() -> {
                        log.error("White Label User not found with ID: {}", id);
                        return new RuntimeException(
                                "White Label User not found with ID: " + id
                        );
                    });

            WhiteLabelUserResponseDTO dto = mapToResponse(whiteLabelUser);

            response = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();

            log.debug("Fetched WhiteLabelUser: {}", response);
            log.info("Successfully retrieved WhiteLabelUser with ID: {}", id);

            return dto;

        } catch (Exception ex) {

            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error(
                    "Exception in getWhiteLabelUserById for ID {}: {}",
                    id,
                    ex.getMessage(),
                    ex
            );

            throw ex;

        } finally {

            commonFunctions.saveImLogs(
                    request,
                    requestDateTime,
                    null,
                    response,
                    responseDateTime
            );

            log.info("IM logs saved for getWhiteLabelUserById request.");
        }
    }

    @Override
    @Transactional
    public WhiteLabelUserResponseDTO updateWhiteLabelUser(
            String id,
            WhiteLabelUserRequestDTO dto
    ) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("Starting updateWhiteLabelUser for ID: {}", id);

        try {

            request = objectMapper.writeValueAsString(dto);
            log.debug("Update request payload: {}", request);

            WhiteLabelUser existingUser = whiteLabelUserRepository
                    .findById(id)
                    .orElseThrow(() -> {
                        log.error("White Label User not found with ID: {}", id);
                        return new RuntimeException(
                                "White Label User not found with ID: " + id
                        );
                    });

            // Update Entity
            existingUser.setName(dto.getName());
            existingUser.setMobileNumber(dto.getMobileNumber());
            existingUser.setUsername(dto.getUsername());
            existingUser.setPassword(dto.getPassword());
            existingUser.setEmailId(dto.getEmailId());
            existingUser.setEmailVerified(dto.getEmailVerified());
            existingUser.setDob(dto.getDob());
            existingUser.setAdd(mapAddress(dto.getAdd()));
            existingUser.setEditedBy(dto.getEditedBy());

            if (dto.getPermissions() != null) {
                existingUser.setPermission(
                        String.join(",", dto.getPermissions())
                );
            }

            WhiteLabelUser savedUser =
                    whiteLabelUserRepository.save(existingUser);

            log.info(
                    "White Label User updated successfully for ID: {}",
                    savedUser.getId()
            );

            // Update UserDetails
            UserDetails userDetails = userDetailsRepository
                    .findByHierarchyId(savedUser.getId())
                    .orElseThrow(() -> {
                        log.error(
                                "UserDetails not found for hierarchyId: {}",
                                savedUser.getId()
                        );
                        return new RuntimeException(
                                "UserDetails not found for hierarchyId: "
                                        + savedUser.getId()
                        );
                    });

            userDetails.setMobileNumber(savedUser.getMobileNumber());
            userDetails.setLoginUserName(savedUser.getUsername());
            userDetails.setLoginPassword(savedUser.getPassword());
            userDetails.setEmailId(savedUser.getEmailId());
            userDetails.setEditedBy(dto.getEditedBy());
            userDetails.setPermission(savedUser.getPermission());

            userDetailsRepository.save(userDetails);

            log.info(
                    "UserDetails updated successfully for hierarchyId: {}",
                    savedUser.getId()
            );

            WhiteLabelUserResponseDTO responseDTO =
                    mapToResponse(savedUser);

            response = objectMapper.writeValueAsString(responseDTO);
            responseDateTime = LocalDateTime.now();

            return responseDTO;

        } catch (Exception ex) {

            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error(
                    "Exception during updateWhiteLabelUser: {}",
                    ex.getMessage(),
                    ex
            );

            throw ex;

        } finally {

            commonFunctions.saveImLogs(
                    request,
                    requestDateTime,
                    null,
                    response,
                    responseDateTime
            );

            log.info(
                    "IM logs saved for updateWhiteLabelUser request."
            );
        }
    }
}
