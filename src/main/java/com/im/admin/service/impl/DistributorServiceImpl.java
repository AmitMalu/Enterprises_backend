package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.DistributorPostpaidAmount;
import com.im.admin.entities.DistributorPrepaidAmount;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.DistributorPostpaidAmountRepository;
import com.im.admin.repositories.DistributorPrepaidAmountRepository;
import com.im.admin.repositories.DistributorRepository;
import com.im.admin.service.DistributorService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
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
public class DistributorServiceImpl implements DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private DistributorPrepaidAmountRepository prepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository postpaidAmountRepository;

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
    public Distributor createDistributor(Distributor distributor,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        LocalDateTime RequestDateTime = LocalDateTime.now();

        // Field Validation
        Set<ConstraintViolation<Distributor>> violations = validator.validate(distributor);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(distributor.getMobileNumber());
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

        if (distributorRepository.existsByAbbreviation(distributor.getAbbreviation())) {
            errors.add(new FieldErrorDetailsDTO("abbreviation",
                    "Abbreviation '" + distributor.getAbbreviation() + "' already exists."));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        distributor.setDistributorId(idGenerator.generateHierarchyEntityId("Distributor", distributor.getAbbreviation()));
        String folder = "Distributor";

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, distributor::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, distributor::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, distributor::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, distributor::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, distributor::setBusinessProofImage);

        // Save distributor and amounts
        Distributor savedDistributor = distributorRepository.save(distributor);

        DistributorPrepaidAmount prepaid = new DistributorPrepaidAmount();
        prepaid.setDistributorId(savedDistributor);
        prepaidAmountRepository.save(prepaid);

        DistributorPostpaidAmount postpaid = new DistributorPostpaidAmount();
        postpaid.setDistributorId(savedDistributor);
        postpaidAmountRepository.save(postpaid);


        LocalDateTime ResponseDateTime = LocalDateTime.now();
        String DistributorRequest  = objectMapper.writeValueAsString(distributor);
        String DistributorResponse  = objectMapper.writeValueAsString(savedDistributor);

        commonFunctions.saveHierarchyLogs(
                RequestDateTime,
                DistributorRequest,
                null, // IP address as null
                ResponseDateTime,
                DistributorResponse
        );




        return savedDistributor;
    }

    @Transactional
    @Override
    public Distributor updateDistributor(String id,
                                         Distributor distributor,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        Distributor existingDistributor = distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        if (existingDistributor.equals(distributor)) {
            return existingDistributor;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(distributor.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        if (distributor.getTitle() != null) existingDistributor.setTitle(distributor.getTitle());
        if (distributor.getName() != null) existingDistributor.setName(distributor.getName());
        if (distributor.getMobileNumber() != null) existingDistributor.setMobileNumber(distributor.getMobileNumber());
        if (distributor.getAbbreviation() != null) existingDistributor.setAbbreviation(distributor.getAbbreviation());
        if (distributor.getEmailId() != null) existingDistributor.setEmailId(distributor.getEmailId());
        if (distributor.getDob() != null) existingDistributor.setDob(distributor.getDob());
        if (distributor.getOutletName() != null) existingDistributor.setOutletName(distributor.getOutletName());
        if (distributor.getCompanyType() != null) existingDistributor.setCompanyType(distributor.getCompanyType());
        if (distributor.getCompetitor() != null) existingDistributor.setCompetitor(distributor.getCompetitor());
        if (distributor.getAddress() != null) existingDistributor.setAddress(distributor.getAddress());
        if (distributor.getOutletAddress() != null) existingDistributor.setOutletAddress(distributor.getOutletAddress());


        // Optional image updates
        String folder = "Distributor";
        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingDistributor::setAadhaarCardFrontImage);
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingDistributor::setAadhaarCardBackImage);
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingDistributor::setPanCardImage);
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingDistributor::setShopImage);
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingDistributor::setBusinessProofImage);
        }

        return distributorRepository.save(existingDistributor);
    }


    //Get All Distributors
    @Override
    public List<Distributor> getAllDistributors() {
        return distributorRepository.findAll();
    }

    @Override
    public Distributor getDistributorById(String id) {
        return distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));
    }

    @Transactional
    @Override
    public void deleteDistributor(String id) {
        Distributor distributor = distributorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Distributor not found"));

        prepaidAmountRepository.deleteByDistributorId(distributor);
        postpaidAmountRepository.deleteByDistributorId(distributor);
        distributorRepository.delete(distributor);
    }

}
