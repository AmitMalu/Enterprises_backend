package com.im.admin.controller;

import com.im.admin.dto.CityDTO;
import com.im.admin.dto.CityRequestDTO;
import com.im.admin.dto.CityResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CityService;
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

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    @Autowired
    private CityService cityService;

    @PostMapping
    public ResponseEntity<CommonResponse<CityResponseDTO>> create(@RequestBody CityRequestDTO dto) {
        log.info("API Call: Create City, Request Body: {}", dto);
        return ResponseEntity.ok(cityService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<CityResponseDTO>> update(@PathVariable Long id, @RequestBody CityRequestDTO dto) {
        log.info("API Call: Update City, ID: {}, Request Body: {}", id, dto);
        return ResponseEntity.ok(cityService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<CityResponseDTO>> getById(@PathVariable Long id) {
        log.info("API Call: Get City by ID, ID: {}", id);
        return ResponseEntity.ok(cityService.getById(id));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<CityResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String cityName,
            @RequestParam(required = false) String cityCode,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy
    ) {
        log.info("API Call: Get All Cities, page={}, size={}, cityName={}, cityCode={}, blocked={}, createdBy={}, editedBy={}",
                page, size, cityName, cityCode, blocked, createdBy, editedBy);
        return ResponseEntity.ok(
                cityService.getAll(page, size, cityName, cityCode, blocked, createdBy, editedBy)
        );
    }

    @GetMapping("/by-district/{districtId}")
    public ResponseEntity<CommonResponse<List<CityDTO>>> getCitiesByDistrict(@PathVariable Long districtId) {
        log.info("API Call: Get Cities By District, districtId={}", districtId);
        return ResponseEntity.ok(CommonResponse.success(
                cityService.getCitiesByDistrict(districtId), "Cities loaded successfully"));
    }

    @GetMapping("/download-city-template")
    public ResponseEntity<Resource> downloadCityTemplate() throws IOException {
        log.info("API Call: Download City Template");
        ByteArrayResource resource = cityService.generateCitySampleTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=city_sample_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload-city")
    public ResponseEntity<Resource> uploadCityExcel(@RequestParam("file") MultipartFile file,
                                                    @RequestParam("uploadedBy") String uploadedBy) {
        log.info("Bulk uploading cities from Excel by user: {}", uploadedBy);
        try {
            ByteArrayResource processedFile = cityService.uploadCitiesFromExcel(file, uploadedBy);

            String originalFileName = file.getOriginalFilename();
            String baseName = originalFileName != null && originalFileName.contains(".")
                    ? originalFileName.substring(0, originalFileName.lastIndexOf('.'))
                    : "city_upload";

            String returnFileName = baseName + "_Upload_Result.xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + returnFileName + "\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(processedFile);
        } catch (Exception e) {
            log.error("Bulk upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}

