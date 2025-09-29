package com.im.admin.controller;

import com.im.admin.dto.PinCodeDTO;
import com.im.admin.dto.PinCodeRequestDTO;
import com.im.admin.dto.PinCodeResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PinCodeLocationResponse;
import com.im.admin.service.PinCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pincodes")
@RequiredArgsConstructor
public class PinCodeController {

    @Autowired
    private PinCodeService pinCodeService;

    @PostMapping
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> createPinCode(@RequestBody PinCodeRequestDTO dto) {
        log.info("Received request to create PinCode: {}", dto);
        CommonResponse<PinCodeResponseDTO> response = pinCodeService.save(dto);
        log.info("PinCode created successfully: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> updatePinCode(@PathVariable Long id, @RequestBody PinCodeRequestDTO dto) {
        log.info("Received request to update PinCode with ID: {}", id);
        CommonResponse<PinCodeResponseDTO> response = pinCodeService.update(id, dto);
        log.info("PinCode updated successfully: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PinCodeResponseDTO>> getPinCodeById(@PathVariable Long id) {
        log.info("Fetching PinCode with ID: {}", id);
        CommonResponse<PinCodeResponseDTO> response = pinCodeService.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<PinCodeResponseDTO>>> getAllPinCodes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String pinCode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy) {

        log.info("Fetching PinCodes with filters - pinCode: {}, blocked: {}, createdBy: {}, editedBy: {}", pinCode, blocked, createdBy, editedBy);
        CommonResponse<Page<PinCodeResponseDTO>> response = pinCodeService.getAll(page, size, pinCode, blocked, createdBy, editedBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-city/{cityId}")
    public ResponseEntity<CommonResponse<List<PinCodeDTO>>> getPinCodesByCity(@PathVariable Long cityId) {
        log.info("Fetching PinCodes for cityId: {}", cityId);
        List<PinCodeDTO> pinCodes = pinCodeService.getPinCodesByCity(cityId);
        return ResponseEntity.ok(CommonResponse.success(pinCodes, "Pin codes loaded successfully"));
    }

    @GetMapping("/lookup/{pinCode}")
    public ResponseEntity<CommonResponse<PinCodeLocationResponse>> lookupPinCodeLocation(@PathVariable String pinCode) {
        log.info("Looking up location for PinCode: {}", pinCode);
        PinCodeLocationResponse response = pinCodeService.lookupByPinCode(pinCode);
        return ResponseEntity.ok(CommonResponse.success(response, "Location found for given pin code"));
    }

    @GetMapping("/download-sample-template")
    public ResponseEntity<Resource> downloadPinCodeTemplate() {
        log.info("Downloading PinCode sample template");
        ByteArrayResource resource = pinCodeService.generateSampleTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pincode_sample_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<byte[]> uploadPinCodeExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadedBy") String uploadedBy) {

        log.info("Uploading PinCode Excel file by: {}", uploadedBy);
        try {
            ByteArrayInputStream updatedFile = pinCodeService.uploadPinCodesFromExcel(file, uploadedBy);

            String originalFileName = file.getOriginalFilename();
            String baseName = (originalFileName != null && originalFileName.contains("."))
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : "Upload";

            String returnFileName = baseName + "_Upload_Result.xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + returnFileName + "\"");

            log.info("PinCode Excel upload processed successfully");
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(updatedFile.readAllBytes());

        } catch (Exception e) {
            log.error("PinCode Excel upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Upload failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

}

