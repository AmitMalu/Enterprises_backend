package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Partner;
import com.im.admin.entities.Retailer;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PartnerResponseDTO;
import com.im.admin.service.PartnerService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import com.im.admin.util.JsonUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/partners")
@Slf4j
public class PartnerController {

    @Autowired
    private PartnerService partnerService;

    @Autowired
    private ObjectMapper objectMapper;


    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Partner>> createRetailer(
            @RequestPart("data") String partnerJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {
        log.info("Received request to create Partner: {}", partnerJson);
        Partner partner = JsonUtil.parseJson(partnerJson, Partner.class);

        Partner savedPartner= partnerService.savePartner(
                partner, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        CommonResponse<Partner> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Partner created successfully");
        response.setData(savedPartner);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<Partner> updatePartner(
//            @PathVariable String id,
//            @RequestPart("data") String partnerJson,
//            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
//            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
//            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
//            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
//            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
//    ) throws IOException {
//
//        Partner partner = JsonUtil.parseJson(partnerJson, Partner.class);
//
//        Partner updatedPartner = partnerService.updatePartner(
//                id,
//                partner,
//                aadhaarCardFrontImage,
//                aadhaarCardBackImage,
//                panCardImage,
//                shopImage,
//                businessProofImage
//        );
//
//        return ResponseEntity.ok(updatedPartner);
//    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<PartnerResponseDTO>> updatePartner(
            @PathVariable String id,
            @RequestPart("data") PartnerResponseDTO dto,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: updatePartner with ID: {} and data: {}", id, dto);

        // DTO → Entity
        Partner partner = objectMapper.convertValue(dto, Partner.class);

        Partner updatedPartner = partnerService.updatePartner(
                id,
                partner,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        PartnerResponseDTO responseDto;

        try {
            // Entity → DTO
            responseDto = objectMapper.convertValue(updatedPartner, PartnerResponseDTO.class);
        } catch (IllegalArgumentException ex) {
            log.error("Error converting Partner entity to DTO for ID: {}", id, ex);
            throw new RuntimeException("Failed to convert Partner entity to DTO");
        }

        CommonResponse<PartnerResponseDTO> response =
                new CommonResponse<>(200, "Partner updated successfully", responseDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Partner>> getAllPartners() throws JsonProcessingException {
        log.info("Received request to fetch all Partners");
        return ResponseEntity.ok(partnerService.getAllPartners());
    }

    @GetMapping("/{partnerId}")
    public ResponseEntity<CommonResponse<PartnerResponseDTO>> getPartnerById(
            @PathVariable String partnerId) throws JsonProcessingException {

        log.info("Received request to fetch Partner with ID: {}", partnerId);

        PartnerResponseDTO dto = partnerService.getPartnerById(partnerId);

        return ResponseEntity.ok(
                CommonResponse.success(dto, "Partner fetched successfully")
        );
    }

    @DeleteMapping("/{partnerId}")
    public ResponseEntity<String> deletePartner(@PathVariable String partnerId) {
        log.info("Received request to delete Partner with ID: {}", partnerId);
        partnerService.deletePartner(partnerId);
        return ResponseEntity.ok("SuperDistributor deleted successfully.");    }
}
