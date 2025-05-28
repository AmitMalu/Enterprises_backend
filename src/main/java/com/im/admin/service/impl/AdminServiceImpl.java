package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.Admin;
import com.im.admin.entities.AdminPostpaidAmount;
import com.im.admin.entities.AdminPrepaidAmount;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.AdminPostpaidAmountRepository;
import com.im.admin.repositories.AdminPrepaidAmountRepository;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.repositories.UserDetailsRepository;
import com.im.admin.service.AdminService;
import com.im.admin.util.CommonMethods;
import com.im.admin.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

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


    @Override
    public Admin saveAdmin(Admin admin) {
        phoneNumberValidationService.validatePhoneNumber(admin.getMobileNumber());

        LocalDateTime RequestDateTime = LocalDateTime.now();

        admin.setId(idGenerator.generateId("Admin"));
        Admin saveAdmin = adminRepository.save(admin);

        saveUserDetails(saveAdmin);

        AdminPostpaidAmount adminPostpaidAccount = new AdminPostpaidAmount();
        adminPostpaidAccount.setAdminId(saveAdmin);
        adminPostpaidAmountRepository.save(adminPostpaidAccount);

        AdminPrepaidAmount adminPrepaidAccount = new AdminPrepaidAmount();
        adminPrepaidAccount.setAdminId(saveAdmin);
        adminPrepaidAmountRepository.save(adminPrepaidAccount);

        LocalDateTime ResponseDateTime = LocalDateTime.now();

        String DistributorRequest = null;
        String DistributorResponse = null;
        try {
            DistributorRequest = objectMapper.writeValueAsString(admin);
            DistributorResponse = objectMapper.writeValueAsString(saveAdmin);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Optional: Log it
        }


        commonFunctions.saveImLogs(
                DistributorRequest,
                RequestDateTime,
                null, // IP address as null
                DistributorResponse,
                ResponseDateTime
        );

        return saveAdmin;
    }

    private void saveUserDetails(Admin admin) {

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

    }

    /**
     * Updates an Admin record.
     */
    @Override
    public Admin updateAdmin(String id, Admin updatedAdmin) {
        Optional<Admin> existing = adminRepository.findById(id);
        if (existing.isPresent()) {
            Admin admin = existing.get();
            admin.setName(updatedAdmin.getName());
            admin.setMobileNumber(updatedAdmin.getMobileNumber());
            admin.setEmailId(updatedAdmin.getEmailId());
            admin.setUserName(updatedAdmin.getUserName());
            admin.setPassword(updatedAdmin.getPassword());
            return adminRepository.save(admin);
        }
        throw new RuntimeException("Admin not found with ID: " + id);
    }

    /**
     * Deletes an Admin record.
     */
    @Override
    public void deleteAdmin(String id) {
        if (adminRepository.existsById(id)) {
            adminRepository.deleteById(id);
        } else {
            throw new RuntimeException("Admin not found with ID: " + id);
        }
    }

    /**
     * Retrieves all Admins with pagination.
     */
    @Override
    public List<Admin> getAllAdmins(int page, int size) {
        Page<Admin> result = adminRepository.findAll(PageRequest.of(page, size));
        return result.getContent();
    }

    /**
     * Retrieves an Admin by ID.
     */
    @Override
    public Admin getAdminById(String id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Admin not found with ID: " + id));
    }
}


