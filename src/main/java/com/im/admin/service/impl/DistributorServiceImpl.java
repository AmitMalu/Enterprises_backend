package com.im.admin.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.commons.CommonFunctions;
import com.im.admin.dto.FieldErrorDetailsDTO;
import com.im.admin.entities.*;
import com.im.admin.exception.CustomValidationException;
import com.im.admin.repositories.*;
import com.im.admin.response.DistributorResponseDTO;
import com.im.admin.service.DistributorService;
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
@RequiredArgsConstructor
@Slf4j
public class DistributorServiceImpl implements DistributorService {

    @Autowired
    private DistributorRepository distributorRepository;

    @Autowired
    private DistributorPrepaidAmountRepository prepaidAmountRepository;

    @Autowired
    private DistributorPostpaidAmountRepository postpaidAmountRepository;

    @Autowired
    private DistributorGullakAmountRepository gullakAmountRepository;

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
    public Distributor createDistributor(Distributor distributor,
                                         MultipartFile aadhaarCardFrontImage,
                                         MultipartFile aadhaarCardBackImage,
                                         MultipartFile panCardImage,
                                         MultipartFile shopImage,
                                         MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String distributorRequest = null;
        String distributorResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            log.info("Starting Distributor creation process for parentAccount: {}", distributor.getParentAccount());

            // Validate distributor fields
            Set<ConstraintViolation<Distributor>> violations = validator.validate(distributor);
            List<FieldErrorDetailsDTO> errors = violations.stream()
                    .map(violation -> new FieldErrorDetailsDTO(violation.getPropertyPath().toString(), violation.getMessage()))
                    .collect(Collectors.toList());

            // Phone validation
            try {
                phoneNumberValidationService.validatePhoneNumber(distributor.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                log.error("Phone number validation failed: {}", ex.getMessage());
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
                    log.warn("Missing required image: {}", entry.getKey());
                    errors.add(new FieldErrorDetailsDTO(entry.getKey(), "image is required"));
                }
            }

            if (!errors.isEmpty()) {
                log.error("Validation failed for Distributor: {}", errors);
                throw new CustomValidationException(errors);
            }

            // Validate parent account
            String parentAccount = distributor.getParentAccount();
            if (parentAccount == null || !parentAccount.contains("-")) {
                log.error("Invalid parentAccount format: {}", parentAccount);
                throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
            }

            String abbreviation = parentAccount.split("-")[0];
            if (!abbreviation.matches("^[A-Z]{2,3}$")) {
                log.error("Derived abbreviation '{}' is invalid.", abbreviation);
                throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
            }

            distributor.setAbbreviation(abbreviation);
            String folder = "Distributor";
            distributor.setDistributorId(idGenerator.generateHierarchyEntityId(folder, distributor.getAbbreviation()));

            // Serialize request before DB save
            distributorRequest = objectMapper.writeValueAsString(distributor);

            // 🔹 Upload images (clean & correct mapping)
            log.info("Uploading distributor images...");
            distributor.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, "Distributor"));
            distributor.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, "Distributor"));
            distributor.setPanCardImage(imageUtil.saveImage(panCardImage, "Distributor"));
            distributor.setShopImage(imageUtil.saveImage(shopImage, "Distributor"));
            distributor.setBusinessProofImage(imageUtil.saveImage(businessProofImage, "Distributor"));

            // Save Bank Details separately (Before retailer save)
            if (distributor.getBankDetails() != null) {
                BanksDetails bankDetails = distributor.getBankDetails();
                bankDetails.setHierarchyId(distributor.getDistributorId()); // set after ID is generated
                bankDetails.setBankName(distributor.getBankDetails().getBankName());
                bankDetails.setBankHolderName(distributor.getBankDetails().getBankHolderName());
                bankDetails.setBankAccountNumber(distributor.getBankDetails().getBankAccountNumber());
                bankDetails.setIsVerified(distributor.getBankDetails().getIsVerified());
                bankDetails.setIfscCode(distributor.getBankDetails().getIfscCode());
                bankDetails.setCreatedBy(distributor.getCreatedBy());

                // Save bank details
                BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

                // Attach it to Distributor (foreign key)
                distributor.setBankDetails(savedBankDetails);
            }

            // Save distributor
            log.info("Saving Distributor entity...");
            Distributor savedDistributor = distributorRepository.save(distributor);

            // Initialize wallet balances
            DistributorPrepaidAmount prepaid = new DistributorPrepaidAmount();
            prepaid.setDistributorId(savedDistributor);
            prepaidAmountRepository.save(prepaid);

            DistributorPostpaidAmount postpaid = new DistributorPostpaidAmount();
            postpaid.setDistributorId(savedDistributor);
            postpaidAmountRepository.save(postpaid);

            DistributorGullakAmount gullak = new DistributorGullakAmount();
            gullak.setDistributorId(savedDistributor);
            gullakAmountRepository.save(gullak);

            log.info("Distributor created successfully with ID: {}", savedDistributor.getDistributorId());

            responseDateTime = LocalDateTime.now();
            distributorResponse = objectMapper.writeValueAsString(savedDistributor);

            return savedDistributor;

        } catch (Exception ex) {
            log.error("Error occurred while creating Distributor: {}", ex.getMessage(), ex);
            distributorResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;
        } finally {
            log.info("Saving hierarchy logs for Distributor creation...");
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    distributorRequest,
                    null, // IP address
                    responseDateTime,
                    distributorResponse
            );
        }
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

        LocalDateTime requestDateTime = LocalDateTime.now();
        String distributorRequest = null;
        String distributorResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            log.info("Starting update process for Distributor ID: {}", id);

            Distributor existingDistributor = distributorRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Distributor not found with ID: {}", id);
                        return new EntityNotFoundException("Distributor not found");
                    });

            // Capture request payload
            distributorRequest = objectMapper.writeValueAsString(distributor);

            // Check if update is necessary
            if (existingDistributor.equals(distributor)) {
                log.info("No changes detected for Distributor ID: {}", id);
                distributorResponse = objectMapper.writeValueAsString(existingDistributor);
                return existingDistributor;
            }

            // Validate mobile
            List<FieldErrorDetailsDTO> errors = new ArrayList<>();
            try {
                phoneNumberValidationService.validatePhoneNumber(distributor.getMobileNumber());
            } catch (IllegalArgumentException ex) {
                log.error("Invalid mobile number for Distributor ID {}: {}", id, ex.getMessage());
                errors.add(new FieldErrorDetailsDTO("mobile", ex.getMessage()));
            }

            if (!errors.isEmpty()) {
                log.error("Validation failed for Distributor update. Errors: {}", errors);
                throw new CustomValidationException(errors);
            }

            // Update fields if provided
            if (distributor.getTitle() != null) existingDistributor.setTitle(distributor.getTitle());
            if (distributor.getName() != null) existingDistributor.setName(distributor.getName());
            if (distributor.getMobileNumber() != null)
                existingDistributor.setMobileNumber(distributor.getMobileNumber());
            if (distributor.getEmailId() != null) existingDistributor.setEmailId(distributor.getEmailId());
            if (distributor.getDob() != null) existingDistributor.setDob(distributor.getDob());
            if (distributor.getAddress() != null) existingDistributor.setAddress(distributor.getAddress());
            if (distributor.getOutletAddress() != null)
                existingDistributor.setOutletAddress(distributor.getOutletAddress());

            // Optional image updates
            String folder = "Distributor";
            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                distributor.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                distributor.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                distributor.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                distributor.setShopImage(imageUtil.saveImage(shopImage, folder));
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                distributor.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
            }

            // Save updated distributor
            Distributor updatedDistributor = distributorRepository.save(existingDistributor);
            log.info("Distributor updated successfully with ID: {}", updatedDistributor.getDistributorId());

            responseDateTime = LocalDateTime.now();
            distributorResponse = objectMapper.writeValueAsString(updatedDistributor);

            return updatedDistributor;

        } catch (Exception ex) {
            log.error("Error while updating Distributor ID {}: {}", id, ex.getMessage(), ex);
            distributorResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            throw ex;
        } finally {
            log.info("Saving hierarchy logs for Distributor update...");
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    distributorRequest,
                    null, // IP address
                    responseDateTime,
                    distributorResponse
            );
        }
    }


    //Get All Distributors
    @Override
    public List<Distributor> getAllDistributors() {
        LocalDateTime requestTime = LocalDateTime.now();
        String requestPayload = "Fetching all distributors";

        log.info("Request received at {}: {}", requestTime, requestPayload);

        try {
            List<Distributor> distributors = distributorRepository.findAll();
            LocalDateTime responseTime = LocalDateTime.now();

            log.info("Fetched {} distributors successfully at {}", distributors.size(), responseTime);

            // Save logs in DB
            commonFunctions.saveImLogs(
                    requestPayload,
                    requestTime,
                    null,
                    "Fetched " + distributors.size() + " distributors successfully",
                    responseTime
            );

            return distributors;
        } catch (Exception e) {
            LocalDateTime responseTime = LocalDateTime.now();

            log.error("Error fetching distributors at {}: {}", responseTime, e.getMessage(), e);

            commonFunctions.saveImLogs(
                    requestPayload,
                    requestTime,
                    null,
                    "Error fetching distributors: " + e.getMessage(),
                    responseTime
            );

            throw e;
        }
    }

    @Override
    public DistributorResponseDTO getDistributorById(String id) {

        LocalDateTime requestTime = LocalDateTime.now();
        String requestPayload = "Fetching distributor with ID: " + id;

        log.info("Request received at {}: {}", requestTime, requestPayload);

        try {

            Distributor distributor = distributorRepository.findById(id)
                    .orElseThrow(() ->
                            new EntityNotFoundException("Distributor not found with ID: " + id)
                    );

            DistributorResponseDTO dto = convertToDto(distributor);

            LocalDateTime responseTime = LocalDateTime.now();

            log.info("Distributor found with ID {} at {}", distributor.getDistributorId(), responseTime);

            commonFunctions.saveImLogs(
                    requestPayload,
                    requestTime,
                    null,
                    "Distributor found: " + distributor.getDistributorId(),
                    responseTime
            );

            return dto;

        } catch (EntityNotFoundException e) {

            LocalDateTime responseTime = LocalDateTime.now();

            log.warn("Distributor not found with ID {} at {}", id, responseTime);

            commonFunctions.saveImLogs(
                    requestPayload,
                    requestTime,
                    null,
                    "Error: " + e.getMessage(),
                    responseTime
            );

            throw e;
        }
    }

    private DistributorResponseDTO convertToDto(Distributor distributor) {

        DistributorResponseDTO dto = new DistributorResponseDTO();

        dto.setDistributorId(distributor.getDistributorId());
        dto.setParentAccount(distributor.getParentAccount());
        dto.setTitle(distributor.getTitle());
        dto.setName(distributor.getName());
        dto.setMobileNumber(distributor.getMobileNumber());
        dto.setEmailId(distributor.getEmailId());
        dto.setAbbreviation(distributor.getAbbreviation());
        dto.setDob(distributor.getDob());
        dto.setStatus(distributor.getStatus());
        dto.setRemarks(distributor.getRemarks());
        dto.setAadhaarNumber(distributor.getAadhaarNumber());
        dto.setPanNumber(distributor.getPanNumber());
        dto.setPanVerified(distributor.getPanVerified());
        dto.setAadhaarPanVerified(distributor.getAadhaarPanVerified());
        dto.setShopNumber(distributor.getShopNumber());
        dto.setBusinessNumber(distributor.getBusinessNumber());
        dto.setCreatedBy(distributor.getCreatedBy());
        dto.setCreatedOn(distributor.getCreatedOn());

        return dto;
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
