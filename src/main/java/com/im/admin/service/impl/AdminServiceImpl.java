package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.AdminPostpaidAmountRepository;
import com.im.admin.repositories.AdminPrepaidAmountRepository;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.AdminService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.IdGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private AdminPostpaidAmountRepository adminPostpaidAmountRepository;

    @Autowired
    private AdminPrepaidAmountRepository adminPrepaidAmountRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommonMethods commonMethods;

    @Autowired
    private Validator validator;


    @Transactional
    @Override
    public Admin saveAdmin(Admin admin) {

        LocalDateTime RequestDateTime = LocalDateTime.now();
        log.info("Start saving Admin | RequestDateTime={}, Admin={}", RequestDateTime, admin);

        // Field Validation
        Set<ConstraintViolation<Admin>> violations = validator.validate(admin);
        List<FieldErrorDetailsDTO> errors = violations.stream().map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage())).collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(admin.getMobileNumber());
            log.info("Phone number validation successful for mobile={}", admin.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            log.error("Phone number validation failed: {}", ex.getMessage());
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            log.error("Validation errors found: {}", errors);
            throw new CustomValidationException(errors);
        }

        admin.setId(idGenerator.generateEntityId("Admin", null));
        log.info("Generated Admin ID={}", admin.getId());

        Admin saveAdmin = adminRepository.save(admin);
        log.info("Admin entity saved successfully | ID={}", saveAdmin.getId());

        try {
            saveUserDetails(saveAdmin);
            log.info("UserDetails saved successfully for Admin ID={}", saveAdmin.getId());
        } catch (Exception e) {
            log.error("Failed to save user details for Admin ID={} | Error={}", saveAdmin.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to save user details: " + e.getMessage());
        }

        AdminPostpaidAmount adminPostpaidAccount = new AdminPostpaidAmount();
        adminPostpaidAccount.setAdminId(saveAdmin);
        adminPostpaidAmountRepository.save(adminPostpaidAccount);
        log.info("AdminPostpaidAmount initialized for Admin ID={}", saveAdmin.getId());

        AdminPrepaidAmount adminPrepaidAccount = new AdminPrepaidAmount();
        adminPrepaidAccount.setAdminId(saveAdmin);
        adminPrepaidAmountRepository.save(adminPrepaidAccount);
        log.info("AdminPrepaidAmount initialized for Admin ID={}", saveAdmin.getId());

        LocalDateTime ResponseDateTime = LocalDateTime.now();

        String adminRequest = null;
        String adminResponse = null;
        try {
            adminRequest = objectMapper.writeValueAsString(admin);
            adminResponse = objectMapper.writeValueAsString(saveAdmin);
            log.debug("DistributorRequest={}, DistributorResponse={}", adminRequest, adminResponse);
        } catch (JsonProcessingException e) {
            log.error("Error serializing Admin objects for logs: {}", e.getMessage(), e);
            e.printStackTrace(); // Optional: Log it
        }

        commonFunctions.saveImLogs(adminRequest, RequestDateTime, null, adminResponse, ResponseDateTime);
        log.info("IM Logs saved for Admin ID={} | RequestTime={}, ResponseTime={}", saveAdmin.getId(), RequestDateTime, ResponseDateTime);

        log.info("Admin saved successfully | ID={}", saveAdmin.getId());
        return saveAdmin;
    }

    private void saveUserDetails(Admin admin) {

        log.info("Saving UserDetails for Admin ID={}", admin.getId());

        String hierarchyCode = commonMethods.extractHierarchyType(admin.getId());

        UserDetails userDetails = new UserDetails();
        userDetails.setMobileNumber(admin.getMobileNumber());
        userDetails.setLoginUserName(admin.getUserName());
        userDetails.setLoginPassword(admin.getPassword());
        userDetails.setHierarchyId(admin.getId());
        userDetails.setEmailId(admin.getEmailId());
        userDetails.setHierarchyCode(hierarchyCode);

        userDetails.setCreatedBy("Admin");
        userDetails.setCreatedDate(LocalDate.now());
        userDetails.setCreatedTime(LocalTime.now());
        userDetails.setEditedBy("Admin");
        userDetails.setEditedDate(LocalDate.now());
        userDetails.setEditedTime(LocalTime.now());

        userDetailsRepository.save(userDetails);
        log.info("UserDetails saved successfully | HierarchyId={}", admin.getId());
    }

    @Override
    public Admin updateAdmin(String id, Admin updatedAdmin) {

        log.info("Received request to update Admin with ID: {}", id);

        LocalDateTime requestDateTime = LocalDateTime.now();

        Optional<Admin> existing = adminRepository.findById(id);
        if (existing.isPresent()) {
            Admin admin = existing.get();
            log.debug("Existing Admin data: {}", admin);

            // Serialize incoming request
            String adminRequest = null;
            try {
                adminRequest = objectMapper.writeValueAsString(updatedAdmin);
                log.info("Incoming Admin update request: {}", adminRequest);
            } catch (JsonProcessingException e) {
                log.error("Error serializing updatedAdmin request", e);
                e.printStackTrace(); // keeping your original line
            }

            // Update fields
            admin.setName(updatedAdmin.getName());
            admin.setMobileNumber(updatedAdmin.getMobileNumber());
            admin.setEmailId(updatedAdmin.getEmailId());
            admin.setUserName(updatedAdmin.getUserName());
            admin.setPassword(updatedAdmin.getPassword());

            Admin savedAdmin = adminRepository.save(admin);
            log.info("Admin updated and saved successfully: {}", savedAdmin);

            LocalDateTime responseDateTime = LocalDateTime.now();

            // Serialize updated response
            String adminResponse = null;
            try {
                adminResponse = objectMapper.writeValueAsString(savedAdmin);
                log.debug("Serialized Admin response: {}", adminResponse);
            } catch (JsonProcessingException e) {
                log.error("Error serializing savedAdmin response", e);
                e.printStackTrace(); // keeping your original line
            }

            // Save logs
            commonFunctions.saveImLogs(
                    adminRequest,
                    requestDateTime,
                    null, // IP Address
                    adminResponse,
                    responseDateTime
            );
            log.info("Logs saved for Admin update operation");

            return savedAdmin;
        }

        log.warn("Admin not found with ID: {}", id);
        throw new RuntimeException("Admin not found with ID: " + id);
    }


    @Override
    public void deleteAdmin(String id) {
        log.info("Request received to delete Admin with ID: {}", id);

        if (adminRepository.existsById(id)) {
            adminRepository.deleteById(id);
            log.info("Successfully deleted Admin with ID: {}", id);
        } else {
            log.error("Failed to delete Admin. Admin not found with ID: {}", id);
            throw new RuntimeException("Admin not found with ID: " + id);
        }
    }


    @Override
    public List<Admin> getAllAdmins(int page, int size) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching all Admins - Page: " + page + ", Size: " + size;

        List<Admin> admins = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            log.info(request);
            Page<Admin> result = adminRepository.findAll(PageRequest.of(page, size));
            admins = result.getContent();

            response = objectMapper.writeValueAsString(admins);
            responseDateTime = LocalDateTime.now();

            log.info("Total Admins fetched: {}", admins.size());
            return admins;
        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex; // propagate error
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }


    @Override
    public Admin getAdminById(String id) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching Admin with ID: " + id;

        Admin admin = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            log.info(request);
            admin = adminRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Admin not found with ID: {}", id);
                        return new RuntimeException("Admin not found with ID: " + id);
                    });

            response = objectMapper.writeValueAsString(admin);
            responseDateTime = LocalDateTime.now();

            return admin;
        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }


}


