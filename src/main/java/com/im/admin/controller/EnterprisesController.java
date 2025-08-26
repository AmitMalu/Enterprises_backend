package com.im.admin.controller;

import com.im.admin.entities.Distributor;
import com.im.admin.entities.Enterprises;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.EnterprisesService;
import com.im.admin.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/enterprises")
@Validated
@Slf4j
public class EnterprisesController {

    @Autowired
    private EnterprisesService enterprisesService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<Enterprises>> saveEnterprise(
            @RequestPart("data") String enterprisesJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: saveEnterprise with data: {}", enterprisesJson);

        Enterprises enterprises = JsonUtil.parseJson(enterprisesJson, Enterprises.class);

        Enterprises savedEnterprises = enterprisesService.saveEnterprise(
                enterprises, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        log.info("Enterprise saved successfully with ID: {}", savedEnterprises.getId());

        CommonResponse<Enterprises> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Enterprises created successfully");
        response.setData(savedEnterprises);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Enterprises> updateEnterprise(
            @PathVariable String id,
            @RequestPart("data") String enterprisesJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("API Call: updateEnterprise with ID: {} and data: {}", id, enterprisesJson);

        Enterprises enterprises = JsonUtil.parseJson(enterprisesJson, Enterprises.class);

        Enterprises updatedEnterprise = enterprisesService.updateEnterprise(
                id,
                enterprises,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        log.info("Enterprise updated successfully with ID: {}", id);

        return ResponseEntity.ok(updatedEnterprise);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<Enterprises>> getAllEnterprises() {
        log.info("API Call: getAllEnterprises");
        List<Enterprises> enterprises = enterprisesService.getAllEnterprises();
        log.info("Fetched {} enterprises", enterprises.size());
        return ResponseEntity.ok(enterprises);
    }

    @GetMapping("/getById/{enterpriseId}")
    public ResponseEntity<?> getEnterpriseById(@PathVariable String enterpriseId) {
        log.info("API Call: getEnterpriseById with ID: {}", enterpriseId);
        Enterprises enterprise = enterprisesService.getEnterpriseById(enterpriseId);
        return ResponseEntity.ok(enterprise);
    }

    @DeleteMapping("/delete/{enterpriseId}")
    public ResponseEntity<?> deleteEnterprise(@PathVariable String enterpriseId) {
        log.info("API Call: deleteEnterprise with ID: {}", enterpriseId);
        enterprisesService.deleteEnterprise(enterpriseId);
        log.info("Enterprise deleted successfully with ID: {}", enterpriseId);
        return ResponseEntity.ok("Enterprise " + enterpriseId + " deleted successfully.");
    }

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        log.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest().body(errors);
    }
}
