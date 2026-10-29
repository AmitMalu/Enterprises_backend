package com.scs.admin.controller;


import com.scs.admin.dto.DistrictDTO;
import com.scs.admin.dto.DistrictRequestDTO;
import com.scs.admin.dto.DistrictResponseDTO;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.DistrictService;
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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    public ResponseEntity<byte[]> uploadDistrictExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("uploadedBy") String uploadedBy) {

        try {
            ByteArrayInputStream updatedFile = districtService.uploadDistrictsFromExcel(file, uploadedBy);

            String originalFileName = file.getOriginalFilename();
            String baseName = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : "DistrictUpload";

            String returnFileName = baseName + "_Upload_Result.xlsx";

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + returnFileName + "\"");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(updatedFile.readAllBytes());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Upload failed: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

}

