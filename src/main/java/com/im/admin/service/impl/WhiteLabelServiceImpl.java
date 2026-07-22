package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.BankDetailsDTO;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.dto.WhiteLabelDropdownDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.properties.FileUrlUtil;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.repositories.WhiteLabelPostpaidAmountRepository;
import com.im.admin.repositories.WhiteLabelPrepaidAmountRepository;
import com.im.admin.repositories.WhiteLabelRepository;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.response.SuperDistributorResponseDTO;
import com.im.admin.response.WhiteLabelResponseDTO;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.WhiteLabelService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
import com.im.admin.util.TokenUtil;
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
    private TokenUtil tokenUtil;

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

    @Autowired
    private FileUrlUtil fileUrlUtil;

    @Autowired
    private HierarchyAccessService hierarchyAccessService;

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

            hierarchyAccessService.registerHierarchyNode(
                    HierarchyRegistrationRequest
                            .builder()
                            .hierarchyId(savedWhiteLabel.getWhiteLabelId())
                            .hierarchyType("WL")
                            .parentHierarchyId(
                                    savedWhiteLabel.getParentAccount())
                            .build()
            );

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

        String loginId = tokenUtil.extractHierarchyId();
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
//            List<FieldErrorDetailsDTO> errors = new ArrayList<>();
//            try {
//                phoneNumberValidationService.validatePhoneNumber(whiteLabels.getMobileNumber());
//            } catch (IllegalArgumentException ex) {
//                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
//            }
//            if (!errors.isEmpty()) {
//                throw new CustomValidationException(errors);
//            }

            // Update non-null fields
            if (whiteLabels.getTitle() != null) existingWhiteLabels.setTitle(whiteLabels.getTitle());
//            if (whiteLabels.getMobileNumber() != null) existingWhiteLabels.setMobileNumber(whiteLabels.getMobileNumber());
            if (whiteLabels.getAbbreviation() != null) existingWhiteLabels.setAbbreviation(whiteLabels.getAbbreviation());
            if (whiteLabels.getEmailId() != null) existingWhiteLabels.setEmailId(whiteLabels.getEmailId());
            if (whiteLabels.getAddress() != null) existingWhiteLabels.setAddress(whiteLabels.getAddress());
            if (whiteLabels.getIsAdmin() != null) existingWhiteLabels.setIsAdmin(whiteLabels.getIsAdmin());

            existingWhiteLabels.setEditedBy(loginId);
            existingWhiteLabels.setEditedOn(LocalDateTime.now());

            if (whiteLabels.getShopNumber() != null)
                existingWhiteLabels.setShopNumber(whiteLabels.getShopNumber());

            if (whiteLabels.getAadhaarNumber() != null)
                existingWhiteLabels.setAadhaarNumber(whiteLabels.getAadhaarNumber());

            if (whiteLabels.getPanNumber() != null)
                existingWhiteLabels.setPanNumber(whiteLabels.getPanNumber());

            if (whiteLabels.getBusinessNumber() != null)
                existingWhiteLabels.setBusinessNumber(whiteLabels.getBusinessNumber());

            if (whiteLabels.getBankDetails() != null &&
                    existingWhiteLabels.getBankDetails() != null) {

                BanksDetails existingBank = existingWhiteLabels.getBankDetails();
                BanksDetails requestBank = whiteLabels.getBankDetails();

                if (requestBank.getBankName() != null) {
                    existingBank.setBankName(requestBank.getBankName());
                }

                if (requestBank.getBankHolderName() != null) {
                    existingBank.setBankHolderName(requestBank.getBankHolderName());
                }

                if (requestBank.getBankAccountNumber() != null) {
                    existingBank.setBankAccountNumber(requestBank.getBankAccountNumber());
                }

                if (requestBank.getIfscCode() != null) {
                    existingBank.setIfscCode(requestBank.getIfscCode());
                }
            }

            // Handle optional image updates
            String folder = "White Label";

            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                existingWhiteLabels.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                existingWhiteLabels.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                existingWhiteLabels.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                existingWhiteLabels.setShopImage(imageUtil.saveImage(shopImage, folder));
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                existingWhiteLabels.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
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
    public WhiteLabelResponseDTO getWhiteLabelById(String id) {

        log.info("Fetching WhiteLabel by ID: {}", id);

        WhiteLabels whiteLabel = whiteLabelRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("WhiteLabel not found with ID: " + id));

        return convertToDto(whiteLabel);
    }

    private WhiteLabelResponseDTO convertToDto(WhiteLabels wl) {

        WhiteLabelResponseDTO dto = new WhiteLabelResponseDTO();

        dto.setWhiteLabelId(wl.getWhiteLabelId());
        dto.setName(wl.getName());
        dto.setMobileNumber(wl.getMobileNumber());
        dto.setEmailId(wl.getEmailId());
        dto.setTitle(wl.getTitle());
        dto.setAbbreviation(wl.getAbbreviation());
        dto.setDob(wl.getDob());

        dto.setAddress(mapAddress(wl.getAddress()));
        dto.setOutletAddress(mapAddress(wl.getOutletAddress()));

        if (wl.getBankDetails() != null) {
            BanksDetails bank = wl.getBankDetails();
            BankDetailsDTO bankDto = new BankDetailsDTO();
            bankDto.setBankName(bank.getBankName());
            bankDto.setBankAccountNumber(bank.getBankAccountNumber());
            bankDto.setIfscCode(bank.getIfscCode());
            bankDto.setBankHolderName(bank.getBankHolderName());
            bankDto.setIsVerified(bank.getIsVerified());
            dto.setBankDetails(bankDto);
        }

        dto.setAadhaarNumber(wl.getAadhaarNumber());
        dto.setPanNumber(wl.getPanNumber());
        dto.setShopNumber(wl.getShopNumber());
        dto.setBusinessNumber(wl.getBusinessNumber());

        dto.setAadhaarCardFrontImage(fileUrlUtil.getFullUrl(wl.getAadhaarCardFrontImage()));
        dto.setAadhaarCardBackImage( fileUrlUtil.getFullUrl(wl.getAadhaarCardBackImage()));
        dto.setPanCardImage( fileUrlUtil.getFullUrl(wl.getPanCardImage()));
        dto.setShopImage( fileUrlUtil.getFullUrl(wl.getShopImage()));
        dto.setBusinessProofImage( fileUrlUtil.getFullUrl(wl.getBusinessProofImage()));

        dto.setStatus(wl.getStatus());
        dto.setRemarks(wl.getRemarks());
        dto.setRejectionReason(wl.getRejectionReason());

        dto.setIsAdmin(wl.getIsAdmin());
        dto.setDomainURL(wl.getDomainURL());
        dto.setBusinessDesc(wl.getBusinessDesc());

        dto.setCreatedBy(wl.getCreatedBy());
        dto.setCreatedByIPAddress(wl.getCreatedByIPAddress());
        dto.setCreatedOn(wl.getCreatedOn());
        dto.setLastEditedOn(wl.getEditedOn());

        return dto;
    }

    private AddressDTO mapAddress(Address address) {
        if (address == null) return null;
        AddressDTO dto = new AddressDTO();
        dto.setOutletName(address.getOutletName());
        dto.setCompanyType(address.getCompanyType());
        dto.setCompanyName(address.getCompanyName());
        dto.setAddress(address.getAddress());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setZipCode(address.getZipCode());
        return dto;
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

