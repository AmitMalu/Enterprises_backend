package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.repositories.EnterprisesPostpaidAmountRepository;
import com.im.admin.repositories.EnterprisesPrepaidAmountRepository;
import com.im.admin.repositories.EnterprisesRepository;
import com.im.admin.response.EnterpriseResponseDTO;
import com.im.admin.service.EnterprisesService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
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
@Slf4j
public class EnterprisesServiceImpl implements EnterprisesService {

    @Autowired
    private EnterprisesRepository enterpriseRepository;

    @Autowired
    private EnterprisesPrepaidAmountRepository prepaidRepository;

    @Autowired
    private EnterprisesPostpaidAmountRepository postpaidRepository;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private ImageHandlerUtil imageHandlerUtil;

    @Autowired
    private Validator validator;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ImageUtil imageUtil;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Transactional
    @Override
    public Enterprises saveEnterprise(Enterprises enterprises,
                                      MultipartFile aadhaarCardFrontImage,
                                      MultipartFile aadhaarCardBackImage,
                                      MultipartFile panCardImage,
                                      MultipartFile shopImage,
                                      MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        log.info("Request to save Enterprise received at {}: {}", requestDateTime, enterprises);

        // ===== Field Validation =====
        Set<ConstraintViolation<Enterprises>> violations = validator.validate(enterprises);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .collect(Collectors.toList());

        // ===== Phone Validation =====
        try {
            phoneNumberValidationService.validatePhoneNumber(enterprises.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            log.warn("Phone validation failed for enterprise [{}]: {}", enterprises.getName(), ex.getMessage());
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        // ===== Image Validation =====
        Map<String, MultipartFile> imageMap = new HashMap<>();
        imageMap.put("aadhaarCardFrontImage", aadhaarCardFrontImage);
        imageMap.put("aadhaarCardBackImage", aadhaarCardBackImage);
        imageMap.put("panCardImage", panCardImage);
        imageMap.put("shopImage", shopImage);
        imageMap.put("businessProofImage", businessProofImage);

        for (Map.Entry<String, MultipartFile> entry : imageMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                log.warn("Missing required image: {}", entry.getKey());
                errors.add(new FieldErrorDetailsDTO(entry.getKey(), "image is required"));
            }
        }

        if (!errors.isEmpty()) {
            log.error("Validation failed for Enterprise [{}], Errors: {}", enterprises.getName(), errors);
            throw new CustomValidationException(errors);
        }

        // ===== Abbreviation Handling =====
        String abbreviation = enterprises.getAbbreviation();
        if (abbreviation == null || abbreviation.isBlank()) {
            String parentAccount = enterprises.getParentAccount();
            if (parentAccount == null || !parentAccount.contains("-")) {
                log.error("Invalid parentAccount format: {}", parentAccount);
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            abbreviation = parentAccount.split("-")[0];
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                log.error("Derived abbreviation '{}' is invalid", abbreviation);
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }

            enterprises.setAbbreviation(abbreviation);
        }

        String folder = "Enterprises";
        enterprises.setEnterpriseId(idGenerator.generateHierarchyEntityId(folder, enterprises.getAbbreviation()));

        // ===== Image Upload =====
        log.info("Uploading images for Enterprise [{}]", enterprises.getEnterpriseId());
        enterprises.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, "Enterprises"));
        enterprises.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, "Enterprises"));
        enterprises.setPanCardImage(imageUtil.saveImage(panCardImage, "Enterprises"));
        enterprises.setShopImage(imageUtil.saveImage(shopImage, "Enterprises"));
        enterprises.setBusinessNumber( imageUtil.saveImage(businessProofImage, "Enterprises"));

        // Save Bank Details separately (Before enterprises save)
        if (enterprises.getBankDetails() != null) {
            BanksDetails bankDetails = enterprises.getBankDetails();
            bankDetails.setHierarchyId(enterprises.getEnterpriseId()); // set after ID is generated
            bankDetails.setBankName(enterprises.getBankDetails().getBankName());
            bankDetails.setBankHolderName(enterprises.getBankDetails().getBankHolderName());
            bankDetails.setBankAccountNumber(enterprises.getBankDetails().getBankAccountNumber());
            bankDetails.setIsVerified(enterprises.getBankDetails().getIsVerified());
            bankDetails.setIfscCode(enterprises.getBankDetails().getIfscCode());
            bankDetails.setCreatedBy(enterprises.getCreatedBy());

            // Save bank details
            BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

            // Attach it to enterprises (foreign key)
            enterprises.setBankDetails(savedBankDetails);
        }

        // ===== Save Enterprise =====
        Enterprises savedEnterprise = enterpriseRepository.save(enterprises);
        log.info("Enterprise [{}] saved successfully with ID: {}", savedEnterprise.getName(), savedEnterprise.getEnterpriseId());

        // ===== Initialize Amounts =====
        EnterprisesPrepaidAmount prepaid = new EnterprisesPrepaidAmount();
        prepaid.setEnterprise(savedEnterprise);
        prepaidRepository.save(prepaid);

        EnterprisesPostpaidAmount postpaid = new EnterprisesPostpaidAmount();
        postpaid.setEnterprise(savedEnterprise);
        postpaidRepository.save(postpaid);

        log.info("Prepaid and Postpaid amounts initialized for Enterprise [{}]", savedEnterprise.getEnterpriseId());

        // ===== Save Logs =====
        LocalDateTime responseDateTime = LocalDateTime.now();
        String enterpriseRequest = objectMapper.writeValueAsString(enterprises);
        String enterpriseResponse = objectMapper.writeValueAsString(savedEnterprise);

        commonFunctions.saveHierarchyLogs(
                requestDateTime,
                enterpriseRequest,
                null, // IP address as null
                responseDateTime,
                enterpriseResponse
        );

        log.info("Hierarchy logs saved for Enterprise [{}] at {}", savedEnterprise.getEnterpriseId(), responseDateTime);

        return savedEnterprise;
    }

    @Transactional
    @Override
    public Enterprises updateEnterprise(String id,
                                        Enterprises enterprises,
                                        MultipartFile aadhaarCardFrontImage,
                                        MultipartFile aadhaarCardBackImage,
                                        MultipartFile panCardImage,
                                        MultipartFile shopImage,
                                        MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        log.info("Request received to update Enterprise with ID [{}] at {}: {}", id, requestDateTime, enterprises);

        Enterprises existingEnterprises = enterpriseRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Enterprise not found with ID: {}", id);
                    return new EntityNotFoundException("Enterprise not found with ID: " + id);
                });

        String enterpriseRequest = objectMapper.writeValueAsString(enterprises);

        // Skip update if identical
        if (existingEnterprises.equals(enterprises)) {
            log.info("No changes detected for Enterprise [{}]. Skipping update.", id);
            return existingEnterprises;
        }

