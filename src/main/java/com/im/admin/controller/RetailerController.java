package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.Retailer;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.RetailerService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import com.im.admin.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

        Retailer retailer = JsonUtil.parseJson(retailerJson, Retailer.class);

        Retailer savedRetailer = retailerService.createRetailer(
                retailer, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        CommonResponse<Retailer> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Distributor created successfully");
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

        return ResponseEntity.ok(updatedRetailer);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRetailer(@PathVariable String id) {
        log.info("Received request to delete retailer with ID: {}", id);
        retailerService.deleteRetailer(id);
        return ResponseEntity.ok("Retailer deleted successfully");
    }

    @GetMapping
    public ResponseEntity<List<Retailer>> getAllRetailers() {
        log.info("Received request to fetch all retailers");
        return ResponseEntity.ok(retailerService.getAllRetailers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Retailer> getRetailerById(@PathVariable String id) {
        log.info("Received request to fetch retailer by ID: {}", id);
        Optional<Retailer> retailer = retailerService.getRetailerById(id);
        return retailer.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}


