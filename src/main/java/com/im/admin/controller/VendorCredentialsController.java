package com.im.admin.controller;

import com.im.admin.dto.VendorCredentialsDTO;
import com.im.admin.dto.VendorCredentialsResponseDTO;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorCredentialsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor-credentials")
@Slf4j
public class VendorCredentialsController {

    @Autowired
    private VendorCredentialsService vendorCredentialsService;

    @PostMapping
    public ResponseEntity<CommonResponse<VendorCredentials>> create(@RequestBody VendorCredentialsDTO dto) {
        log.info("Received request to create VendorCredentials: {}", dto);
        VendorCredentials saved = vendorCredentialsService.saveVendorCredentials(dto);
        log.debug("VendorCredentials created: {}", saved);
        return ResponseEntity.ok(CommonResponse.success(saved, "Vendor credentials saved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorCredentialsResponseDTO>> update(
            @PathVariable Long id, @RequestBody VendorCredentialsDTO dto) {
        log.info("Received request to update VendorCredentials with ID: {}", id);
        log.debug("Update payload: {}", dto);
        VendorCredentialsResponseDTO updated = vendorCredentialsService.updateVendorCredentials(id, dto);
        log.debug("VendorCredentials updated: {}", updated);
        return ResponseEntity.ok(CommonResponse.success(updated, "Vendor credentials updated successfully"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<VendorCredentialsResponseDTO>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String vendorName,
            @RequestParam(required = false) String whiteLabelAdminName,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate editedOn) {

        log.info("Fetching VendorCredentials with filters - productName: {}, vendorName: {}, whiteLabelAdminName: {}, username: {}, isActive: {}, createdBy: {}, page: {}, size: {}",
                productName, vendorName, whiteLabelAdminName, username, isActive, createdBy, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<VendorCredentialsResponseDTO> result = vendorCredentialsService.getAllVendorCredentials(
                productName, vendorName, whiteLabelAdminName, username, isActive, createdBy,editedBy, createdOn, editedOn, pageable);

        log.debug("Fetched VendorCredentials page size: {}", result.getSize());
        return ResponseEntity.ok(CommonResponse.success(result, "Vendor credentials fetched successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorCredentialsResponseDTO>> getById(@PathVariable Long id) {
        log.info("Fetching VendorCredentials by ID: {}", id);
        VendorCredentialsResponseDTO data = vendorCredentialsService.getVendorCredentialsById(id);
        log.debug("Fetched VendorCredentials: {}", data);
        return ResponseEntity.ok(CommonResponse.success(data, "Vendor credentials fetched successfully"));
    }

}
