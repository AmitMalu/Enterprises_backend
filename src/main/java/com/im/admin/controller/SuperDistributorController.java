package com.im.admin.controller;


import com.im.admin.entities.Distributor;
import com.im.admin.entities.SuperDistributor;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.SuperDistributorService;
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
@RequestMapping("/api/super-distributors")
@RequiredArgsConstructor
@Validated
public class SuperDistributorController {

    @Autowired
    private SuperDistributorService service;

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

        SuperDistributor superDistributor = JsonUtil.parseJson(superDistributorJson, SuperDistributor.class);

        SuperDistributor savedSuperDistributor = service.saveSuperDistributor(
                superDistributor, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        CommonResponse<SuperDistributor> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Super Distributor created successfully");
        response.setData(savedSuperDistributor);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SuperDistributor> updateSuperDistributor(
            @PathVariable String id,
            @RequestPart("data") String superDistributorJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        SuperDistributor superDistributor = JsonUtil.parseJson(superDistributorJson, SuperDistributor.class);

        SuperDistributor updatedDistributor = service.updateSuperDistributor(
                id,
                superDistributor,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        return ResponseEntity.ok(updatedDistributor);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SuperDistributor> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getSuperDistributor(id));
    }

    @GetMapping
    public ResponseEntity<List<SuperDistributor>> getAll() {
        return ResponseEntity.ok(service.getAllSuperDistributors());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable String id) {
        service.deleteSuperDistributor(id);
        return ResponseEntity.ok("SuperDistributor deleted successfully.");
    }
}

