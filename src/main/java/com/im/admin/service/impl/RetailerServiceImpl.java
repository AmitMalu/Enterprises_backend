package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.BankDetailsDTO;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.dto.RetailerDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.*;
import com.im.admin.response.RetailerResponseDTO;
import com.im.admin.service.RetailerService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
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
    private RetailerGullakAmountRepository gullakAmountRepository;

    @Autowired
    private IdGenerator idGenerator;

    @Autowired
    private Validator validator;

    @Autowired
    private ImageHandlerUtil imageHandlerUtil;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @Autowired
    private CommonFunctions commonFunctions;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BankDetailsRepository bankDetailsRepository;

    @Transactional
    @Override
    public Retailer createRetailer(Retailer retailer,
                                   MultipartFile aadhaarCardFrontImage,
                                   MultipartFile aadhaarCardBackImage,
                                   MultipartFile panCardImage,
                                   MultipartFile shopImage,
                                   MultipartFile businessProofImage) throws IOException {

        log.info("Creating new retailer: {}", retailer.getName());

        LocalDateTime RequestDateTime = LocalDateTime.now();

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

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        String parentAccount = retailer.getParentAccount();

        if (parentAccount == null || !parentAccount.contains("-")) {
            throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
        }

        // Extract prefix from parentAccount
        String abbreviation = parentAccount.split("-")[0];

        // Optionally validate abbreviation format
        if (!abbreviation.matches("^[A-Z]{2,3}$")) {
            throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
        }

        retailer.setAbbreviation(abbreviation);

        String folder = "Retailer";
        retailer.setRetailerId(idGenerator.generateHierarchyEntityId(folder, retailer.getAbbreviation()));

        // Upload Images
        imageHandlerUtil.handleImageUpload(aadhaarCardFrontImage, "Aadhaar Card Front", folder, retailer::setAadhaarCardFrontImage);
        imageHandlerUtil.handleImageUpload(aadhaarCardBackImage, "Aadhaar Card Back", folder, retailer::setAadhaarCardBackImage);
        imageHandlerUtil.handleImageUpload(panCardImage, "PAN Card", folder, retailer::setPanCardImage);
        imageHandlerUtil.handleImageUpload(shopImage, "Shop", folder, retailer::setShopImage);
        imageHandlerUtil.handleImageUpload(businessProofImage, "Business Proof", folder, retailer::setBusinessProofImage);

        // Save Bank Details separately (Before retailer save)
        if (retailer.getBankDetails() != null) {
            BanksDetailss bankDetails = retailer.getBankDetails();
            bankDetails.setHierarchyId(retailer.getRetailerId()); // set after ID is generated
            bankDetails.setBankAccountNumber(retailer.getBankDetails().getBankAccountNumber());
            bankDetails.setCreatedBy(retailer.getCreatedBy());
            bankDetails.setIsVerified(false);

            // Save bank details
            BanksDetailss savedBankDetails = bankDetailsRepository.save(bankDetails);

            // Attach it to retailer (foreign key)
            retailer.setBankDetails(savedBankDetails);
        }

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

        RetailerGullakAmount gullakAmount = new RetailerGullakAmount();
        gullakAmount.setRetailerId(savedRetailer);
        gullakAmountRepository.save(gullakAmount);
        log.info("Gullak amount record created for Retailer ID: {}", savedRetailer.getRetailerId());

        LocalDateTime ResponseDateTime = LocalDateTime.now();
        String retailerRequest = objectMapper.writeValueAsString(retailer);
        String retailerResponse = objectMapper.writeValueAsString(savedRetailer);

        commonFunctions.saveHierarchyLogs(
                RequestDateTime,
                retailerRequest,
                null, // IP address as null
                ResponseDateTime,
                retailerResponse
        );

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

        LocalDateTime requestDateTime = LocalDateTime.now();

        Retailer existingRetailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new EntityNotFoundException("Retailer not found"));


        String retailerRequest = objectMapper.writeValueAsString(retailer);

        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
        try {
            phoneNumberValidationService.validatePhoneNumber(retailer.getMobileNumber());
        } catch (IllegalArgumentException ex) {
            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
        }

        if (!errors.isEmpty()) {
            throw new CustomValidationException(errors);
        }

        // Update non-null fields
        if (retailer.getTitle() != null) existingRetailer.setTitle(retailer.getTitle());
        if (retailer.getName() != null) existingRetailer.setName(retailer.getName());
        if (retailer.getMobileNumber() != null) existingRetailer.setMobileNumber(retailer.getMobileNumber());
        if (retailer.getEmailId() != null) existingRetailer.setEmailId(retailer.getEmailId());
        if (retailer.getDob() != null) existingRetailer.setDob(retailer.getDob());
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

        Retailer updatedRetailer = retailerRepository.save(existingRetailer);

        LocalDateTime responseDateTime = LocalDateTime.now();
        String retailerResponse = objectMapper.writeValueAsString(updatedRetailer);

        // Save hierarchy logs
        commonFunctions.saveHierarchyLogs(
                requestDateTime,
                retailerRequest,
                null, // IP Address
                responseDateTime,
                retailerResponse
        );

        return updatedRetailer;
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
    public Page<RetailerResponseDTO> getAllRetailers(Pageable pageable) {
        Page<Retailer> retailerPage = retailerRepository.findAll(pageable);
        return retailerPage.map(this::convertToDto);
    }


    @Override
    public RetailerResponseDTO getRetailerById(String retailerId) {
        log.info("Fetching retailer by ID: {}", retailerId);
        Retailer retailer = retailerRepository.findById(retailerId)
                .orElseThrow(() -> new EntityNotFoundException("Retailer not found with ID: " + retailerId));
        return convertToDto(retailer);
    }

    public RetailerResponseDTO convertToDto(Retailer retailer) {
        RetailerResponseDTO dto = new RetailerResponseDTO();
        dto.setRetailerId(retailer.getRetailerId());
        dto.setName(retailer.getName());
        dto.setMobileNumber(retailer.getMobileNumber());
        dto.setEmailId(retailer.getEmailId());
        dto.setTitle(retailer.getTitle());
        dto.setAbbreviation(retailer.getAbbreviation());
        dto.setDob(retailer.getDob());

        dto.setAddress(mapAddress(retailer.getAddress()));
        dto.setOutletAddress(mapAddress(retailer.getOutletAddress()));

        if (retailer.getBankDetails() != null) {
            BanksDetailss bank = retailer.getBankDetails();
            BankDetailsDTO bankDto = new BankDetailsDTO();
            bankDto.setBankName(bank.getBankName());
            bankDto.setBankAccountNumber(bank.getBankAccountNumber());
            bankDto.setIfscCode(bank.getIfscCode());
            bankDto.setBankHolderName(bank.getBankHolderName());
            bankDto.setIsVerified(bank.getIsVerified());
            dto.setBankDetails(bankDto);
        }

        dto.setAadhaarNumber(retailer.getAadhaarNumber());
        dto.setPanNumber(retailer.getPanNumber());
        dto.setShopNumber(retailer.getShopNumber());
        dto.setBusinessNumber(retailer.getBusinessNumber());
        dto.setAadhaarCardFrontImage(retailer.getAadhaarCardFrontImage());
        dto.setAadhaarCardBackImage(retailer.getAadhaarCardBackImage());
        dto.setPanCardImage(retailer.getPanCardImage());
        dto.setShopImage(retailer.getShopImage());
        dto.setBusinessProofImage(retailer.getBusinessProofImage());

        dto.setStatus(retailer.getStatus());
        dto.setRemarks(retailer.getRemarks());
        dto.setRejectionReason(retailer.getRejectionReason());

        dto.setCreatedBy(retailer.getCreatedBy());
        dto.setCreatedByIPAddress(retailer.getCreatedByIPAddress());
        dto.setCreatedOn(retailer.getCreatedOn());
        dto.setLastEditedOn(retailer.getLastEditedOn());

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


}

