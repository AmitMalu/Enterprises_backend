package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.SuperDistributorPostpaidAmountRepository;
import com.im.admin.repositories.SuperDistributorPrepaidAmountRepository;
import com.im.admin.repositories.SuperDistributorRepository;
import com.im.admin.service.SuperDistributorService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperDistributorServiceImpl implements SuperDistributorService {

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private SuperDistributorPrepaidAmountRepository superDistributorPrepaidAmountRepository;

    @Autowired
    private SuperDistributorPostpaidAmountRepository superDistributorPostpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private ImageHandlerUtil imageHandlerUtil;

    @Autowired
    private Validator validator;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;


    @Transactional
    @Override
    public SuperDistributor saveSuperDistributor(SuperDistributor superDistributor,
                                                 MultipartFile aadhaarCardFrontImage,
                                                 MultipartFile aadhaarCardBackImage,
                                                 MultipartFile panCardImage,
                                                 MultipartFile shopImage,
                                                 MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestPayload = objectMapper.writeValueAsString(superDistributor);

        log.info("Starting saveSuperDistributor for mobile: {}", superDistributor.getMobileNumber());

        try {
            // Field Validation
            Set<ConstraintViolation<SuperDistributor>> violations = validator.validate(superDistributor);
            List<FieldErrorDetailsDTO> errors = violations.stream()
                    .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                    .collect(Collectors.toList());

            // Phone validation
            try {
                phoneNumberValidationService.validatePhoneNumber(superDistributor.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
            }

            // Image validation
            Map<String, MultipartFile> imageMap = new HashMap<>();
            imageMap.put("aadhaarCardFrontImage", aadhaarCardFrontImage);
            imageMap.put("aadhaarCardBackImage", aadhaarCardBackImage);
            imageMap.put("panCardImage", panCardImage);
            imageMap.put("shopImage", shopImage);
            imageMap.put("businessProofImage", businessProofImage);

            for (Map.Entry<String, MultipartFile> entry : imageMap.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    errors.add(new FieldErrorDetailsDTO(entry.getKey(), "image is required"));
                }
            }

            if (!errors.isEmpty()) {
                log.warn("Validation failed for SuperDistributor: {}", errors);
                throw new CustomValidationException(errors);
            }

            String parentAccount = superDistributor.getParentAccount();

            if (parentAccount == null || !parentAccount.contains("-")) {
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            // Extract abbreviation
            String abbreviation = parentAccount.split("-")[0];
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }
            superDistributor.setAbbreviation(abbreviation);

            // Generate ID
            String folder = "Super Distributor";
            superDistributor.setSuperDistributorId(
                    idGenerator.generateHierarchyEntityId(folder, superDistributor.getAbbreviation())
            );

            // Upload Images
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, superDistributor::setAadhaarCardFrontImage);
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, superDistributor::setAadhaarCardBackImage);
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, superDistributor::setPanCardImage);
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, superDistributor::setShopImage);
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, superDistributor::setBusinessProofImage);

            // Save SuperDistributor
            SuperDistributor savedDistributor = superDistributorRepository.save(superDistributor);
            log.info("SuperDistributor saved successfully with ID: {}", savedDistributor.getSuperDistributorId());

            // Create prepaid/postpaid records
            SuperDistributorPrepaidAmount prepaidAmount = new SuperDistributorPrepaidAmount();
            prepaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPrepaidAmountRepository.save(prepaidAmount);
            log.info("Prepaid amount record created for Super Distributor: {}", savedDistributor.getSuperDistributorId());

            SuperDistributorPostpaidAmount postpaidAmount = new SuperDistributorPostpaidAmount();
            postpaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPostpaidAmountRepository.save(postpaidAmount);
            log.info("Postpaid amount record created for Super Distributor: {}", savedDistributor.getSuperDistributorId());

            // Save hierarchy logs
            LocalDateTime responseDateTime = LocalDateTime.now();
            String responsePayload = objectMapper.writeValueAsString(savedDistributor);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    responsePayload
            );

            log.info("Hierarchy logs saved for SuperDistributor: {}", savedDistributor.getSuperDistributorId());
            return savedDistributor;

        } catch (Exception e) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            log.error("Error occurred while saving SuperDistributor: {}", e.getMessage(), e);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    "Error: " + e.getMessage()
            );

            throw e;
        }
    }

    @Override
    public SuperDistributor getSuperDistributor(String id) {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = "Fetching SuperDistributor with id: " + id;

        log.info("Request to get SuperDistributor with id: {}", id);

        try {
            SuperDistributor superDistributor = superDistributorRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("SuperDistributor not found with id: " + id));

            LocalDateTime responseTime = LocalDateTime.now();
            String response = superDistributor.toString();

            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.info("Successfully fetched SuperDistributor with id: {}", id);
            return superDistributor;

        } catch (Exception ex) {
            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Error: " + ex.getMessage();

            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.error("Failed to fetch SuperDistributor with id: {}", id, ex);
            throw ex;
        }
    }


    @Transactional
    @Override
    public SuperDistributor updateSuperDistributor(String id,
                                                   SuperDistributor superDistributor,
                                                   MultipartFile aadhaarCardFrontImage,
                                                   MultipartFile aadhaarCardBackImage,
                                                   MultipartFile panCardImage,
                                                   MultipartFile shopImage,
                                                   MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestPayload = objectMapper.writeValueAsString(superDistributor);

        log.info("Starting updateSuperDistributor for ID: {}", id);

        try {
            SuperDistributor existingSuperDistributor = superDistributorRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Super Distributor not found with ID: " + id));

            if (existingSuperDistributor.equals(superDistributor)) {
                log.info("No changes detected for SuperDistributor with ID: {}", id);
                return existingSuperDistributor;
            }

            // Mobile validation
            List<FieldErrorDetailsDTO> errors = new ArrayList<>();
            try {
                phoneNumberValidationService.validatePhoneNumber(superDistributor.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
            }

            if (!errors.isEmpty()) {
                log.warn("Validation failed while updating SuperDistributor with ID: {}, Errors: {}", id, errors);
                throw new CustomValidationException(errors);
            }

            // Update fields selectively
            if (superDistributor.getTitle() != null)
                existingSuperDistributor.setTitle(superDistributor.getTitle());
            if (superDistributor.getName() != null)
                existingSuperDistributor.setName(superDistributor.getName());
            if (superDistributor.getMobileNumber() != null)
                existingSuperDistributor.setMobileNumber(superDistributor.getMobileNumber());
            if (superDistributor.getEmailId() != null)
                existingSuperDistributor.setEmailId(superDistributor.getEmailId());
            if (superDistributor.getDob() != null)
                existingSuperDistributor.setDob(superDistributor.getDob());
            if (superDistributor.getAddress() != null)
                existingSuperDistributor.setAddress(superDistributor.getAddress());
            if (superDistributor.getOutletAddress() != null)
                existingSuperDistributor.setOutletAddress(superDistributor.getOutletAddress());

            // Image updates
            String folder = "Super Distributor";
            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingSuperDistributor::setAadhaarCardFrontImage);
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingSuperDistributor::setAadhaarCardBackImage);
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingSuperDistributor::setPanCardImage);
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingSuperDistributor::setShopImage);
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingSuperDistributor::setBusinessProofImage);
            }

            // Save updated distributor
            SuperDistributor updatedDistributor = superDistributorRepository.save(existingSuperDistributor);
            log.info("SuperDistributor updated successfully with ID: {}", updatedDistributor.getSuperDistributorId());

            // Save hierarchy logs
            LocalDateTime responseDateTime = LocalDateTime.now();
            String responsePayload = objectMapper.writeValueAsString(updatedDistributor);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    responsePayload
            );
            log.info("Hierarchy logs saved for updateSuperDistributor with ID: {}", updatedDistributor.getSuperDistributorId());

            return updatedDistributor;

        } catch (Exception e) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            log.error("Error updating SuperDistributor with ID: {}. Message: {}", id, e.getMessage(), e);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    "Error: " + e.getMessage()
            );

            throw e;
        }
    }


    @Override
    public List<SuperDistributor> getAllSuperDistributors() {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = "Fetch all SuperDistributors";

        try {
            log.info("Fetching all SuperDistributors at {}", requestTime);

            List<SuperDistributor> superDistributors = superDistributorRepository.findAll();

            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Fetched " + superDistributors.size() + " SuperDistributors successfully";

            // Save logs
            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            return superDistributors;
        } catch (Exception e) {
            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Error fetching SuperDistributors: " + e.getMessage();

            // Save error logs
            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.error("Error fetching SuperDistributors", e);
            throw e;
        }
    }


    @Transactional
    @Override
    public void deleteSuperDistributor(String id) {
        log.info("Deleting Super Distributor: {}", id);

        SuperDistributor superDistributor = superDistributorRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("SuperDistributor not found with ID: " + id));

        superDistributorPrepaidAmountRepository.deleteBySuperDistributorId(superDistributor);
        superDistributorPostpaidAmountRepository.deleteBySuperDistributorId(superDistributor);
        superDistributorRepository.delete(superDistributor);
        log.info("SuperDistributor deleted successfully: {}", id);

    }
}

