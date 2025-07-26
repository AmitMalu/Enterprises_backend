package com.im.admin.controller;


import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistrictService;
import lombok.RequiredArgsConstructor;
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

@RestController
@RequestMapping("/api/districts")
@RequiredArgsConstructor
public class DistrictController {

    @Autowired
    private DistrictService districtService;

    @PostMapping
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> create(@RequestBody DistrictRequestDTO dto) {
        return ResponseEntity.ok(districtService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> update(@PathVariable Long id, @RequestBody DistrictRequestDTO dto) {
        return ResponseEntity.ok(districtService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<DistrictResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(districtService.getById(id));
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
        return ResponseEntity.ok(
                districtService.getAll(page, size, districtName, districtCode, blocked, createdBy, editedBy)
        );
    }

    @GetMapping("/by-state/{stateId}")
    public ResponseEntity<CommonResponse<List<DistrictDTO>>> getDistrictsByState(@PathVariable Long stateId) {
        List<DistrictDTO> districts = districtService.getDistrictsByState(stateId);
        return ResponseEntity.ok(CommonResponse.success(districts, "Districts loaded successfully"));
    }

    @GetMapping("/download-sample-template")
    public ResponseEntity<Resource> downloadSampleTemplate() throws IOException {
        ByteArrayResource resource = districtService.generateSampleTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=district_sample_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<CommonResponse<String>> uploadDistrictExcel(@RequestParam("file") MultipartFile file,
                                                                      @RequestParam("uploadedBy") String uploadedBy) {
        try {
            String result = districtService.uploadDistrictsFromExcel(file, uploadedBy);
            return ResponseEntity.ok(CommonResponse.success("Upload Result", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.failure("Upload failed: " + e.getMessage()));
        }
    }

}

