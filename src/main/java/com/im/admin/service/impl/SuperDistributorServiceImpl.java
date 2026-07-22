package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.AddressDTO;
import com.im.admin.dto.BankDetailsDTO;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.properties.FileUrlUtil;
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.repositories.SuperDistributorPostpaidAmountRepository;
import com.im.admin.repositories.SuperDistributorPrepaidAmountRepository;
import com.im.admin.repositories.SuperDistributorRepository;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.response.PartnerResponseDTO;
import com.im.admin.response.SuperDistributorResponseDTO;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.SuperDistributorService;
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
@RequiredArgsConstructor
@Slf4j
public class SuperDistributorServiceImpl implements SuperDistributorService {

    @Autowired
    private SuperDistributorRepository superDistributorRepository;

    @Autowired
    private TokenUtil tokenUtil;

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
    public SuperDistributor saveSuperDistributor(SuperDistributor superDistributor,
                                                 MultipartFile aadhaarCardFrontImage,
                                                 MultipartFile aadhaarCardBackImage,
                                                 MultipartFile panCardImage,
                                                 MultipartFile shopImage,
                                                 MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestPayload = objectMapper.writeValueAsString(superDistributor);

        log.info("Starting saveSuperDistributor for mobile: {}", superDistributor.getMobileNumber());

        try {
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

            if (!errors.isEmpty()) {
                log.warn("Validation failed for SuperDistributor: {}", errors);
                throw new CustomValidationException(errors);
            }

            String parentAccount = superDistributor.getParentAccount();

            if (parentAccount == null || !parentAccount.contains("-")) {
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            // Extract abbreviation
            String abbreviation = parentAccount.split("-")[0];
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }
            superDistributor.setAbbreviation(abbreviation);

            // Generate ID
            String folder = "Super Distributor";
            superDistributor.setSuperDistributorId(
                    idGenerator.generateHierarchyEntityId(folder, superDistributor.getAbbreviation())
            );

            // 🔹 Upload images (clean & correct mapping)
            log.info("Uploading distributor images...");
            superDistributor.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            superDistributor.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            superDistributor.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            superDistributor.setShopImage(imageUtil.saveImage(shopImage, folder));
            superDistributor.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));

            // Save Bank Details separately (Before superDistributor save)
            if (superDistributor.getBankDetails() != null) {
                BanksDetails bankDetails = superDistributor.getBankDetails();
                bankDetails.setHierarchyId(superDistributor.getSuperDistributorId()); // set after ID is generated
                bankDetails.setBankName(superDistributor.getBankDetails().getBankName());
                bankDetails.setBankHolderName(superDistributor.getBankDetails().getBankHolderName());
                bankDetails.setBankAccountNumber(superDistributor.getBankDetails().getBankAccountNumber());
                bankDetails.setIsVerified(superDistributor.getBankDetails().getIsVerified());
                bankDetails.setIfscCode(superDistributor.getBankDetails().getIfscCode());
                bankDetails.setCreatedBy(superDistributor.getCreatedBy());

                // Save bank details
                BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

                // Attach it to superDistributor (foreign key)
                superDistributor.setBankDetails(savedBankDetails);
            }

            // Save SuperDistributor
            SuperDistributor savedDistributor = superDistributorRepository.save(superDistributor);
            log.info("SuperDistributor saved successfully with ID: {}", savedDistributor.getSuperDistributorId());

            hierarchyAccessService.registerHierarchyNode(
                    HierarchyRegistrationRequest
                            .builder()
                            .hierarchyId(savedDistributor.getSuperDistributorId())
                            .hierarchyType("SD")
                            .parentHierarchyId(
                                    savedDistributor.getParentAccount())
                            .build()
            );

            // Create prepaid/postpaid records
            SuperDistributorPrepaidAmount prepaidAmount = new SuperDistributorPrepaidAmount();
            prepaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPrepaidAmountRepository.save(prepaidAmount);
            log.info("Prepaid amount record created for Super Distributor: {}", savedDistributor.getSuperDistributorId());

            SuperDistributorPostpaidAmount postpaidAmount = new SuperDistributorPostpaidAmount();
            postpaidAmount.setSuperDistributorId(savedDistributor);
            superDistributorPostpaidAmountRepository.save(postpaidAmount);
            log.info("Postpaid amount record created for Super Distributor: {}", savedDistributor.getSuperDistributorId());

