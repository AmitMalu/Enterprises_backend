package com.im.admin.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.BankDetailsDTO;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.properties.FileUrlUtil;
import com.im.admin.repositories.*;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.response.RetailerResponseDTO;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.RetailerService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
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

    @Autowired
    private ImageUtil imageUtil;

    @Autowired
    private FileUrlUtil fileUrlUtil;

    @Autowired
    private HierarchyAccessService hierarchyAccessService;

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
        retailer.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, "Retailer"));

        retailer.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, "Retailer"));

        retailer.setPanCardImage(imageUtil.saveImage(panCardImage, "Retailer"));

        retailer.setShopImage(imageUtil.saveImage(shopImage, "Retailer"));

        retailer.setBusinessProofImage(imageUtil.saveImage(businessProofImage, "Retailer"));

        // Save Bank Details separately (Before retailer save)
        if (retailer.getBankDetails() != null) {
            BanksDetails bankDetails = retailer.getBankDetails();
            bankDetails.setHierarchyId(retailer.getRetailerId()); // set after ID is generated
            bankDetails.setBankName(retailer.getBankDetails().getBankName());
            bankDetails.setBankHolderName(retailer.getBankDetails().getBankHolderName());
            bankDetails.setBankAccountNumber(retailer.getBankDetails().getBankAccountNumber());
            bankDetails.setIsVerified(retailer.getBankDetails().getIsVerified());
            bankDetails.setIfscCode(retailer.getBankDetails().getIfscCode());
            bankDetails.setCreatedBy(retailer.getCreatedBy());

            // Save bank details
            BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

            // Attach it to retailer (foreign key)
            retailer.setBankDetails(savedBankDetails);
        }

        Retailer savedRetailer = retailerRepository.save(retailer);
        log.info("Retailer created successfully with ID: {}", savedRetailer.getRetailerId());

        hierarchyAccessService.registerHierarchyNode(
                HierarchyRegistrationRequest
                        .builder()
                        .hierarchyId(savedRetailer.getRetailerId())
                        .hierarchyType("RT")
                        .parentHierarchyId(
                                savedRetailer.getParentAccount())
                        .build()
        );

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

//        List<FieldErrorDetailsDTO> errors = new ArrayList<>();
//        try {
//            phoneNumberValidationService.validatePhoneNumber(retailer.getMobileNumber());
//        } catch (IllegalArgumentException ex) {
//            errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
//        }
//
//        if (!errors.isEmpty()) {
//            throw new CustomValidationException(errors);
//        }

        List<FieldErrorDetailsDTO> errors = new ArrayList<>();

        if (retailer.getMobileNumber() != null
                && !retailer.getMobileNumber().equals(existingRetailer.getMobileNumber())) {

            try {
                phoneNumberValidationService.validatePhoneNumber(retailer.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
            }
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

        if (retailer.getShopNumber() != null)
            existingRetailer.setShopNumber(existingRetailer.getShopNumber());

        if (retailer.getAadhaarNumber() != null)
            existingRetailer.setAadhaarNumber(existingRetailer.getAadhaarNumber());

        if (retailer.getPanNumber() != null)
            existingRetailer.setPanNumber(existingRetailer.getPanNumber());

        if (retailer.getBusinessNumber() != null)
            existingRetailer.setBusinessNumber(existingRetailer.getBusinessNumber());

        if (retailer.getBankDetails() != null &&
                existingRetailer.getBankDetails() != null) {

            BanksDetails existingBank = existingRetailer.getBankDetails();
            BanksDetails requestBank = retailer.getBankDetails();

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


        // Optional image updates
        String folder = "Retailer";

        if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
            existingRetailer.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
        }
        if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
            existingRetailer.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
        }
        if (panCardImage != null && !panCardImage.isEmpty()) {
            existingRetailer.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
        }
        if (shopImage != null && !shopImage.isEmpty()) {
            existingRetailer.setShopImage(imageUtil.saveImage(shopImage, folder));
        }
        if (businessProofImage != null && !businessProofImage.isEmpty()) {
            existingRetailer.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
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

    @Override
    public Page<RetailerResponseDTO> getAllRetailers(Pageable pageable) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String retailerRequest = "Fetching all retailers with pageable: " + pageable.toString();

        Page<RetailerResponseDTO> dtoPage = null;
        String retailerResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            Page<Retailer> retailerPage = retailerRepository.findAll(pageable);
            dtoPage = retailerPage.map(this::convertToDto);

            retailerResponse = objectMapper.writeValueAsString(dtoPage.getContent());
            responseDateTime = LocalDateTime.now();

            return dtoPage;
        } catch (Exception ex) {
            retailerResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex; // let API return error as usual
        } finally {
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    retailerRequest,
                    null, // ipAddress if available
                    responseDateTime,
                    retailerResponse
            );
        }
    }


    @Override
    public RetailerResponseDTO getRetailerById(String retailerId) throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String retailerRequest = "Fetching retailer with ID: " + retailerId;

        RetailerResponseDTO dto = null;
        String retailerResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            Retailer retailer = retailerRepository.findById(retailerId)
                    .orElseThrow(() -> new EntityNotFoundException("Retailer not found with ID: " + retailerId));

            dto = convertToDto(retailer);
            retailerResponse = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();

            return dto;
        } catch (Exception ex) {
            retailerResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;
        } finally {
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    retailerRequest,
                    null, // ipAddress if available
                    responseDateTime,
                    retailerResponse
            );
        }
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
            BanksDetails bank = retailer.getBankDetails();
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

        dto.setAadhaarCardFrontImage(fileUrlUtil.getFullUrl(retailer.getAadhaarCardFrontImage()));
        dto.setAadhaarCardBackImage( fileUrlUtil.getFullUrl(retailer.getAadhaarCardBackImage()));
        dto.setPanCardImage( fileUrlUtil.getFullUrl(retailer.getPanCardImage()));
        dto.setShopImage( fileUrlUtil.getFullUrl(retailer.getShopImage()));
        dto.setBusinessProofImage( fileUrlUtil.getFullUrl(retailer.getBusinessProofImage()));

        dto.setStatus(retailer.getStatus());
        dto.setRemarks(retailer.getRemarks());
        dto.setRejectionReason(retailer.getRejectionReason());

        dto.setCreatedBy(retailer.getCreatedBy());
        dto.setCreatedByIPAddress(retailer.getCreatedByIPAddress());
        dto.setCreatedOn(retailer.getCreatedOn());
        dto.setLastEditedOn(retailer.getLastEditedOn());
        dto.setLatitude(retailer.getLatitude());
        dto.setLongitude(retailer.getLongitude());

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

