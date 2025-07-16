package com.im.admin.controller;

import com.im.admin.dto.VendorCredentialsDTO;
import com.im.admin.dto.VendorCredentialsResponseDTO;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorCredentialsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/vendor-credentials")
public class VendorCredentialsController {

    @Autowired
    private VendorCredentialsService vendorCredentialsService;

    @PostMapping
    public ResponseEntity<CommonResponse<VendorCredentials>> create(@RequestBody VendorCredentialsDTO dto) {
        VendorCredentials saved = vendorCredentialsService.saveVendorCredentials(dto);
        return ResponseEntity.ok(CommonResponse.success(saved, "Vendor credentials saved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorCredentials>> update(
            @PathVariable Long id, @RequestBody VendorCredentialsDTO dto) {
        VendorCredentials updated = vendorCredentialsService.updateVendorCredentials(id, dto);
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
            @RequestParam(required = false) String createdBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        Page<VendorCredentialsResponseDTO> result = vendorCredentialsService.getAllVendorCredentials(
                productName, vendorName, whiteLabelAdminName, username, isActive, createdBy, pageable);

        return ResponseEntity.ok(CommonResponse.success(result, "Vendor credentials fetched successfully"));
    }



    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorCredentials>> getById(@PathVariable Long id) {
        VendorCredentials data = vendorCredentialsService.getVendorCredentialsById(id);
        return ResponseEntity.ok(CommonResponse.success(data, "Vendor credentials fetched successfully"));
    }
}
