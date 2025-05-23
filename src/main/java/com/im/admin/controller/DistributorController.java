package com.im.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.im.admin.entities.Distributor;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistributorService;
import com.im.admin.service.impl.PhoneNumberValidationService;
import com.im.admin.util.JsonUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/distributors")
@RequiredArgsConstructor
@Validated
public class DistributorController {

    @Autowired
    private DistributorService distributorService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Distributor>> createDistributor(
            @RequestPart("data") String distributorJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        Distributor distributor = JsonUtil.parseJson(distributorJson, Distributor.class);

        Distributor savedDistributor = distributorService.createDistributor(
                distributor, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        CommonResponse<Distributor> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Distributor created successfully");
        response.setData(savedDistributor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Distributor> updateDistributor(
            @PathVariable String id,
            @RequestPart("data") String distributorJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        Distributor distributor = JsonUtil.parseJson(distributorJson, Distributor.class);

        Distributor updatedDistributor = distributorService.updateDistributor(
                id,
                distributor,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        return ResponseEntity.ok(updatedDistributor);
    }

    @GetMapping
    public ResponseEntity<List<Distributor>> getAllDistributors() {
        return ResponseEntity.ok(distributorService.getAllDistributors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Distributor> getDistributorById(@PathVariable String id) {
        return ResponseEntity.ok(distributorService.getDistributorById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDistributor(@PathVariable String id) {
        distributorService.deleteDistributor(id);
        return ResponseEntity.ok("Distributor deleted successfully." + id);
}
}