//        // ===== Mobile Validation =====
//        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
//        try {
//            phoneNumberValidationService.validatePhoneNumber(enterprises.getMobileNumber());
//        } catch (IllegalArgumentException ex) {
//            log.warn("Mobile validation failed for Enterprise [{}]: {}", id, ex.getMessage());
//            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
//        }
//
//        if (!errors.isEmpty()) {
//            log.error("Validation errors for Enterprise [{}]: {}", id, errors);
//            throw new CustomValidationException(errors);
//        }

        // ===== Update Fields =====
//        if (enterprises.getMobileNumber() != null) existingEnterprises.setMobileNumber(enterprises.getMobileNumber());
        if (enterprises.getAbbreviation() != null) existingEnterprises.setAbbreviation(enterprises.getAbbreviation());
        if (enterprises.getEmailId() != null) existingEnterprises.setEmailId(enterprises.getEmailId());
        if (enterprises.getAddress() != null) existingEnterprises.setAddress(enterprises.getAddress());
        if (enterprises.getOutletAddress() != null) existingEnterprises.setOutletAddress(enterprises.getOutletAddress());

        log.info("Fields updated for Enterprise [{}]. Proceeding with image uploads if provided.", id);

        // ===== Optional Image Uploads =====
        String folder = "Enterprises";

        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            existingEnterprises.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            existingEnterprises.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            existingEnterprises.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            existingEnterprises.setShopImage(imageUtil.saveImage(shopImage, folder));
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            existingEnterprises.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
        }
        // ===== Save Updated Enterprise =====
        Enterprises updatedEnterprise = enterpriseRepository.save(existingEnterprises);
        log.info("Enterprise [{}] updated successfully.", updatedEnterprise.getEnterpriseId());

        // ===== Save Hierarchy Logs =====
        LocalDateTime responseDateTime = LocalDateTime.now();
        String enterpriseResponse = objectMapper.writeValueAsString(updatedEnterprise);

        commonFunctions.saveHierarchyLogs(
                requestDateTime,
                enterpriseRequest,
                null, // IP address as null
                responseDateTime,
                enterpriseResponse
        );

        log.info("Hierarchy logs saved for Enterprise [{}] at {}", updatedEnterprise.getEnterpriseId(), responseDateTime);

        return updatedEnterprise;
    }

    @Override
    public List<Enterprises> getAllEnterprises() throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String enterpriseRequest = "Fetching all enterprises";

        List<Enterprises> enterprises = null;
        String enterpriseResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            enterprises = enterpriseRepository.findAll();
            enterpriseResponse = objectMapper.writeValueAsString(enterprises);
            responseDateTime = LocalDateTime.now();

            return enterprises;
        } catch (Exception ex) {
            enterpriseResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;
        } finally {
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    enterpriseRequest,
                    null, // ipAddress if available
                    responseDateTime,
                    enterpriseResponse
            );
        }
    }

    @Override
    public EnterpriseResponseDTO getEnterpriseById(String enterpriseId) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String enterpriseRequest = "Fetching enterprise with ID: " + enterpriseId;

        EnterpriseResponseDTO dto = null;
        String enterpriseResponse = null;
        LocalDateTime responseDateTime = null;

        try {

            Enterprises enterprise = enterpriseRepository.findById(enterpriseId)
                    .orElseThrow(() ->
                            new RuntimeException("Enterprise not found with ID: " + enterpriseId)
                    );

            dto = convertToDto(enterprise);

            enterpriseResponse = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();

            return dto;

        } catch (Exception ex) {

            enterpriseResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;

        } finally {

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    enterpriseRequest,
                    null,
                    responseDateTime,
                    enterpriseResponse
            );
        }
    }

    private EnterpriseResponseDTO convertToDto(Enterprises enterprise) {

        EnterpriseResponseDTO dto = new EnterpriseResponseDTO();

        dto.setEnterpriseId(enterprise.getEnterpriseId());
        dto.setParentAccount(enterprise.getParentAccount());
        dto.setName(enterprise.getName());
        dto.setMobileNumber(enterprise.getMobileNumber());
        dto.setEmailId(enterprise.getEmailId());
        dto.setTitle(enterprise.getTitle());
        dto.setAbbreviation(enterprise.getAbbreviation());
        dto.setDob(enterprise.getDob());
        dto.setStatus(enterprise.getStatus());
        dto.setDomainURL(enterprise.getDomainURL());
        dto.setBusinessDesc(enterprise.getBusinessDesc());
        dto.setPanNumber(enterprise.getPanNumber());
        dto.setAadhaarNumber(enterprise.getAadhaarNumber());
        dto.setPanVerified(enterprise.getPanVerified());
        dto.setAadhaarPanVerified(enterprise.getAadhaarPanVerified());
        dto.setCreatedBy(enterprise.getCreatedBy());
        dto.setCreatedOn(enterprise.getCreatedOn());
        dto.setAadhaarCardFrontImage(enterprise.getAadhaarCardFrontImage());
        dto.setAadhaarCardBackImage(enterprise.getAadhaarCardBackImage());
        dto.setPanCardImage(enterprise.getPanCardImage());
        dto.setShopImage(enterprise.getShopImage());
        dto.setBusinessProofImage(enterprise.getBusinessProofImage());
        return dto;
    }


    @Transactional
    @Override
    public void deleteEnterprise(String enterpriseId) {
        Enterprises enterprise = enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));

        prepaidRepository.deleteByEnterprise(enterprise);
        postpaidRepository.deleteByEnterprise(enterprise);
        enterpriseRepository.delete(enterprise);
    }
}
