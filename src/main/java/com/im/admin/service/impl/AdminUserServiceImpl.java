package com.im.admin.service.impl;

import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.AdminUserRequestDTO;
import com.im.admin.entities.*;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.AdminUserRepository;
import com.im.admin.repositories.ImSubRoleRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;


@Service
public class AdminUserServiceImpl implements AdminUserService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private AdminUserRepository adminUserRepository;

    @Autowired
    private UserDetailsRepository userDetailsRepository;

    @Autowired
    private ImSubRoleRepository imSubRoleRepository;

    @Override
    public AdminUser createAdminUser(AdminUserRequestDTO dto) {
        String adminId = dto.getParentAccount();

        // Validate parent admin
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));

        // Join permissions into comma-separated string
        String permissionString = String.join(",", dto.getPermissions());

        // Map DTO to entity
        AdminUser adminUser = new AdminUser();
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

        // Save UserDetails for login
        UserDetails user = new UserDetails();
        user.setMobileNumber(savedAdmin.getMobileNumber());
        user.setHierarchyId(adminId);
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

        return savedAdmin;
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
    public Page<AdminUser> getAllAdminUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdOn").descending());
        return adminUserRepository.findAll(pageable);
    }

    @Override
    public AdminUser updateAdminUser(Long id, AdminUserRequestDTO dto) {
        AdminUser existingUser = adminUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin user not found with ID: " + id));

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

        // Update corresponding UserDetails
        UserDetails userDetails = userDetailsRepository
                .findByLoginUserName(savedUser.getUsername())
                .orElseThrow(() -> new RuntimeException("UserDetails not found for username: " + savedUser.getUsername()));


        userDetails.setMobileNumber(savedUser.getMobileNumber());
        userDetails.setLoginUserName(savedUser.getUsername());
        userDetails.setLoginPassword(savedUser.getPassword());
        userDetails.setEmailId(savedUser.getEmailId());
        userDetails.setEditedBy(savedUser.getEditedBy());
        userDetails.setPermission(savedUser.getPermission());

        userDetailsRepository.save(userDetails);

        return savedUser;
    }


    @Override
    public AdminUser getAdminUserById(Long id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin user not found with ID: " + id));
    }




}
