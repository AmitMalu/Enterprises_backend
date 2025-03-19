package com.im.admin.service.impl;

import com.im.admin.entities.SuperAdmin;
import com.im.admin.repositories.SuperAdminRepository;
import com.im.admin.service.SuperAdminService;
import com.im.admin.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    @Autowired
    private SuperAdminRepository superAdminRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Override
    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        phoneNumberValidationService.validatePhoneNumber(superAdmin.getMobileNumber());

        superAdmin.setId(idGenerator.generateId("SA"));
        return superAdminRepository.save(superAdmin);
    }


    /**
     * Retrieves a SuperAdmin by ID.
     */
    @Override
    public SuperAdmin getSuperAdminById(String id) {
        return superAdminRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperAdmin not found with ID: " + id));
    }


    /**
     * Retrieves all SuperAdmins with pagination.
     */
    @Override
    public List<SuperAdmin> getAllSuperAdmins(int page, int size) {
        Page<SuperAdmin> result = superAdminRepository.findAll(PageRequest.of(page, size));
        return result.getContent();
    }


    /**
     * Updates a SuperAdmin record.
     */
    @Override
    public SuperAdmin updateSuperAdmin(String id, SuperAdmin updatedSuperAdmin) {
        Optional<SuperAdmin> existing = superAdminRepository.findById(id);
        if (existing.isPresent()) {
            SuperAdmin superAdmin = existing.get();
            superAdmin.setName(updatedSuperAdmin.getName());
            superAdmin.setMobileNumber(updatedSuperAdmin.getMobileNumber());
            superAdmin.setEmailId(updatedSuperAdmin.getEmailId());
            superAdmin.setUserName(updatedSuperAdmin.getUserName());
            superAdmin.setPassword(updatedSuperAdmin.getPassword());
            return superAdminRepository.save(superAdmin);
        }
        throw new RuntimeException("SuperAdmin not found with ID: " + id);
    }


    /**
     * Deletes a SuperAdmin record.
     */
    @Override
    public void deleteSuperAdmin(String id) {
        if (superAdminRepository.existsById(id)) {
            superAdminRepository.deleteById(id);
        } else {
            throw new RuntimeException("SuperAdmin not found with ID: " + id);
        }
    }

}

