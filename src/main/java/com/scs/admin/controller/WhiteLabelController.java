package com.scs.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scs.admin.dto.WhiteLabelDropdownDTO;
import com.scs.admin.entities.Distributor;
import com.scs.admin.entities.WhiteLabels;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.WhiteLabelResponseDTO;
import com.scs.admin.service.WhiteLabelService;
import com.scs.admin.util.JsonUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/api/white-labels")
@Slf4j
@RequiredArgsConstructor
public class WhiteLabelController {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WhiteLabelService whiteLabelService;

        @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<CommonResponse<WhiteLabels>> saveWhiteLabel(
                @RequestPart("data") String whiteLabelsJson,
                @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
                @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
                @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
                @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
                @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
        ) throws IOException {

            log.info("Received request to create WhiteLabel with data: {}", whiteLabelsJson);

            WhiteLabels whiteLabels = JsonUtil.parseJson(whiteLabelsJson, WhiteLabels.class);

            log.debug("Parsed WhiteLabel object: {}", whiteLabels);

            WhiteLabels savedWhiteLabel = whiteLabelService.saveWhiteLabel(
                    whiteLabels, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

            log.info("WhiteLabel saved successfully with ID: {}", savedWhiteLabel.getWhiteLabelId());

            CommonResponse<WhiteLabels> response = new CommonResponse<>();
            response.setStatusCode(HttpStatus.OK.value());
            response.setMessage("White Label Created Successfully");
            response.setData(savedWhiteLabel);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonResponse<WhiteLabelResponseDTO>> updateWhiteLabel(
            @PathVariable String id,
            @RequestPart("data") WhiteLabelResponseDTO dto,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        log.info("Received request to update WhiteLabel with ID: {}", id);
        log.debug("Update DTO payload: {}", dto);

        // DTO → Entity
        WhiteLabels whiteLabels = objectMapper.convertValue(dto, WhiteLabels.class);

        WhiteLabels updatedWhiteLabel = whiteLabelService.updateWhiteLabel(
                id,
                whiteLabels,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        WhiteLabelResponseDTO responseDto;

        try {
            // Entity → DTO
            responseDto = objectMapper.convertValue(updatedWhiteLabel, WhiteLabelResponseDTO.class);
        } catch (IllegalArgumentException ex) {
            log.error("Error converting WhiteLabel entity to DTO for ID: {}", id, ex);
            throw new RuntimeException("Failed to convert WhiteLabel entity to DTO");
        }

        CommonResponse<WhiteLabelResponseDTO> response =
                new CommonResponse<>(200, "WhiteLabel updated successfully", responseDto);

        log.info("WhiteLabel updated successfully for ID: {}", id);

        return ResponseEntity.ok(response);
    }

        @GetMapping("/getAll")
        public ResponseEntity<List<WhiteLabels>> getAllWhiteLabels() {
            log.info("Fetching all WhiteLabels");
            List<WhiteLabels> whiteLabelsList = whiteLabelService.getAllWhiteLabels();
            log.debug("Total WhiteLabels fetched: {}", whiteLabelsList.size());
            return ResponseEntity.ok(whiteLabelsList);
        }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<WhiteLabelResponseDTO>> getWhiteLabelById(
            @PathVariable String id) {

        log.info("Fetching WhiteLabel by ID: {}", id);

        WhiteLabelResponseDTO dto = whiteLabelService.getWhiteLabelById(id);

        return ResponseEntity.ok(
                CommonResponse.success(dto, "WhiteLabel fetched successfully")
        );
    }

        @DeleteMapping("/delete/{id}")
        public ResponseEntity<String> deleteWhiteLabel(@PathVariable String id) {
            log.info("Deleting WhiteLabel with ID: {}", id);
            whiteLabelService.deleteWhiteLabel(id);
            log.info("WhiteLabel deleted successfully: {}", id);
            return ResponseEntity.ok("WhiteLabel " + id + " deleted successfully.");
        }

        @GetMapping("/admin-dropdown")
        public ResponseEntity<CommonResponse<List<WhiteLabelDropdownDTO>>> getAdminWhiteLabelDropdown() {
            log.info("Fetching Admin WhiteLabel dropdown list");
            List<WhiteLabelDropdownDTO> list = whiteLabelService.getAdminWhiteLabels();
            log.debug("Admin dropdown list size: {}", list.size());
            return ResponseEntity.ok(CommonResponse.success(list, "Admin white labels loaded successfully"));
        }

}


