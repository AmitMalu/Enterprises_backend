package com.scs.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.Enterprises;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.EnterpriseResponseDTO;
import com.scs.admin.service.EnterprisesService;
import com.scs.admin.util.JsonUtil;
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

    @Autowired
    private ObjectMapper objectMapper;

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

        log.info("Enterprise saved successfully with ID: {}", savedEnterprises.getEnterpriseId());

        CommonResponse<Enterprises> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("Enterprises created successfully");
        response.setData(savedEnterprises);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }



@PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public ResponseEntity<CommonResponse<EnterpriseResponseDTO>> updateEnterprise(
        @PathVariable String id,
        @RequestPart(value = "data") EnterpriseResponseDTO dto,
        @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
        @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
        @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
        @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
        @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
) throws IOException {

    log.info("API Call: updateEnterprise with ID: {} and data: {}", id, dto);

    Enterprises enterprise = objectMapper.convertValue(dto, Enterprises.class);

    Enterprises updatedEnterprise = enterprisesService.updateEnterprise(
            id,
            enterprise,
            aadhaarCardFrontImage,
            aadhaarCardBackImage,
            panCardImage,
            shopImage,
            businessProofImage
    );

    EnterpriseResponseDTO responseDto =
            objectMapper.convertValue(updatedEnterprise, EnterpriseResponseDTO.class);

    CommonResponse<EnterpriseResponseDTO> response =
            new CommonResponse<>(200, "Enterprise updated successfully", responseDto);

    return ResponseEntity.ok(response);
}

    @GetMapping("/getAll")
    public ResponseEntity<List<Enterprises>> getAllEnterprises() throws JsonProcessingException {
        log.info("API Call: getAllEnterprises");
        List<Enterprises> enterprises = enterprisesService.getAllEnterprises();
        log.info("Fetched {} enterprises", enterprises.size());
        return ResponseEntity.ok(enterprises);
    }

    @GetMapping("/{enterpriseId}")
    public ResponseEntity<CommonResponse<EnterpriseResponseDTO>> getEnterpriseById(
            @PathVariable String enterpriseId) {

        log.info("API Call: getEnterpriseById with ID: {}", enterpriseId);

        try {

            EnterpriseResponseDTO dto = enterprisesService.getEnterpriseById(enterpriseId);

            return ResponseEntity.ok(
                    CommonResponse.success(dto, "Enterprise fetched successfully")
            );

        } catch (Exception ex) {

            log.error("Error fetching enterprise: ", ex);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.failure(ex.getMessage()));
        }
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
