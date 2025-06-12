package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Enterprises;
import com.im.admin.entities.EnterprisesPostpaidAmount;
import com.im.admin.entities.EnterprisesPrepaidAmount;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.EnterprisesPostpaidAmountRepository;
import com.im.admin.repositories.EnterprisesPrepaidAmountRepository;
import com.im.admin.repositories.EnterprisesRepository;
import com.im.admin.service.EnterprisesService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
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

    @Transactional
    @Override
    public Enterprises saveEnterprise(Enterprises enterprises,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        LocalDateTime RequestDateTime = LocalDateTime.now();

        // Field Validation
        Set<ConstraintViolation<Enterprises>> violations = validator.validate(enterprises);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(enterprises.getMobileNumber());
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
            throw new CustomValidationException(errors);
        }

        String abbreviation = enterprises.getAbbreviation();
        if (abbreviation == null || abbreviation.isBlank()) {
            String parentAccount = enterprises.getParentAccount();
            if (parentAccount == null || !parentAccount.contains("-")) {
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            // Extract prefix from parentAccount
            abbreviation = parentAccount.split("-")[0];

            // Optionally validate abbreviation format
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }

            enterprises.setAbbreviation(abbreviation);
        }

        String folder = "Enterprises";
        enterprises.setEnterpriseId(idGenerator.generateHierarchyEntityId(folder, enterprises.getAbbreviation()));

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, enterprises::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, enterprises::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, enterprises::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, enterprises::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, enterprises::setBusinessProofImage);

        Enterprises savedEnterprise = enterpriseRepository.save(enterprises);

        EnterprisesPrepaidAmount prepaid = new EnterprisesPrepaidAmount();
        prepaid.setEnterprise(savedEnterprise);
        prepaidRepository.save(prepaid);

        EnterprisesPostpaidAmount postpaid = new EnterprisesPostpaidAmount();
        postpaid.setEnterprise(savedEnterprise);
        postpaidRepository.save(postpaid);

        LocalDateTime ResponseDateTime = LocalDateTime.now();
        String EnterpriseRequest  = objectMapper.writeValueAsString(enterprises);
        String EnterpriseResponse  = objectMapper.writeValueAsString(savedEnterprise);

        commonFunctions.saveHierarchyLogs(
                RequestDateTime,
                EnterpriseRequest,
                null, // IP address as null
                ResponseDateTime,
                EnterpriseResponse
        );
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

        Enterprises existingEnterprises = enterpriseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        if (existingEnterprises.equals(enterprises)) {
            return existingEnterprises;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(enterprises.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        //if (enterprises.getTit != null) existingEnterprises.setTitle(enterprises.getTitle());
        //if (enterprises.getName() != null) existingEnterprises.setName(enterprises.getName());
        if (enterprises.getMobileNumber() != null) existingEnterprises.setMobileNumber(enterprises.getMobileNumber());
        if (enterprises.getAbbreviation() != null) existingEnterprises.setAbbreviation(enterprises.getAbbreviation());
        if (enterprises.getEmailId() != null) existingEnterprises.setEmailId(enterprises.getEmailId());
        //if (enterprises.getDob() != null) existingEnterprises.setDob(enterprises.getDob());
        //if (enterprises.getOutletName() != null) existingEnterprises.setOutletName(enterprises.getOutletName());
        //if (enterprises.getCompetitor() != null) existingEnterprises.setCompetitor(enterprises.getCompetitor());
        if (enterprises.getAddress() != null) existingEnterprises.setAddress(enterprises.getAddress());
        if (enterprises.getOutletAddress() != null) existingEnterprises.setOutletAddress(enterprises.getOutletAddress());


        // Optional image updates
        String folder = "Enterprises";
        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingEnterprises::setAadhaarCardFrontImage);
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingEnterprises::setAadhaarCardBackImage);
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingEnterprises::setPanCardImage);
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingEnterprises::setShopImage);
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingEnterprises::setBusinessProofImage);
        }

        return enterpriseRepository.save(existingEnterprises);
    }

    @Override
    public List<Enterprises> getAllEnterprises() {
        return enterpriseRepository.findAll();
    }

    @Override
    public Enterprises getEnterpriseById(String enterpriseId) {
        return enterpriseRepository.findById(enterpriseId)
                .orElseThrow(() -> new RuntimeException("Enterprise not found with ID: " + enterpriseId));
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



