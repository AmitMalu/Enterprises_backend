package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.im.admin.entities.Distributor;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DistributorResponseDTO;
import com.im.admin.service.DistributorService;
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

@Slf4j
@RestController
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
@Validated
public class DistributorController {

    @Autowired
    private DistributorService distributorService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Distributor>> createDistributor(
            @RequestPart("data") String distributorJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: createDistributor with data: {}", distributorJson);

        Distributor distributor = JsonUtil.parseJson(distributorJson, Distributor.class);

        Distributor savedDistributor = distributorService.createDistributor(
                distributor, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        log.info("Distributor created successfully: {}", savedDistributor);

        CommonResponse<Distributor> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Distributor created successfully");
        response.setData(savedDistributor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<DistributorResponseDTO>> updateDistributor(
            @PathVariable String id,
            @RequestPart("data") DistributorResponseDTO dto,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: updateDistributor with ID: {} and data: {}", id, dto);

        // DTO → Entity
        Distributor distributor = objectMapper.convertValue(dto, Distributor.class);

        Distributor updatedDistributor = distributorService.updateDistributor(
                id,
                distributor,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        DistributorResponseDTO responseDto;

        try {
            // Entity → DTO
            responseDto = objectMapper.convertValue(updatedDistributor, DistributorResponseDTO.class);
        } catch (IllegalArgumentException ex) {
            log.error("Error converting Distributor entity to DTO for ID: {}", id, ex);
            throw new RuntimeException("Failed to convert Distributor entity to DTO");
        }

        CommonResponse<DistributorResponseDTO> response =
                new CommonResponse<>(200, "Distributor updated successfully", responseDto);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<Distributor>> getAllDistributors() {
        log.info("API Call: getAllDistributors");
        List<Distributor> distributors = distributorService.getAllDistributors();
        log.info("Fetched {} distributors", distributors.size());
        return ResponseEntity.ok(distributors);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<DistributorResponseDTO>> getDistributorById(
            @PathVariable String id) {

        log.info("API Call: getDistributorById for id: {}", id);

        DistributorResponseDTO dto = distributorService.getDistributorById(id);

        return ResponseEntity.ok(
                CommonResponse.success(dto, "Distributor fetched successfully")
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDistributor(@PathVariable String id) {
        log.info("API Call: deleteDistributor for id: {}", id);
        distributorService.deleteDistributor(id);
        log.info("Distributor deleted successfully: {}", id);
        return ResponseEntity.ok("Distributor deleted successfully." + id);
    }
}

