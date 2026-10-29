package com.scs.admin.service.impl;

import com.scs.admin.commons.CommonFunctions;
import com.scs.admin.entities.SuperAdmin;
import com.scs.admin.repositories.SuperAdminRepository;
import com.scs.admin.service.SuperAdminService;
import com.scs.admin.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public SuperAdmin saveSuperAdmin(SuperAdmin superAdmin) {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = superAdmin.toString(); // you can use ObjectMapper to save JSON instead

        log.info("Saving SuperAdmin: {}", superAdmin);

        phoneNumberValidationService.validatePhoneNumber(superAdmin.getMobileNumber());

        superAdmin.setId(idGenerator.generateId("SuperAdmin"));
        SuperAdmin savedSuperAdmin = superAdminRepository.save(superAdmin);

        LocalDateTime responseTime = LocalDateTime.now();
        String response = savedSuperAdmin.toString();

        // Save Hierarchy Logs
        commonFunctions.saveHierarchyLogs(
                requestTime,
                request,
                null, // you can capture IP if available
                responseTime,
                response
        );

        log.info("SuperAdmin saved successfully with ID: {}", savedSuperAdmin.getId());

        return savedSuperAdmin;
    }


    @Override
    public SuperAdmin getSuperAdminById(String id) {
        log.info("Fetching SuperAdmin with ID: {}", id);

        LocalDateTime requestTime = LocalDateTime.now();
        String requestData = "Get SuperAdmin By ID: " + id;

        try {
            SuperAdmin superAdmin = superAdminRepository.findById(id)
                    .orElseThrow(() -> {
                        String errorMsg = "SuperAdmin not found with ID: " + id;
                        log.error(errorMsg);

                        // Save hierarchy log for failure
                        commonFunctions.saveHierarchyLogs(requestTime, requestData, null, LocalDateTime.now(), errorMsg);
                        return new RuntimeException(errorMsg);
                    });

            log.info("Successfully fetched SuperAdmin with ID: {}", id);

            // Save hierarchy log for success
            commonFunctions.saveHierarchyLogs(requestTime, requestData, null, LocalDateTime.now(), "SuperAdmin fetched successfully");

            return superAdmin;

        } catch (Exception e) {
            log.error("Error fetching SuperAdmin with ID: {} - {}", id, e.getMessage(), e);

            // Save hierarchy log for exception
            commonFunctions.saveHierarchyLogs(requestTime, requestData, null, LocalDateTime.now(), "Error: " + e.getMessage());
            throw e;
        }
    }


    @Override
    public List<SuperAdmin> getAllSuperAdmins(int page, int size) {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = "FETCH_ALL SuperAdmins with pagination (page=" + page + ", size=" + size + ")";
        LocalDateTime responseTime = null;
        String response = null;

        try {
            log.info("Fetching all SuperAdmins with page: {}, size: {}", page, size);

            Page<SuperAdmin> result = superAdminRepository.findAll(PageRequest.of(page, size));
            List<SuperAdmin> superAdmins = result.getContent();

            log.debug("Fetched {} SuperAdmins", superAdmins.size());

            responseTime = LocalDateTime.now();
            response = "SUCCESS - Fetched " + superAdmins.size() + " SuperAdmins";

            return superAdmins;

        } catch (Exception ex) {
            responseTime = LocalDateTime.now();
            response = "ERROR - " + ex.getMessage();

            log.error("Error while fetching SuperAdmins: {}", ex.getMessage(), ex);
            throw ex; // rethrow so API caller knows failure

        } finally {
            try {
                commonFunctions.saveHierarchyLogs(
                        requestTime,
                        request,
                        null,
                        responseTime,
                        response
                );
            } catch (Exception logEx) {
                log.error("Failed to save hierarchy logs: {}", logEx.getMessage(), logEx);
            }
        }
    }

    @Override
    public SuperAdmin updateSuperAdmin(String id, SuperAdmin updatedSuperAdmin) {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestPayload = "UpdateSuperAdmin Request for ID: " + id + " | Payload: " + updatedSuperAdmin.toString();
        String ipAddress = "127.0.0.1"; // TODO: Fetch dynamically from request context if available

        try {
            Optional<SuperAdmin> existing = superAdminRepository.findById(id);

            if (existing.isPresent()) {
                SuperAdmin superAdmin = existing.get();
                superAdmin.setName(updatedSuperAdmin.getName());
                superAdmin.setMobileNumber(updatedSuperAdmin.getMobileNumber());
                superAdmin.setEmailId(updatedSuperAdmin.getEmailId());
                superAdmin.setUserName(updatedSuperAdmin.getUserName());
                superAdmin.setPassword(updatedSuperAdmin.getPassword());

                SuperAdmin savedSuperAdmin = superAdminRepository.save(superAdmin);

                commonFunctions.saveHierarchyLogs(
                        requestTime,
                        requestPayload,
                        ipAddress,
                        LocalDateTime.now(),
                        "Update successful for ID: " + id + " | Response: " + savedSuperAdmin.toString()
                );

                return savedSuperAdmin;
            } else {
                String errorResponse = "SuperAdmin not found with ID: " + id;

                commonFunctions.saveHierarchyLogs(
                        requestTime,
                        requestPayload,
                        ipAddress,
                        LocalDateTime.now(),
                        errorResponse
                );

                throw new RuntimeException(errorResponse);
            }
        } catch (Exception e) {
            commonFunctions.saveHierarchyLogs(
                    requestTime,
                    requestPayload,
                    ipAddress,
                    LocalDateTime.now(),
                    "Error occurred: " + e.getMessage()
            );
            throw e;
        }
    }


    @Override
    public void deleteSuperAdmin(String id) {
        if (superAdminRepository.existsById(id)) {
            superAdminRepository.deleteById(id);
        } else {
            throw new RuntimeException("SuperAdmin not found with ID: " + id);
        }
    }

}

