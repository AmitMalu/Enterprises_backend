package com.im.admin.controller;

import com.im.admin.dto.CityDTO;
import com.im.admin.dto.CityRequestDTO;
import com.im.admin.dto.CityResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.CityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

