package com.im.admin.controller;

import com.im.admin.dto.StateDTO;
import com.im.admin.dto.StateRequestDTO;
import com.im.admin.dto.StateResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.StateService;
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

@RestController
@Slf4j
@RequestMapping("/api/states")
public class StateController {

    @Autowired
    private StateService stateService;

    @PostMapping
    public ResponseEntity<CommonResponse<StateResponseDTO>> create(@RequestBody StateRequestDTO dto) {
        log.info("Received request to create state: {}", dto);
        try {
            CommonResponse<StateResponseDTO> response = stateService.save(dto);
            log.info("State created successfully: {}", response.getData());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error creating state", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to create state"));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<StateResponseDTO>> update(@PathVariable Long id, @RequestBody StateRequestDTO dto) {
        log.info("Updating state with ID: {}", id);
        try {
            CommonResponse<StateResponseDTO> response = stateService.update(id, dto);
            log.info("State updated successfully for ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating state with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to update state"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<StateResponseDTO>> getById(@PathVariable Long id) {
        log.info("Fetching state by ID: {}", id);
        try {
            CommonResponse<StateResponseDTO> response = stateService.getById(id);
            log.info("State fetched successfully for ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching state with ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to fetch state"));
        }
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
        log.info("Fetching states with filters - page: {}, size: {}, stateName: {}, stateCode: {}", page, size, stateName, stateCode);
        try {
            Page<StateResponseDTO> result = stateService.getAllStates(page, size, zone, stateName, stateCode, regionType, countryId, blocked);
            log.info("Fetched {} states", result.getTotalElements());
            return ResponseEntity.ok(CommonResponse.success(result, "States fetched"));
        } catch (Exception e) {
            log.error("Error fetching states", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to fetch states"));
        }
    }

    @GetMapping("/by-country/{countryId}")
    public ResponseEntity<CommonResponse<List<StateDTO>>> getStatesByCountry(@PathVariable Long countryId) {
        log.info("Fetching states by country ID: {}", countryId);
        try {
            List<StateDTO> states = stateService.getStatesByCountry(countryId);
            log.info("Fetched {} states for country ID: {}", states.size(), countryId);
            return ResponseEntity.ok(CommonResponse.success(states, "States loaded successfully"));
        } catch (Exception e) {
            log.error("Error fetching states for country ID: {}", countryId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to fetch states by country"));
        }
    }

    @GetMapping("/template")
    public ResponseEntity<Resource> downloadTemplate() throws IOException {
        log.info("Downloading sample state upload template");
        ByteArrayResource resource = stateService.generateSampleTemplate();
        log.info("Template generated successfully");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sample_state_upload_template.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @PostMapping("/bulk-upload")
    public ResponseEntity<Resource> uploadStateExcel(@RequestParam("file") MultipartFile file,
                                                     @RequestParam("createdBy") String createdBy) {
        log.info("Bulk uploading states from Excel by user: {}", createdBy);
        try {
            ByteArrayResource processedFile = stateService.saveStatesFromExcel(file, createdBy);
            log.info("Bulk upload completed successfully");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=state_upload_result.xlsx")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(processedFile);
        } catch (Exception e) {
            log.error("Error during bulk upload", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }



}
