package com.im.admin.controller;

import com.im.admin.dto.CityDTO;
import com.im.admin.dto.CityRequestDTO;
import com.im.admin.dto.CityResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CityService;
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
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    @Autowired
    private CityService cityService;

    @PostMapping
    public ResponseEntity<CommonResponse<CityResponseDTO>> create(@RequestBody CityRequestDTO dto) {
        return ResponseEntity.ok(cityService.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<CityResponseDTO>> update(@PathVariable Long id, @RequestBody CityRequestDTO dto) {
        return ResponseEntity.ok(cityService.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<CityResponseDTO>> getById(@PathVariable Long id) {
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
        return ResponseEntity.ok(
                cityService.getAll(page, size, cityName, cityCode, blocked, createdBy, editedBy)
        );
    }

    @GetMapping("/by-district/{districtId}")
    public ResponseEntity<CommonResponse<List<CityDTO>>> getCitiesByDistrict(@PathVariable Long districtId) {
        return ResponseEntity.ok(CommonResponse.success(
                cityService.getCitiesByDistrict(districtId), "Cities loaded successfully"));
    }

    @GetMapping("/download-city-template")
    public ResponseEntity<Resource> downloadCityTemplate() throws IOException {
        ByteArrayResource resource = cityService.generateCitySampleTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=city_sample_template.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(resource);
    }

    @PostMapping("/bulk-upload-city")
    public ResponseEntity<CommonResponse<String>> uploadCityExcel(@RequestParam("file") MultipartFile file,
                                                                  @RequestParam("uploadedBy") String uploadedBy) {
        try {
            String result = cityService.uploadCitiesFromExcel(file, uploadedBy);
            return ResponseEntity.ok(CommonResponse.success("Upload Result", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(CommonResponse.failure("Upload failed: " + e.getMessage()));
        }
    }


}

