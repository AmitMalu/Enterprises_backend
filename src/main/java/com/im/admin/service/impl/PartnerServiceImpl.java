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
import com.im.admin.repositories.BankDetailsRepository;
import com.im.admin.repositories.PartnerPostpaidAmountRepository;
import com.im.admin.repositories.PartnerPrepaidAmountRepository;
import com.im.admin.repositories.PartnerRepository;
import com.im.admin.request.HierarchyRegistrationRequest;
import com.im.admin.response.DistributorResponseDTO;
import com.im.admin.response.PartnerResponseDTO;
import com.im.admin.service.HierarchyAccessService;
import com.im.admin.service.PartnerService;
import com.im.admin.util.IdGenerator;
import com.im.admin.util.ImageHandlerUtil;
import com.im.admin.util.ImageUtil;
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
    public Partner savePartner(Partner partner,
                               MultipartFile aadhaarCardFrontImage,
                               MultipartFile aadhaarCardBackImage,
                               MultipartFile panCardImage,
                               MultipartFile shopImage,
                               MultipartFile businessProofImage) throws IOException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        log.info("Starting Partner save operation for parentAccount: {}", partner.getParentAccount());

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
            log.warn("Phone validation failed for Partner mobile: {}", partner.getMobileNumber());
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
                log.warn("Missing required image: {}", entry.getKey());
            }
        }

        if (!errors.isEmpty()) {
            log.error("Partner validation failed: {}", errors);
            throw new CustomValidationException(errors);
        }

        // Abbreviation extraction
        String parentAccount = partner.getParentAccount();
        if (parentAccount == null || !parentAccount.contains("-")) {
            throw new IllegalArgumentException("Invalid parentAccount format. Expected format: 'ABC-XX-001'");
        }

        String abbreviation = parentAccount.split("-")[0];
        if (!abbreviation.matches("^[A-Z]{2,3}$")) {
            throw new IllegalArgumentException("Derived abbreviation '" + abbreviation + "' is invalid. Must be 2 to 3 uppercase letters.");
        }

        partner.setAbbreviation(abbreviation);

        // Generate ID
        String folder = "Partner";
        partner.setPartnerId(idGenerator.generateHierarchyEntityId(folder, partner.getAbbreviation()));

        // Upload images (clean & correct mapping)
        log.info("Uploading distributor images...");
        partner.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
        partner.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
        partner.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
        partner.setShopImage(imageUtil.saveImage(shopImage, folder));
        partner.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));

        // Save Bank Details separately (Before partner save)
        if (partner.getBankDetails() != null) {
            BanksDetails bankDetails = partner.getBankDetails();
            bankDetails.setHierarchyId(partner.getPartnerId()); // set after ID is generated
            bankDetails.setBankName(partner.getBankDetails().getBankName());
            bankDetails.setBankHolderName(partner.getBankDetails().getBankHolderName());
            bankDetails.setBankAccountNumber(partner.getBankDetails().getBankAccountNumber());
            bankDetails.setIsVerified(partner.getBankDetails().getIsVerified());
            bankDetails.setIfscCode(partner.getBankDetails().getIfscCode());
            bankDetails.setCreatedBy(partner.getCreatedBy());

            // Save bank details
            BanksDetails savedBankDetails = bankDetailsRepository.save(bankDetails);

            // Attach it to partner (foreign key)
            partner.setBankDetails(savedBankDetails);
        }

        Partner savedPartner = partnerRepository.save(partner);

        hierarchyAccessService.registerHierarchyNode(
                HierarchyRegistrationRequest
                        .builder()
                        .hierarchyId(savedPartner.getPartnerId())
                        .hierarchyType("PT")
                        .parentHierarchyId(
                                savedPartner.getParentAccount())
                        .build()
        );

        // Save prepaid & postpaid accounts
        PartnerPrepaidAmount prepaidAmount = new PartnerPrepaidAmount();
        prepaidAmount.setPartnerId(savedPartner);
        partnerPrepaidAmountRepository.save(prepaidAmount);

        PartnerPostpaidAmount postpaidAmount = new PartnerPostpaidAmount();
        postpaidAmount.setPartnerId(savedPartner);
        partnerPostpaidAmountRepository.save(postpaidAmount);

        // Save logs
        LocalDateTime responseDateTime = LocalDateTime.now();
        String partnerRequest = objectMapper.writeValueAsString(partner);
        String partnerResponse = objectMapper.writeValueAsString(savedPartner);

        commonFunctions.saveHierarchyLogs(
                requestDateTime,
                partnerRequest,
                null, // IP address
                responseDateTime,
                partnerResponse
        );

        log.info("Partner saved successfully with ID: {}", savedPartner.getPartnerId());
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

        LocalDateTime requestDateTime = LocalDateTime.now();
        String partnerRequest = objectMapper.writeValueAsString(partner);

        Partner updatedPartner = null;
        String partnerResponse = null;
        LocalDateTime responseDateTime = null;

        try {
            Partner existingPartner = partnerRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Partner not found with ID: " + id));

            if (existingPartner.equals(partner)) {
                log.info("No changes detected for Partner ID: {}", id);
                return existingPartner;
            }

            // Update only provided fields
            if (partner.getTitle() != null) existingPartner.setTitle(partner.getTitle());
            if (partner.getName() != null) existingPartner.setName(partner.getName());
            if (partner.getEmailId() != null) existingPartner.setEmailId(partner.getEmailId());
            if (partner.getDob() != null) existingPartner.setDob(partner.getDob());
            if (partner.getAddress() != null) existingPartner.setAddress(partner.getAddress());
            if (partner.getOutletAddress() != null) existingPartner.setOutletAddress(partner.getOutletAddress());

            // Optional image updates
            String folder = "Partner";

            if (aadhaarCardFrontImage != null && !aadhaarCardFrontImage.isEmpty()) {
                existingPartner.setAadhaarCardFrontImage(imageUtil.saveImage(aadhaarCardFrontImage, folder));
            }
            if (aadhaarCardBackImage != null && !aadhaarCardBackImage.isEmpty()) {
                existingPartner.setAadhaarCardBackImage(imageUtil.saveImage(aadhaarCardBackImage, folder));
            }
            if (panCardImage != null && !panCardImage.isEmpty()) {
                existingPartner.setPanCardImage(imageUtil.saveImage(panCardImage, folder));
            }
            if (shopImage != null && !shopImage.isEmpty()) {
                existingPartner.setShopImage(imageUtil.saveImage(shopImage, folder));
            }
            if (businessProofImage != null && !businessProofImage.isEmpty()) {
                existingPartner.setBusinessProofImage(imageUtil.saveImage(businessProofImage, folder));
            }

            updatedPartner = partnerRepository.save(existingPartner);

            responseDateTime = LocalDateTime.now();
            partnerResponse = objectMapper.writeValueAsString(updatedPartner);

            log.info("Successfully updated Partner with ID: {}", id);
            return updatedPartner;

        } catch (Exception ex) {
            partnerResponse = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("Failed to update Partner with ID: {}. Reason: {}", id, ex.getMessage());
            throw ex;
        } finally {
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    partnerRequest,
                    null, // IP address if available
                    responseDateTime,
                    partnerResponse
            );
        }
    }

    @Override
    public List<Partner> getAllPartners() throws JsonProcessingException {
        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching all partners";

        List<Partner> partners = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            log.info(request);
            partners = partnerRepository.findAll();
            response = objectMapper.writeValueAsString(partners);
            responseDateTime = LocalDateTime.now();
            log.info("Fetched {} partners", partners.size());

            return partners;
        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("Error fetching all partners: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    request,
                    null, // ip address if available
                    responseDateTime,
                    response
            );
        }
    }

    @Override
    public PartnerResponseDTO getPartnerById(String partnerId) throws JsonProcessingException {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request = "Fetching Partner with ID: " + partnerId;

        PartnerResponseDTO dto = null;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {

            log.info(request);

            Partner partner = partnerRepository.findById(partnerId)
                    .orElseThrow(() ->
                            new RuntimeException("Partner not found with ID: " + partnerId)
                    );

            dto = convertToDto(partner);

            response = objectMapper.writeValueAsString(dto);
            responseDateTime = LocalDateTime.now();

            log.info("Fetched Partner: {}", partner.getPartnerId());

            return dto;

        } catch (Exception ex) {

            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();

            log.error("Error fetching Partner with ID {}: {}", partnerId, ex.getMessage(), ex);

            throw ex;

        } finally {

            commonFunctions.saveHierarchyLogs(
                    requestDateTime,
                    request,
                    null,
                    responseDateTime,
                    response
            );
        }

    }

    private PartnerResponseDTO convertToDto(Partner partner) {

        PartnerResponseDTO dto = new PartnerResponseDTO();
        dto.setPartnerId(partner.getPartnerId());
        dto.setName(partner.getName());
        dto.setMobileNumber(partner.getMobileNumber());
        dto.setEmailId(partner.getEmailId());
        dto.setTitle(partner.getTitle());
        dto.setAbbreviation(partner.getAbbreviation());
        dto.setDob(partner.getDob());

        dto.setAddress(mapAddress(partner.getAddress()));
        dto.setOutletAddress(mapAddress(partner.getOutletAddress()));

        if (partner.getBankDetails() != null) {
            BanksDetails bank = partner.getBankDetails();
            BankDetailsDTO bankDto = new BankDetailsDTO();
            bankDto.setBankName(bank.getBankName());
            bankDto.setBankAccountNumber(bank.getBankAccountNumber());
            bankDto.setIfscCode(bank.getIfscCode());
            bankDto.setBankHolderName(bank.getBankHolderName());
            bankDto.setIsVerified(bank.getIsVerified());
            dto.setBankDetails(bankDto);
        }


        dto.setAadhaarNumber(partner.getAadhaarNumber());
        dto.setPanNumber(partner.getPanNumber());
        dto.setShopNumber(partner.getShopNumber());
        dto.setBusinessNumber(partner.getBusinessNumber());

        dto.setAadhaarCardFrontImage(fileUrlUtil.getFullUrl(partner.getAadhaarCardFrontImage()));
        dto.setAadhaarCardBackImage( fileUrlUtil.getFullUrl(partner.getAadhaarCardBackImage()));
        dto.setPanCardImage( fileUrlUtil.getFullUrl(partner.getPanCardImage()));
        dto.setShopImage( fileUrlUtil.getFullUrl(partner.getShopImage()));
        dto.setBusinessProofImage( fileUrlUtil.getFullUrl(partner.getBusinessProofImage()));

        dto.setStatus(partner.getStatus());
        dto.setRemarks(partner.getRemarks());
        dto.setRejectionReason(partner.getRejectionReason());

        dto.setCreatedBy(partner.getCreatedBy());
        dto.setCreatedByIPAddress(partner.getCreatedByIPAddress());
        dto.setCreatedOn(partner.getCreatedOn());
        dto.setLastEditedOn(partner.getLastEditedOn());

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


