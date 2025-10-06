package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.dto.WhiteLabelDropdownDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.repositories.WhiteLabelPostpaidAmountRepository;
import com.im.admin.repositories.WhiteLabelPrepaidAmountRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.service.WhiteLabelService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
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

    @Autowired
    private ImageUtil imageUtil;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Transactional
    @Override
    public WhiteLabels saveWhiteLabel(WhiteLabels whiteLabels,
                                      MultipartFile aadhaarCardFrontImage,
                                      MultipartFile aadhaarCardBackImage,
                                      MultipartFile panCardImage,
                                      MultipartFile shopImage,
                                      MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String whiteLabelRequest = objectMapper.writeValueAsString(whiteLabels);

        log.info("Request received to save WhiteLabel: {}", whiteLabelRequest);

        try {
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

            // Handle abbreviation
            String abbreviation = whiteLabels.getAbbreviation();
            if (abbreviation == null || abbreviation.isBlank()) {
                String parentAccount = whiteLabels.getParentAccount();
                if (parentAccount == null || !parentAccount.contains("-")) {
                    throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
                }

                abbreviation = parentAccount.split("-")[0];

                if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                    throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
                }
                whiteLabels.setAbbreviation(abbreviation);
            }

            // Generate ID
            String folder = "White Label";
            whiteLabels.setWhiteLabelId(idGenerator.generateHierarchyEntityId(folder, whiteLabels.getAbbreviation()));

            // Upload Images
            whiteLabels.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, "whiteLabel"));
            whiteLabels.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, "whiteLabel"));
            whiteLabels.setPanCardImage(imageUtil.saveImage(panCardImage, "whiteLabel"));
            whiteLabels.setShopImage(imageUtil.saveImage(shopImage, "whiteLabel"));
            whiteLabels.setBusinessNumber( imageUtil.saveImage(businessProofImage, "whiteLabel"));

            // Save Bank Details separately (Before whiteLabels save)
            if (whiteLabels.getBankDetails() != null) {
                BanksDetails bankDetails = whiteLabels.getBankDetails();
                bankDetails.setHierarchyId(whiteLabels.getWhiteLabelId()); // set after ID is generated
                bankDetails.setBankName(whiteLabels.getBankDetails().getBankName());
                bankDetails.setBankHolderName(whiteLabels.getBankDetails().getBankHolderName());
                bankDetails.setBankAccountNumber(whiteLabels.getBankDetails().getBankAccountNumber());
                bankDetails.setIsVerified(whiteLabels.getBankDetails().getIsVerified());
                bankDetails.setIfscCode(whiteLabels.getBankDetails().getIfscCode());
                bankDetails.setCreatedBy(whiteLabels.getCreatedBy());

                // Save bank details
                BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

                // Attach it to whiteLabels (foreign key)
                whiteLabels.setBankDetails(savedBankDetails);
            }

            // Save entity
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

            // Response
            LocalDateTime responseDateTime = LocalDateTime.now();
            String whiteLabelResponse = objectMapper.writeValueAsString(savedWhiteLabel);

            // Save hierarchy logs
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    whiteLabelRequest,
                    null,
                    responseDateTime,
                    whiteLabelResponse
            );

            log.info("WhiteLabel creation successful. ID: {}", savedWhiteLabel.getWhiteLabelId());
            return savedWhiteLabel;

        } catch (Exception ex) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            String errorResponse = "Error while saving WhiteLabel: " + ex.getMessage();

            // Save failure log
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    whiteLabelRequest,
                    null,
                    responseDateTime,
                    errorResponse
            );

            log.error("Failed to save WhiteLabel. Request: {}", whiteLabelRequest, ex);
            throw ex;
        }
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
        String whiteLabelRequest = objectMapper.writeValueAsString(whiteLabels);

        log.info("Request received to update WhiteLabel ID: {} with data: {}", id, whiteLabelRequest);

        try {
            WhiteLabels existingWhiteLabels = whiteLabelRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("WhiteLabel not found with ID: " + id));

            if (existingWhiteLabels.equals(whiteLabels)) {
                log.info("No changes detected for WhiteLabel ID: {}. Skipping update.", id);
                return existingWhiteLabels;
            }

            // Validate mobile number
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

            // Handle optional image updates
            String folder = "White Label";
            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                whiteLabels.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                whiteLabels.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                whiteLabels.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                whiteLabels.setShopImage(imageUtil.saveImage(shopImage, folder));
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                whiteLabels.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
            }
            // Save updated entity
            WhiteLabels updatedWhiteLabel = whiteLabelRepository.save(existingWhiteLabels);

            LocalDateTime responseDateTime = LocalDateTime.now();
            String whiteLabelResponse = objectMapper.writeValueAsString(updatedWhiteLabel);

            // Save logs (success case)
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    whiteLabelRequest,
                    null,
                    responseDateTime,
                    whiteLabelResponse
            );

            log.info("WhiteLabel updated successfully. ID: {}", updatedWhiteLabel.getWhiteLabelId());
            return updatedWhiteLabel;

        } catch (Exception ex) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            String errorResponse = "Error updating WhiteLabel: " + ex.getMessage();

            // Save failure logs
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    whiteLabelRequest,
                    null,
                    responseDateTime,
                    errorResponse
            );

            log.error("Failed to update WhiteLabel ID: {}. Request: {}", id, whiteLabelRequest, ex);
            throw ex;
        }
    }


    @Override
    public List<WhiteLabels> getAllWhiteLabels() {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = "Fetching all WhiteLabels";
        log.info("Request to fetch all WhiteLabels at {}", requestTime);

        try {
            List<WhiteLabels> whiteLabels = whiteLabelRepository.findAll();

            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Fetched " + whiteLabels.size() + " WhiteLabels";

            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.info("Response for fetching WhiteLabels at {}", responseTime);

            return whiteLabels;

        } catch (Exception e) {
            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Error fetching WhiteLabels: " + e.getMessage();

            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.error("Exception while fetching WhiteLabels at {}: {}", responseTime, e.getMessage(), e);

            throw new RuntimeException("Failed to fetch WhiteLabels", e);
        }
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

    @Override
    public List<WhiteLabelDropdownDTO> getAdminWhiteLabels() {
        return whiteLabelRepository.findByIsAdminTrue().stream()
                .map(wl -> new WhiteLabelDropdownDTO(wl.getWhiteLabelId(), wl.getName()))
                .collect(Collectors.toList());
    }
}