            // Save hierarchy logs
            LocalDateTime responseDateTime = LocalDateTime.now();
            String responsePayload = objectMapper.writeValueAsString(savedDistributor);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    responsePayload
            );

            log.info("Hierarchy logs saved for SuperDistributor: {}", savedDistributor.getSuperDistributorId());
            return savedDistributor;

        } catch (Exception e) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            log.error("Error occurred while saving SuperDistributor: {}", e.getMessage(), e);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    "Error: " + e.getMessage()
            );

            throw e;
        }
    }

    @Override
    public SuperDistributorResponseDTO getSuperDistributor(String id) {

        LocalDateTime requestTime = LocalDateTime.now();
        String request = "Fetching SuperDistributor with id: " + id;

        log.info("Request to get SuperDistributor with id: {}", id);

        try {

            SuperDistributor superDistributor = superDistributorRepository.findById(id)
                    .orElseThrow(() ->
                            new RuntimeException("SuperDistributor not found with id: " + id)
                    );

            SuperDistributorResponseDTO dto = convertToDto(superDistributor);

            LocalDateTime responseTime = LocalDateTime.now();

            commonFunctions.saveHierarchyLogs(
                    requestTime,
                    request,
                    null,
                    responseTime,
                    dto.toString()
            );

            log.info("Successfully fetched SuperDistributor with id: {}", id);

            return dto;

        } catch (Exception ex) {

            LocalDateTime responseTime = LocalDateTime.now();

            commonFunctions.saveHierarchyLogs(
                    requestTime,
                    request,
                    null,
                    responseTime,
                    "Error: " + ex.getMessage()
            );

            log.error("Failed to fetch SuperDistributor with id: {}", id, ex);

            throw ex;
        }
    }

    private SuperDistributorResponseDTO convertToDto(SuperDistributor sd) {

        SuperDistributorResponseDTO dto = new SuperDistributorResponseDTO();

        dto.setSuperDistributorId(sd.getSuperDistributorId());
        dto.setName(sd.getName());
        dto.setMobileNumber(sd.getMobileNumber());
        dto.setEmailId(sd.getEmailId());
        dto.setTitle(sd.getTitle());
        dto.setAbbreviation(sd.getAbbreviation());
        dto.setDob(sd.getDob());

        dto.setAddress(mapAddress(sd.getAddress()));
        dto.setOutletAddress(mapAddress(sd.getOutletAddress()));

        if (sd.getBankDetails() != null) {
            BanksDetails bank = sd.getBankDetails();
            BankDetailsDTO bankDto = new BankDetailsDTO();
            bankDto.setBankName(bank.getBankName());
            bankDto.setBankAccountNumber(bank.getBankAccountNumber());
            bankDto.setIfscCode(bank.getIfscCode());
            bankDto.setBankHolderName(bank.getBankHolderName());
            bankDto.setIsVerified(bank.getIsVerified());
            dto.setBankDetails(bankDto);
        }


        dto.setAadhaarNumber(sd.getAadhaarNumber());
        dto.setPanNumber(sd.getPanNumber());
        dto.setShopNumber(sd.getShopNumber());
        dto.setBusinessNumber(sd.getBusinessNumber());

        dto.setAadhaarCardFrontImage(fileUrlUtil.getFullUrl(sd.getAadhaarCardFrontImage()));
        dto.setAadhaarCardBackImage( fileUrlUtil.getFullUrl(sd.getAadhaarCardBackImage()));
        dto.setPanCardImage( fileUrlUtil.getFullUrl(sd.getPanCardImage()));
        dto.setShopImage( fileUrlUtil.getFullUrl(sd.getShopImage()));
        dto.setBusinessProofImage( fileUrlUtil.getFullUrl(sd.getBusinessProofImage()));

        dto.setStatus(sd.getStatus());
        dto.setRemarks(sd.getRemarks());
        dto.setRejectionReason(sd.getRejectionReason());

        dto.setCreatedBy(sd.getCreatedBy());
        dto.setCreatedByIPAddress(sd.getCreatedByIPAddress());
        dto.setCreatedOn(sd.getCreatedOn());
        dto.setLastEditedOn(sd.getLastEditedOn());

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
    public SuperDistributor updateSuperDistributor(String id,
                                                   SuperDistributor superDistributor,
                                                   MultipartFile aadhaarCardFrontImage,
                                                   MultipartFile aadhaarCardBackImage,
                                                   MultipartFile panCardImage,
                                                   MultipartFile shopImage,
                                                   MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String requestPayload = objectMapper.writeValueAsString(superDistributor);

        String loginId = tokenUtil.extractHierarchyId();


        log.info("Starting updateSuperDistributor for ID: {}", id);

        try {
            SuperDistributor existingSuperDistributor = superDistributorRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Super Distributor not found with ID: " + id));

            if (existingSuperDistributor.equals(superDistributor)) {
                log.info("No changes detected for SuperDistributor with ID: {}", id);
                return existingSuperDistributor;
            }

//            // Mobile validation
//            List<FieldErrorDetailsDTO> errors = new ArrayList<>();
//            try {
//                phoneNumberValidationService.validatePhoneNumber(superDistributor.getMobileNumber());
//            } catch (IllegalArgumentException ex) {
//                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
//            }
//
//            if (!errors.isEmpty()) {
//                log.warn("Validation failed while updating SuperDistributor with ID: {}, Errors: {}", id, errors);
//                throw new CustomValidationException(errors);
//            }

            // Update fields selectively
            if (superDistributor.getTitle() != null)
                existingSuperDistributor.setTitle(superDistributor.getTitle());

            if (superDistributor.getShopNumber() != null)
                existingSuperDistributor.setShopNumber(superDistributor.getShopNumber());

            if (superDistributor.getAadhaarNumber() != null)
                existingSuperDistributor.setAadhaarNumber(superDistributor.getAadhaarNumber());

            if (superDistributor.getBusinessNumber() != null)
                existingSuperDistributor.setBusinessNumber(superDistributor.getBusinessNumber());

            if (superDistributor.getPanNumber() != null)
                existingSuperDistributor.setPanNumber(superDistributor.getPanNumber());

            if (superDistributor.getBankDetails() != null)
                existingSuperDistributor.setPanNumber(superDistributor.getPanNumber());

            if (superDistributor.getName() != null)
                existingSuperDistributor.setName(superDistributor.getName());
//            if (superDistributor.getMobileNumber() != null)
//                existingSuperDistributor.setMobileNumber(superDistributor.getMobileNumber());
            if (superDistributor.getEmailId() != null)
                existingSuperDistributor.setEmailId(superDistributor.getEmailId());
            if (superDistributor.getDob() != null)
                existingSuperDistributor.setDob(superDistributor.getDob());
            if (superDistributor.getAddress() != null)
                existingSuperDistributor.setAddress(superDistributor.getAddress());
            if (superDistributor.getOutletAddress() != null)
                existingSuperDistributor.setOutletAddress(superDistributor.getOutletAddress());

            if (superDistributor.getBankDetails() != null &&
                    existingSuperDistributor.getBankDetails() != null) {

                BanksDetails existingBank = existingSuperDistributor.getBankDetails();
                BanksDetails requestBank = superDistributor.getBankDetails();

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

            existingSuperDistributor.setEditedBy(loginId);
            existingSuperDistributor.setLastEditedOn(LocalDateTime.now());

            // Image updates
            String folder = "Super Distributor";

            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                existingSuperDistributor.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                existingSuperDistributor.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                existingSuperDistributor.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                existingSuperDistributor.setShopImage(imageUtil.saveImage(shopImage, folder));
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                existingSuperDistributor.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
            }


            // Save updated distributor
            SuperDistributor updatedDistributor = superDistributorRepository.save(existingSuperDistributor);
            log.info("SuperDistributor updated successfully with ID: {}", updatedDistributor.getSuperDistributorId());

            // Save hierarchy logs
            LocalDateTime responseDateTime = LocalDateTime.now();
            String responsePayload = objectMapper.writeValueAsString(updatedDistributor);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    responsePayload
            );
            log.info("Hierarchy logs saved for updateSuperDistributor with ID: {}", updatedDistributor.getSuperDistributorId());

            return updatedDistributor;

        } catch (Exception e) {
            LocalDateTime responseDateTime = LocalDateTime.now();
            log.error("Error updating SuperDistributor with ID: {}. Message: {}", id, e.getMessage(), e);

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    requestPayload,
                    null,
                    responseDateTime,
                    "Error: " + e.getMessage()
            );

            throw e;
        }
    }


    @Override
    public List<SuperDistributor> getAllSuperDistributors() {
        LocalDateTime requestTime = LocalDateTime.now();
        String request = "Fetch all SuperDistributors";

        try {
            log.info("Fetching all SuperDistributors at {}", requestTime);

            List<SuperDistributor> superDistributors = superDistributorRepository.findAll();

            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Fetched " + superDistributors.size() + " SuperDistributors successfully";

            // Save logs
            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            return superDistributors;
        } catch (Exception e) {
            LocalDateTime responseTime = LocalDateTime.now();
            String response = "Error fetching SuperDistributors: " + e.getMessage();

            // Save error logs
            commonFunctions.saveHierarchyLogs(requestTime, request, null, responseTime, response);

            log.error("Error fetching SuperDistributors", e);
            throw e;
        }
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

