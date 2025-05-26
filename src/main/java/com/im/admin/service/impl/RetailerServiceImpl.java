package com.im.admin.service.impl;

import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.dto.RetailerDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import com.im.admin.entities.RetailerPostpaidAmount;
import com.im.admin.entities.RetailerPrepaidAmount;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.RetailerPostpaidAmountRepository;
import com.im.admin.repositories.RetailerPrepaidAmountRepository;
import com.im.admin.repositories.RetailerRepository;
import com.im.admin.service.RetailerService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RetailerServiceImpl implements RetailerService {

    @Autowired
    private RetailerRepository retailerRepository;

    @Autowired
    private RetailerPrepaidAmountRepository prepaidAmountRepository;

    @Autowired
    private RetailerPostpaidAmountRepository postpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private Validator validator;

    @Autowired
    private ImageHandlerUtil imageHandlerUtil;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Transactional
    @Override
    public Retailer createRetailer(Retailer retailer,
                                   MultipartFile aadhaarCardFrontImage,
                                   MultipartFile aadhaarCardBackImage,
                                   MultipartFile panCardImage,
                                   MultipartFile shopImage,
                                   MultipartFile businessProofImage) throws IOException {

        log.info("Creating new retailer: {}", retailer.getName());

        // Field Validation
        Set<ConstraintViolation<Retailer>> violations = validator.validate(retailer);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(retailer.getMobileNumber());
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

        if (retailerRepository.existsByAbbreviation(retailer.getAbbreviation())) {
            errors.add(new FieldErrorDetailsDTO("abbreviation",
                    "Abbreviation '" + retailer.getAbbreviation() + "' already exists."));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        retailer.setRetailerId(idGenerator.generateHierarchyEntityId("Retailer", retailer.getAbbreviation()));
        String folder = "Retailer";

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, retailer::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, retailer::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, retailer::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, retailer::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, retailer::setBusinessProofImage);


        Retailer savedRetailer = retailerRepository.save(retailer);
        log.info("Retailer created successfully with ID: {}", savedRetailer.getRetailerId());

        RetailerPrepaidAmount prepaidAmount = new RetailerPrepaidAmount();
        prepaidAmount.setRetailerId(savedRetailer);
        prepaidAmountRepository.save(prepaidAmount);
        log.info("Prepaid amount record created for Retailer ID: {}", savedRetailer.getRetailerId());

        RetailerPostpaidAmount postpaidAmount = new RetailerPostpaidAmount();
        postpaidAmount.setRetailerId(savedRetailer);
        postpaidAmountRepository.save(postpaidAmount);
        log.info("Postpaid amount record created for Retailer ID: {}", savedRetailer.getRetailerId());

        return savedRetailer;
    }

    @Transactional
    @Override
    public Retailer updateRetailer(String retailerId,
                                         Retailer retailer,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        Retailer existingRetailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new EntityNotFoundException("Retailer not found"));

        if (existingRetailer.equals(retailer)) {
            return existingRetailer;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(retailer.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        if (retailer.getTitle() != null) existingRetailer.setTitle(retailer.getTitle());
        if (retailer.getName() != null) existingRetailer.setName(retailer.getName());
        if (retailer.getMobileNumber() != null) existingRetailer.setMobileNumber(retailer.getMobileNumber());
        if (retailer.getAbbreviation() != null) existingRetailer.setAbbreviation(retailer.getAbbreviation());
        if (retailer.getEmailId() != null) existingRetailer.setEmailId(retailer.getEmailId());
        if (retailer.getDob() != null) existingRetailer.setDob(retailer.getDob());
        if (retailer.getOutletName() != null) existingRetailer.setOutletName(retailer.getOutletName());
        if (retailer.getCompanyType() != null) existingRetailer.setCompanyType(retailer.getCompanyType());
        if (retailer.getCompetitor() != null) existingRetailer.setCompetitor(retailer.getCompetitor());
        if (retailer.getAddress() != null) existingRetailer.setAddress(retailer.getAddress());
        if (retailer.getOutletAddress() != null) existingRetailer.setOutletAddress(retailer.getOutletAddress());


        // Optional image updates
        String folder = "Retailer";
        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingRetailer::setAadhaarCardFrontImage);
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingRetailer::setAadhaarCardBackImage);
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingRetailer::setPanCardImage);
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingRetailer::setShopImage);
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingRetailer::setBusinessProofImage);
        }

        return retailerRepository.save(existingRetailer);
    }

    @Transactional
    @Override
    public void deleteRetailer(String retailerId) {
        log.info("Deleting retailer with ID: {}", retailerId);

        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new RuntimeException("Retailer not found"));

        // Delete associated Prepaid Amount record
        prepaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .ifPresent(prepaidAmount -> {
                    prepaidAmountRepository.delete(prepaidAmount);
                    log.info("Deleted prepaid amount record for Retailer ID: {}", retailerId);
                });

        // Delete associated Postpaid Amount record
        postpaidAmountRepository.findByRetailerId_RetailerId(retailerId)
                .ifPresent(postpaidAmount -> {
                    postpaidAmountRepository.delete(postpaidAmount);
                    log.info("Deleted postpaid amount record for Retailer ID: {}", retailerId);
                });

        // Delete retailer
        retailerRepository.delete(retailer);
        log.info("Retailer deleted successfully with ID: {}", retailerId);
    }


    @Override
    public List<Retailer> getAllRetailers() {
        log.info("Fetching all retailers");
        return retailerRepository.findAll();
    }

    @Override
    public Optional<Retailer> getRetailerById(String retailerId) {
        log.info("Fetching retailer by ID: {}", retailerId);
        return retailerRepository.findById(retailerId);
    }
}

