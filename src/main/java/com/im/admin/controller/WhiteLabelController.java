package com.im.admin.controller;

import com.im.admin.dto.WhiteLabelDropdownDTO;
import com.im.admin.entities.Distributor;
import com.im.admin.entities.WhiteLabels;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.WhiteLabelService;
import com.im.admin.util.JsonUtil;
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

        WhiteLabels whiteLabels = JsonUtil.parseJson(whiteLabelsJson, WhiteLabels.class);

        WhiteLabels savedWhiteLabel = whiteLabelService.saveWhiteLabel(
                whiteLabels, aadhaarCardFrontImage, aadhaarCardBackImage, panCardImage, shopImage, businessProofImage);

        CommonResponse<WhiteLabels> response = new CommonResponse<>();
        response.setStatusCode(HttpStatus.OK.value());
        response.setMessage("White Label Created Successfully");
        response.setData(savedWhiteLabel);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<WhiteLabels> updateWhiteLabel(
            @PathVariable String id,
            @RequestPart("data") String whiteLabelsJson,
            @RequestPart(value = "aadhaarCardFrontImage", required = false) MultipartFile aadhaarCardFrontImage,
            @RequestPart(value = "aadhaarCardBackImage", required = false) MultipartFile aadhaarCardBackImage,
            @RequestPart(value = "panCardImage", required = false) MultipartFile panCardImage,
            @RequestPart(value = "shopImage", required = false) MultipartFile shopImage,
            @RequestPart(value = "businessProofImage", required = false) MultipartFile businessProofImage
    ) throws IOException {

        WhiteLabels whiteLabels = JsonUtil.parseJson(whiteLabelsJson, WhiteLabels.class);

        WhiteLabels updatedDistributor = whiteLabelService.updateWhiteLabel(
                id,
                whiteLabels,
                aadhaarCardFrontImage,
                aadhaarCardBackImage,
                panCardImage,
                shopImage,
                businessProofImage
        );

        return ResponseEntity.ok(updatedDistributor);
    }

    @GetMapping("/getAll")
    public ResponseEntity<List<WhiteLabels>> getAllWhiteLabels() {
        log.info("Fetching all WhiteLabels");
        return ResponseEntity.ok(whiteLabelService.getAllWhiteLabels());
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<WhiteLabels> getWhiteLabelById(@PathVariable String id) {
        log.info("Fetching WhiteLabel by ID: {}", id);
        return ResponseEntity.ok(whiteLabelService.getWhiteLabelById(id));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteWhiteLabel(@PathVariable String id) {
        log.info("Deleting WhiteLabel: {}", id);
        whiteLabelService.deleteWhiteLabel(id);
        return ResponseEntity.ok("WhiteLabel " + id + " deleted successfully.");
    }

    @GetMapping("/admin-dropdown")
    public ResponseEntity<CommonResponse<List<WhiteLabelDropdownDTO>>> getAdminWhiteLabelDropdown() {
        List<WhiteLabelDropdownDTO> list = whiteLabelService.getAdminWhiteLabels();
        return ResponseEntity.ok(CommonResponse.success(list, "Admin white labels loaded successfully"));
    }

}


