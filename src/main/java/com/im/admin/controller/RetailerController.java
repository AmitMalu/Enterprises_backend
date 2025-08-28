package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerResponseDTO;
import com.im.admin.service.RetailerService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import com.im.admin.util.JsonUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/retailers")
public class RetailerController {

    @Autowired
    private RetailerService retailerService;

    @Autowired
    private PhoneNumberValidationService phoneNumberValidationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Retailer>> createRetailer(
            @RequestPart("data") String retailerJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {
        log.info("Received request to create Retailer: {}", retailerJson);

        Retailer retailer = JsonUtil.parseJson(retailerJson, Retailer.class);
        Retailer savedRetailer = retailerService.createRetailer(
                retailer, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        log.info("Retailer created successfully with ID: {}", savedRetailer.getRetailerId());

        CommonResponse<Retailer> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.CREATED.value());
        response.setMessage("Retailer created successfully");
        response.setData(savedRetailer);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Retailer> updateRetailer(
            @PathVariable String id,
            @RequestPart("data") String retailerJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {
        log.info("Received request to update Retailer with ID: {}", id);
        log.debug("Update payload: {}", retailerJson);

        Retailer retailer = JsonUtil.parseJson(retailerJson, Retailer.class);
        Retailer updatedRetailer = retailerService.updateRetailer(
                id,
                retailer,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        log.info("Retailer updated successfully with ID: {}", id);
        return ResponseEntity.ok(updatedRetailer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRetailer(@PathVariable String id) {
        log.info("Received request to delete Retailer with ID: {}", id);
        retailerService.deleteRetailer(id);
        log.info("Retailer deleted successfully with ID: {}", id);
        return ResponseEntity.ok("Retailer deleted successfully");
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<RetailerResponseDTO>>> getAllRetailers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdOn") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        log.info("Received request to fetch all Retailers - page: {}, size: {}, sortBy: {}, direction: {}",
                page, size, sortBy, direction);
        try {
            Sort sort = direction.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<RetailerResponseDTO> retailerPage = retailerService.getAllRetailers(pageable);

            log.info("Fetched {} retailers", retailerPage.getTotalElements());
            return ResponseEntity.ok(CommonResponse.success(retailerPage, "Retailers fetched successfully"));
        } catch (Exception ex) {
            log.error("Error fetching retailers: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure("Something went wrong while fetching retailers"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<RetailerResponseDTO>> getRetailerById(@PathVariable String id) {
        log.info("Received request to fetch Retailer with ID: {}", id);
        try {
            RetailerResponseDTO responseDto = retailerService.getRetailerById(id);
            log.info("Retailer fetched successfully with ID: {}", id);
            return ResponseEntity.ok(CommonResponse.success(responseDto, "Retailer data fetched successfully"));
        } catch (EntityNotFoundException e) {
            log.warn("Retailer not found with ID: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CommonResponse.failure("Retailer not found"));
        } catch (Exception ex) {
            log.error("Error fetching Retailer with ID {}: ", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure("Something went wrong"));
        }
    }
}


