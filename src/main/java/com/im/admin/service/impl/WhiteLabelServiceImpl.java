package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.WhiteLabelPostpaidAmount;
import com.im.admin.entities.WhiteLabelPrepaidAmount;
import com.im.admin.entities.WhiteLabels;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.WhiteLabelPostpaidAmountRepository;
import com.im.admin.repositories.WhiteLabelPrepaidAmountRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.service.WhiteLabelService;
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
@Slf4j
@RequiredArgsConstructor
public class WhiteLabelServiceImpl implements WhiteLabelService {

    @Autowired
    private  WhiteLabelRepository whiteLabelRepository;

    @Autowired
    private WhiteLabelPrepaidAmountRepository prepaidRepository;

    @Autowired
    private WhiteLabelPostpaidAmountRepository postpaidRepository;

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
    public WhiteLabels saveWhiteLabel(WhiteLabels whiteLabels,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        LocalDateTime RequestDateTime = LocalDateTime.now();

        // Field Validation
        Set<ConstraintViolation<WhiteLabels>> violations = validator.validate(whiteLabels);
        List<FieldErrorDetailsDTO> errors = violations.stream()
                .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                .collect(Collectors.toList());

        // Phone validation
        try {
            phoneNumberValidationService.validatePhoneNumber(whiteLabels.getMobileNumber());
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

        String abbreviation = whiteLabels.getAbbreviation();
        if (abbreviation == null || abbreviation.isBlank()) {
            String parentAccount = whiteLabels.getParentAccount();
            if (parentAccount == null || !parentAccount.contains("-")) {
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            // Extract prefix from parentAccount
            abbreviation = parentAccount.split("-")[0];

            // Optionally validate abbreviation format
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }

            whiteLabels.setAbbreviation(abbreviation);
        }

        String folder = "White Label";
        whiteLabels.setWhiteLabelId(idGenerator.generateHierarchyEntityId(folder, whiteLabels.getAbbreviation()));


        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, whiteLabels::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, whiteLabels::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, whiteLabels::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, whiteLabels::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, whiteLabels::setBusinessProofImage);

        WhiteLabels savedWhiteLabel = whiteLabelRepository.save(whiteLabels);
        log.info("WhiteLabel saved with ID: {}", savedWhiteLabel.getWhiteLabelId());

        // Save Prepaid Record
        WhiteLabelPrepaidAmount prepaid = new WhiteLabelPrepaidAmount();
        prepaid.setWhiteLabelId(savedWhiteLabel);
        prepaidRepository.save(prepaid);
        log.info("Prepaid amount record created for WhiteLabel: {}", savedWhiteLabel.getWhiteLabelId());

        // Save Postpaid Record
        WhiteLabelPostpaidAmount postpaid = new WhiteLabelPostpaidAmount();
        postpaid.setWhiteLabelId(savedWhiteLabel);
        postpaidRepository.save(postpaid);
        log.info("Postpaid amount record created for WhiteLabel: {}", savedWhiteLabel.getWhiteLabelId());

        LocalDateTime ResponseDateTime = LocalDateTime.now();
        String WhiteLabelRequest  = objectMapper.writeValueAsString(whiteLabels);
        String WhiteLableResponse  = objectMapper.writeValueAsString(savedWhiteLabel);


        commonFunctions.saveHierarchyLogs(
                RequestDateTime,
                WhiteLabelRequest,
                null, // IP address as null
                ResponseDateTime,
                WhiteLableResponse
        );

        return savedWhiteLabel;
    }

    @Transactional
    @Override
    public WhiteLabels updateWhiteLabel(String id,
                                        WhiteLabels whiteLabels,
                                        MultipartFile aadhaarCardFrontImage,
                                        MultipartFile aadhaarCardBackImage,
                                        MultipartFile panCardImage,
                                        MultipartFile shopImage,
                                        MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();

        WhiteLabels existingWhiteLabels = whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found"));

        // Capture request object (incoming update data)
        String whiteLabelRequest = objectMapper.writeValueAsString(whiteLabels);

        if (existingWhiteLabels.equals(whiteLabels)) {
            return existingWhiteLabels;
        }

        // Validate mobile
        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(whiteLabels.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        // Update non-null fields
        if (whiteLabels.getTitle() != null) existingWhiteLabels.setTitle(whiteLabels.getTitle());
        if (whiteLabels.getMobileNumber() != null) existingWhiteLabels.setMobileNumber(whiteLabels.getMobileNumber());
        if (whiteLabels.getAbbreviation() != null) existingWhiteLabels.setAbbreviation(whiteLabels.getAbbreviation());
        if (whiteLabels.getEmailId() != null) existingWhiteLabels.setEmailId(whiteLabels.getEmailId());
        if (whiteLabels.getAddress() != null) existingWhiteLabels.setAddress(whiteLabels.getAddress());
        if (whiteLabels.getIsAdmin() != null) existingWhiteLabels.setIsAdmin(whiteLabels.getIsAdmin());

        // Optional image updates
        String folder = "White Label";
        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, existingWhiteLabels::setAadhaarCardFrontImage);
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, existingWhiteLabels::setAadhaarCardBackImage);
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, existingWhiteLabels::setPanCardImage);
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, existingWhiteLabels::setShopImage);
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, existingWhiteLabels::setBusinessProofImage);
        }

        WhiteLabels updatedWhiteLabel = whiteLabelRepository.save(existingWhiteLabels);

        LocalDateTime responseDateTime = LocalDateTime.now();

        String whiteLabelResponse = objectMapper.writeValueAsString(updatedWhiteLabel);

        // Save logs
        commonFunctions.saveHierarchyLogs(
                requestDateTime,
                whiteLabelRequest,
                null, // IP address as null
                responseDateTime,
                whiteLabelResponse
        );
        return updatedWhiteLabel;
    }


    @Override
    public List<WhiteLabels> getAllWhiteLabels() {
        log.info("Fetching all WhiteLabels...");
        return whiteLabelRepository.findAll();
    }

    @Override
    public WhiteLabels getWhiteLabelById(String id) {
        log.info("Fetching WhiteLabel by ID: {}", id);
        return whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));
    }

    @Transactional
    @Override
    public void deleteWhiteLabel(String id) {
        log.info("Deleting WhiteLabel: {}", id);
        WhiteLabels whiteLabel = whiteLabelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));

        prepaidRepository.deleteByWhiteLabelId(whiteLabel);
        postpaidRepository.deleteByWhiteLabelId(whiteLabel);
        whiteLabelRepository.delete(whiteLabel);
        log.info("WhiteLabel deleted successfully: {}", id);
    }
}

