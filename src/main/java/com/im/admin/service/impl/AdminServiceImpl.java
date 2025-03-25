package com.im.admin.service.impl;

import com.im.admin.entities.Admin;
import com.im.admin.repositories.AdminRepository;
import com.im.admin.service.AdminService;
import com.im.admin.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private IdGenerator idGenerator;

    @Override
    public Admin saveAdmin(Admin admin) {
        phoneNumberValidationService.validatePhoneNumber(admin.getMobileNumber());

        admin.setId(idGenerator.generateId("Admin"));
        return adminRepository.save(admin);
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


