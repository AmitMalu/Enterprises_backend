package com.im.admin.service.impl;

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

    @Transactional
    @Override
    public SuperDistributor saveSuperDistributor(SuperDistributor superDistributor,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

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

        if (superDistributorRepository.existsByAbbreviation(superDistributor.getAbbreviation())) {
            errors.add(new FieldErrorDetailsDTO("abbreviation",
                    "Abbreviation '" + superDistributor.getAbbreviation() + "' already exists."));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        superDistributor.setSuperDistributorId(idGenerator.generateHierarchyEntityId("Super Distributor", superDistributor.getAbbreviation()));
        String folder = "Super Distributor";

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, superDistributor::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, superDistributor::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, superDistributor::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, superDistributor::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, superDistributor::setBusinessProofImage);

        SuperDistributor savedDistributor = superDistributorRepository.save(superDistributor);

            SuperDistributorPrepaidAmount prepaidAmount = new SuperDistributorPrepaidAmount();
            prepaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPrepaidAmountRepository.save(prepaidAmount);
            log.info("Prepaid amount record created for Super Distributor: {}", superDistributor.getSuperDistributorId());

            SuperDistributorPostpaidAmount postpaidAmount = new SuperDistributorPostpaidAmount();
            postpaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPostpaidAmountRepository.save(postpaidAmount);
            log.info("Postpaid amount record created for Super Distributor: {}", superDistributor.getSuperDistributorId());

            return savedDistributor;

    }

    @Override
    public SuperDistributor getSuperDistributor(String id) {
        return superDistributorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("SuperDistributor not found with id: " + id));
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

        SuperDistributor existingSuperDistributor = superDistributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Super Distributor not found"));

        if (existingSuperDistributor.equals(superDistributor)) {
            return existingSuperDistributor;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(superDistributor.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        if (superDistributor.getTitle() != null) existingSuperDistributor.setTitle(superDistributor.getTitle());
        if (superDistributor.getName() != null) existingSuperDistributor.setName(superDistributor.getName());
        if (superDistributor.getMobileNumber() != null) existingSuperDistributor.setMobileNumber(superDistributor.getMobileNumber());
        if (superDistributor.getAbbreviation() != null) existingSuperDistributor.setAbbreviation(superDistributor.getAbbreviation());
        if (superDistributor.getEmailId() != null) existingSuperDistributor.setEmailId(superDistributor.getEmailId());
        if (superDistributor.getDob() != null) existingSuperDistributor.setDob(superDistributor.getDob());
        if (superDistributor.getOutletName() != null) existingSuperDistributor.setOutletName(superDistributor.getOutletName());
        if (superDistributor.getCompanyType() != null) existingSuperDistributor.setCompanyType(superDistributor.getCompanyType());
        if (superDistributor.getAddress() != null) existingSuperDistributor.setAddress(superDistributor.getAddress());
        if (superDistributor.getOutletAddress() != null) existingSuperDistributor.setOutletAddress(superDistributor.getOutletAddress());

        // Optional image updates
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

        return superDistributorRepository.save(existingSuperDistributor);
    }


    @Override
    public List<SuperDistributor> getAllSuperDistributors() {
        return superDistributorRepository.findAll();
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

