package com.im.admin.controller;


import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistrictService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    @Autowired
    private DistrictService districtService;

    @PostMapping
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> create(@RequestBody DistrictRequestDTO dto) {
        log.info("API Call: createDistrict with data: {}", dto);
        CommonResponse<DistrictResponseDTO> response = districtService.save(dto);
        log.info("District created successfully: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> update(@PathVariable Long id, @RequestBody DistrictRequestDTO dto) {
        log.info("API Call: updateDistrict for id: {} with data: {}", id, dto);
        CommonResponse<DistrictResponseDTO> response = districtService.update(id, dto);
        log.info("District updated successfully: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> getById(@PathVariable Long id) {
        log.info("API Call: getDistrictById for id: {}", id);
        CommonResponse<DistrictResponseDTO> response = districtService.getById(id);
        log.info("District fetched: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<DistrictResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String districtName,
            @RequestParam(required = false) String districtCode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy
    ) {
        log.info("API Call: getAllDistricts with filters - districtName: {}, districtCode: {}, blocked: {}, createdBy: {}, editedBy: {}",
                districtName, districtCode, blocked, createdBy, editedBy);
        CommonResponse<Page<DistrictResponseDTO>> response = districtService.getAll(page, size, districtName, districtCode, blocked, createdBy, editedBy);
        log.info("Fetched {} districts", response.getData().getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/by-state/{stateId}")
    public ResponseEntity<CommonResponse<List<DistrictDTO>>> getDistrictsByState(@PathVariable Long stateId) {
        log.info("API Call: getDistrictsByState for stateId: {}", stateId);
        List<DistrictDTO> districts = districtService.getDistrictsByState(stateId);
        log.info("Fetched {} districts for stateId: {}", districts.size(), stateId);
        return ResponseEntity.ok(CommonResponse.success(districts, "Districts loaded successfully"));
    }

    @GetMapping("/download-sample-template")
    public ResponseEntity<Resource> downloadSampleTemplate() throws IOException {
        log.info("API Call: downloadSampleTemplate for districts");
        ByteArrayResource resource = districtService.generateSampleTemplate();
        log.info("Sample template generated successfully");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=district_sample_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<CommonResponse<String>> uploadDistrictExcel(@RequestParam("file") MultipartFile file,
                                                                      @RequestParam("uploadedBy") String uploadedBy) {
        log.info("API Call: uploadDistrictExcel by {}", uploadedBy);
        try {
            String result = districtService.uploadDistrictsFromExcel(file, uploadedBy);
            log.info("District bulk upload successful: {}", result);
            return ResponseEntity.ok(CommonResponse.success("Upload Result", result));
        } catch (Exception e) {
            log.error("District bulk upload failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(CommonResponse.failure("Upload failed: " + e.getMessage()));
        }
    }
}

