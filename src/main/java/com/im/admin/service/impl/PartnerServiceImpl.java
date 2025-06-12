package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Partner;
import com.im.admin.entities.PartnerPostpaidAmount;
import com.im.admin.entities.PartnerPrepaidAmount;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.PartnerPostpaidAmountRepository;
import com.im.admin.repositories.PartnerPrepaidAmountRepository;
import com.im.admin.repositories.PartnerRepository;
import com.im.admin.service.PartnerService;
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
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PartnerServiceImpl implements PartnerService {

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private PartnerPrepaidAmountRepository partnerPrepaidAmountRepository;

    @Autowired
    private PartnerPostpaidAmountRepository partnerPostpaidAmountRepository;

    @Autowired
    private IdGenerator idGenerator; // Assuming you have an ID generator utility

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
    public Partner savePartner(Partner partner,
                               MultipartFile aadhaarCardFrontImage,
                               MultipartFile aadhaarCardBackImage,
                               MultipartFile panCardImage, MultipartFile shopImage,
                               MultipartFile businessProofImage) throws IOException {

        LocalDateTime RequestDateTime = LocalDateTime.now();
        // Field Validation
        Set<ConstraintViolation<Partner>> violations = validator.validate(partner);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(partner.getMobileNumber());
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

//        if (partnerRepository.existsByAbbreviation(partner.getAbbreviation())) {
//            errors.add(new FieldErrorDetailsDTO("abbreviation",
//                    "Abbreviation '" + partner.getAbbreviation() + "' already exists."));
//        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        //partner.setPartnerId(idGenerator.generateHierarchyEntityId("Partner", partner.getAbbreviation()));
        String folder = "Partner";

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, partner::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, partner::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, partner::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, partner::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, partner::setBusinessProofImage);

        Partner savedPartner = partnerRepository.save(partner);

        PartnerPrepaidAmount prepaidAmount = new PartnerPrepaidAmount();
        prepaidAmount.setPartnerId(savedPartner);
        partnerPrepaidAmountRepository.save(prepaidAmount);

        PartnerPostpaidAmount postpaidAmount = new PartnerPostpaidAmount();
        postpaidAmount.setPartnerId(savedPartner);
        partnerPostpaidAmountRepository.save(postpaidAmount);


        LocalDateTime ResponseDateTime = LocalDateTime.now();
        String PartnerRequest  = objectMapper.writeValueAsString(partner);
        String PartnerResponse  = objectMapper.writeValueAsString(savedPartner);

        commonFunctions.saveHierarchyLogs(
                RequestDateTime,
                PartnerRequest,
                null, // IP address as null
                ResponseDateTime,
                PartnerResponse
        );

        log.info("Successfully saved Partner: {}", savedPartner.getPartnerId());
        return savedPartner;
    }

    @Transactional
    @Override
    public Partner updatePartner(String id,
                                         Partner partner,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        Partner existingPartner = partnerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Partner not found"));

        if (existingPartner.equals(partner)) {
            return existingPartner;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(partner.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        if (partner.getTitle() != null) existingPartner.setTitle(partner.getTitle());
        if (partner.getName() != null) existingPartner.setName(partner.getName());
        if (partner.getMobileNumber() != null) existingPartner.setMobileNumber(partner.getMobileNumber());
        //if (partner.getAbbreviation() != null) existingPartner.setAbbreviation(partner.getAbbreviation());
        if (partner.getEmailId() != null) existingPartner.setEmailId(partner.getEmailId());
        if (partner.getDob() != null) existingPartner.setDob(partner.getDob());
        if (partner.getAddress() != null) existingPartner.setAddress(partner.getAddress());
        if (partner.getOutletAddress() != null) existingPartner.setOutletAddress(partner.getOutletAddress());


        // Optional image updates
        String folder = "Partner";
        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingPartner::setAadhaarCardFrontImage);
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingPartner::setAadhaarCardBackImage);
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingPartner::setPanCardImage);
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingPartner::setShopImage);
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingPartner::setBusinessProofImage);
        }

        return partnerRepository.save(existingPartner);
    }

    @Override
    public List<Partner> getAllPartners() {
        log.info("Fetching all Partners");
        return partnerRepository.findAll();
    }

    @Override
    public Partner getPartnerById(String partnerId) {
        log.info("Fetching Partner with ID: {}", partnerId);
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partner not found"));
    }

    @Override
    public void deletePartner(String partnerId) {
        log.info("Deleting Partner with ID: {}", partnerId);

        Partner partner = partnerRepository.findById(partnerId).
                orElseThrow(() -> new EntityNotFoundException("Partner Not Found " + partnerId));

        partnerPostpaidAmountRepository.deleteByPartnerId(partner);
        partnerPrepaidAmountRepository.deleteByPartnerId(partner);
        partnerRepository.deleteById(partnerId);
        log.info("Successfully deleted Partner with ID: {}", partnerId);
    }
}


