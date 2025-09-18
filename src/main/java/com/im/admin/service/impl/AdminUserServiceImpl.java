package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.AdminUserRepository;
import com.im.admin.repositories.ImSubRoleRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.AdminUserService;
import com.im.admin.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private AdminRepository adminRepository;

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

    @Override
    @Transactional
    public AdminUser createAdminUser(AdminUserRequestDTO dto) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
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

            String folder = "AdminUser";
            dto.setAdminrUserId(idGenerator.generateHierarchyEntityId(folder, "AD"));
            log.debug("Generated AdminUser ID: {}", dto.getAdminrUserId());

            String adminId = dto.getParentAccount();

            Admin admin = adminRepository.findById(adminId)
                    .orElseThrow(() -> {
                        log.error("Admin not found with ID: {}", adminId);
                        return new RuntimeException("Admin not found with ID: " + adminId);
                    });

            String permissionString = String.join(",", dto.getPermissions());
            log.debug("Permissions joined as: {}", permissionString);

            AdminUser adminUser = new AdminUser();
            adminUser.setAdminrUserId(dto.getAdminrUserId());
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

            AdminUser savedAdmin = adminUserRepository.save(adminUser);
            log.info("AdminUser saved successfully with ID: {}", savedAdmin.getAdminrUserId());

            UserDetails user = new UserDetails();
            user.setMobileNumber(savedAdmin.getMobileNumber());
            user.setHierarchyId(savedAdmin.getAdminrUserId());
            user.setHierarchyCode("AU");
            user.setLoginUserName(savedAdmin.getUsername());
            user.setLoginPassword(savedAdmin.getPassword());
            user.setEmailId(savedAdmin.getEmailId());
            user.setCreatedBy(savedAdmin.getCreatedBy());
            user.setEditedBy(savedAdmin.getCreatedBy());
            user.setUserStatus("Active");
            user.setUserLoginStatus(false);
            user.setChannel("web");
            user.setPermission(savedAdmin.getPermission());

            userDetailsRepository.save(user);
            log.info("UserDetails saved for AdminUser ID: {}", savedAdmin.getAdminrUserId());

            response = objectMapper.writeValueAsString(savedAdmin);
            responseDateTime = LocalDateTime.now();
            log.debug("Response prepared: {}", response);

            return savedAdmin;

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
        if (dto == null) return null;
        Address address = new Address();
        address.setAddress(dto.getAddress());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setZipCode(dto.getZipCode());
        return address;
    }

    @Override
    public Page<AdminUser> getAllAdminUsers(int page, int size) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching all AdminUsers - Page: " + page + ", Size: " + size;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("Starting getAllAdminUsers with page: {}, size: {}", page, size);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
            Page<AdminUser> pageResult = adminUserRepository.findAll(pageable);

            response = objectMapper.writeValueAsString(pageResult.getContent());
            responseDateTime = LocalDateTime.now();

            log.debug("Fetched AdminUsers: {}", response);
            log.info("Successfully retrieved AdminUsers - total elements: {}", pageResult.getTotalElements());

            return pageResult;
        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error("Error occurred while fetching AdminUsers: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
            log.info("IM logs saved for getAllAdminUsers request.");
        }
    }


    @Override
    public AdminUser updateAdminUser(String id, AdminUserRequestDTO dto) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("Starting updateAdminUser for ID: {}", id);

        try {
            request = objectMapper.writeValueAsString(dto);
            log.debug("Update request payload: {}", request);

            AdminUser existingUser = adminUserRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Admin user not found with ID: {}", id);
                        return new RuntimeException("Admin user not found with ID: " + id);
                    });

            // Update AdminUser fields
            existingUser.setName(dto.getName());
            existingUser.setMobileNumber(dto.getMobileNumber());
            existingUser.setUsername(dto.getUsername());
            existingUser.setPassword(dto.getPassword());
            existingUser.setEmailId(dto.getEmailId());
            existingUser.setEmailVerified(dto.getEmailVerified());
            existingUser.setDob(dto.getDob());
            existingUser.setAdd(mapAddress(dto.getAdd()));
            existingUser.setPermission(String.join(",", dto.getPermissions()));
            existingUser.setEditedBy(dto.getEditedBy());

            AdminUser savedUser = adminUserRepository.save(existingUser);
            log.info("AdminUser updated successfully for ID: {}", savedUser.getAdminrUserId());

            // Update corresponding UserDetails
            UserDetails userDetails = userDetailsRepository
                    .findByLoginUserName(savedUser.getUsername())
                    .orElseThrow(() -> {
                        log.error("UserDetails not found for username: {}", savedUser.getUsername());
                        return new RuntimeException("UserDetails not found for username: " + savedUser.getUsername());
                    });

            userDetails.setMobileNumber(savedUser.getMobileNumber());
            userDetails.setLoginUserName(savedUser.getUsername());
            userDetails.setLoginPassword(savedUser.getPassword());
            userDetails.setEmailId(savedUser.getEmailId());
            userDetails.setEditedBy(savedUser.getEditedBy());
            userDetails.setPermission(savedUser.getPermission());

            userDetailsRepository.save(userDetails);
            log.info("UserDetails updated for username: {}", savedUser.getUsername());

            response = objectMapper.writeValueAsString(savedUser);
            responseDateTime = LocalDateTime.now();
            log.debug("Update response payload: {}", response);

            return savedUser;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("Exception during updateAdminUser: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
            log.info("IM logs saved for updateAdminUser request.");
        }
    }


    @Override
    public AdminUser getAdminUserById(String  id) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching AdminUser with ID: " + id;
        String response = null;
        LocalDateTime responseDateTime = null;

        log.info("Initiating getAdminUserById for ID: {}", id);

        try {
            AdminUser adminUser = adminUserRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Admin user not found with ID: {}", id);
                        return new RuntimeException("Admin user not found with ID: " + id);
                    });


            response = objectMapper.writeValueAsString(adminUser);
            responseDateTime = LocalDateTime.now();

            log.debug("Fetched AdminUser: {}", response);
            log.info("Successfully retrieved AdminUser with ID: {}", id);

            return adminUser;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error("Exception in getAdminUserById for ID {}: {}", id, ex.getMessage(), ex);
            throw ex;

        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
            log.info("IM logs saved for getAdminUserById request.");
        }
    }

}
