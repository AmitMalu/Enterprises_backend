package com.im.admin.controller;


import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DistrictService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

}

