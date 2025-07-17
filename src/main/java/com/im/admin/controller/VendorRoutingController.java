package com.im.admin.controller;

import com.im.admin.dto.VendorRoutingRequestDTO;
import com.im.admin.dto.VendorRoutingResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorRoutingService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor-routing")
@RequiredArgsConstructor
public class VendorRoutingController {

    @Autowired
    private VendorRoutingService vendorRoutingService;

    @PostMapping
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> save(@RequestBody VendorRoutingRequestDTO request) {
        return ResponseEntity.ok(vendorRoutingService.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> update(@PathVariable Long id,
                                                                           @RequestBody VendorRoutingRequestDTO request) {
        return ResponseEntity.ok(vendorRoutingService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorRoutingService.getById(id));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<VendorRoutingResponseDTO>>> getAll(
            @RequestParam(required = false) String subProductId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(vendorRoutingService.getAll(subProductId, createdBy, pageable));
    }
}

