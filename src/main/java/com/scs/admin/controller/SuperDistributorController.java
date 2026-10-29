package com.scs.admin.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.SuperDistributor;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.SuperDistributorResponseDTO;
import com.scs.admin.service.SuperDistributorService;
import com.scs.admin.service.impl.PhoneNumberValidationService;
import com.scs.admin.util.JsonUtil;
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
@RequestMapping("/api/super-distributors")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SuperDistributorController {

    @Autowired
    private SuperDistributorService service;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<SuperDistributor>> createSuperDistributor(
            @RequestPart("data") String superDistributorJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("Received request to create SuperDistributor");
        log.debug("Payload JSON: {}", superDistributorJson);

        SuperDistributor superDistributor = JsonUtil.parseJson(superDistributorJson, SuperDistributor.class);
        log.debug("Parsed SuperDistributor object: {}", superDistributor);

        SuperDistributor savedSuperDistributor = service.saveSuperDistributor(
                superDistributor,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        log.info("SuperDistributor created successfully with ID: {}", savedSuperDistributor.getSuperDistributorId());

        CommonResponse<SuperDistributor> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Super Distributor created successfully");
        response.setData(savedSuperDistributor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

//    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ResponseEntity<SuperDistributor> updateSuperDistributor(
//            @PathVariable String id,
//            @RequestPart("data") String superDistributorJson,
//            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
//            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
//            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
//            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
//            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
//    ) throws IOException {
//
//        log.info("Received request to update SuperDistributor with ID: {}", id);
//        log.debug("Update payload JSON: {}", superDistributorJson);
//
//        SuperDistributor superDistributor = JsonUtil.parseJson(superDistributorJson, SuperDistributor.class);
//        log.debug("Parsed SuperDistributor object for update: {}", superDistributor);
//
//        SuperDistributor updatedDistributor = service.updateSuperDistributor(
//                id,
//                superDistributor,
//                aadhaarCardFrontImage,
//                aadhaarCardBackImage,
//                panCardImage,
//                shopImage,
//                businessProofImage
//        );
//
//        log.info("SuperDistributor updated successfully for ID: {}", id);
//        return ResponseEntity.ok(updatedDistributor);
//    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<SuperDistributorResponseDTO>> updateSuperDistributor(
            @PathVariable String id,
            @RequestPart(value = "data") SuperDistributorResponseDTO dto,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: updateSuperDistributor with ID: {} and data: {}", id, dto);

        SuperDistributor distributor =
                objectMapper.convertValue(dto, SuperDistributor.class);

        SuperDistributor updatedDistributor = service.updateSuperDistributor(
                id,
                distributor,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        SuperDistributorResponseDTO responseDto =
                objectMapper.convertValue(updatedDistributor, SuperDistributorResponseDTO.class);

        CommonResponse<SuperDistributorResponseDTO> response =
                new CommonResponse<>(200, "SuperDistributor updated successfully", responseDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<SuperDistributorResponseDTO>> getById(@PathVariable String id) {

        log.info("Fetching SuperDistributor with ID: {}", id);

        SuperDistributorResponseDTO dto = service.getSuperDistributor(id);

        return ResponseEntity.ok(
                CommonResponse.success(dto, "Super Distributor fetched successfully")
        );
    }

    @GetMapping
    public ResponseEntity<List<SuperDistributor>> getAll() {
        log.info("Fetching all SuperDistributors");
        try {
            List<SuperDistributor> distributors = service.getAllSuperDistributors();
            log.info("Fetched {} SuperDistributors", distributors.size());
            return ResponseEntity.ok(distributors);
        } catch (Exception e) {
            log.error("Error fetching all SuperDistributors", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        log.info("Deleting SuperDistributor with ID: {}", id);
        try {
            service.deleteSuperDistributor(id);
            log.info("Successfully deleted SuperDistributor with ID: {}", id);
            return ResponseEntity.ok("SuperDistributor deleted successfully.");
        } catch (Exception e) {
            log.error("Error deleting SuperDistributor with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete SuperDistributor.");
        }
    }

}

