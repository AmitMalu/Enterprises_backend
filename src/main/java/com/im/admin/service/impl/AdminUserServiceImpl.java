package com.im.admin.service.impl;

import com.im.admin.entities.*;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.AdminUserRepository;
import com.im.admin.repositories.ImSubRoleRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.AdminUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    public AdminUser createAdminUser(AdminUser adminRequest) {

        String adminId = adminRequest.getParentAccount();

        // 2. Fetch retailer by ID
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + adminId));


        // 2. Save AdminUser (ID is auto-generated)
        AdminUser savedAdmin = adminUserRepository.save(adminRequest);

        // 3. Save UserDetails for login
        UserDetails user = new UserDetails();

        user.setMobileNumber(savedAdmin.getMobileNumber());
        user.setHierarchyId(adminId);       // parent account reference
        user.setHierarchyCode("AU");              // code for Admin User
        user.setLoginUserName(savedAdmin.getUsername());
        user.setLoginPassword(savedAdmin.getPassword());
        user.setEmailId(savedAdmin.getEmailId());
        user.setCreatedBy(savedAdmin.getCreatedBy());
        user.setEditedBy(savedAdmin.getCreatedBy());
        user.setUserStatus("Active");              // or Pending
        user.setUserLoginStatus(false);
        user.setChannel("web");
        user.setRole_id(savedAdmin.getRole_id());

        userDetailsRepository.save(user);

        // 4. Save SubRole if exists
        if (savedAdmin.getSubRoleId() != null && !savedAdmin.getSubRoleId().isEmpty()) {
            ImSubRole imSubRole = new ImSubRole();
            imSubRole.setMobileNumber(savedAdmin.getMobileNumber());
            imSubRole.setSubRoleId(savedAdmin.getSubRoleId());
            imSubRoleRepository.save(imSubRole);
        }

        return savedAdmin;
    }
}
