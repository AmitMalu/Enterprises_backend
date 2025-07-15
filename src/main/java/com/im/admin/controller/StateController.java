package com.im.admin.controller;

import com.im.admin.dto.StateDTO;
import com.im.admin.dto.StateRequestDTO;
import com.im.admin.dto.StateResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.StateService;
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
@RequestMapping("/api/states")
public class StateController {

    @Autowired
    private StateService stateService;

    @PostMapping
    public ResponseEntity<CommonResponse<StateResponseDTO>> create(@RequestBody StateRequestDTO dto) {
        return ResponseEntity.ok(stateService.save(dto));
    }


    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<StateResponseDTO>> update(@PathVariable Long id, @RequestBody StateRequestDTO dto) {
        return ResponseEntity.ok(stateService.update(id, dto));
    }


    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<StateResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stateService.getById(id));
    }


    @GetMapping
    public ResponseEntity<CommonResponse<Page<StateResponseDTO>>> getAllStates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String stateName,
            @RequestParam(required = false) String stateCode,
            @RequestParam(required = false) String regionType,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Boolean blocked
    ) {
        Page<StateResponseDTO> result = stateService.getAllStates(page, size, zone, stateName, stateCode, regionType, countryId, blocked);
        return ResponseEntity.ok(CommonResponse.success(result, "States fetched"));
    }


    @GetMapping("/by-country/{countryId}")
    public ResponseEntity<CommonResponse<List<StateDTO>>> getStatesByCountry(@PathVariable Long countryId) {
        List<StateDTO> states = stateService.getStatesByCountry(countryId);
        return ResponseEntity.ok(CommonResponse.success(states, "States loaded successfully"));
    }

    // API 1: Download sample template
    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        ByteArrayResource resource = stateService.generateSampleTemplate();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_state_upload_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Resource> uploadStateExcel(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("createdBy") String createdBy) {
        ByteArrayResource processedFile = stateService.saveStatesFromExcel(file, createdBy);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=state_upload_result.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(processedFile);
    }


}
