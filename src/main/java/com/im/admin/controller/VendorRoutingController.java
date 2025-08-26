package com.im.admin.controller;

import com.im.admin.dto.VendorRoutingRequestDTO;
import com.im.admin.dto.VendorRoutingResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VendorRoutingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class VendorRoutingController {

    @Autowired
    private VendorRoutingService vendorRoutingService;

    @PostMapping
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> save(@RequestBody VendorRoutingRequestDTO request) {
        log.info("Received request to save VendorRouting: {}", request);
        CommonResponse<VendorRoutingResponseDTO> response = vendorRoutingService.save(request);
        log.debug("VendorRouting saved: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> update(@PathVariable Long id,
                                                                           @RequestBody VendorRoutingRequestDTO request) {
        log.info("Received request to update VendorRouting with ID: {}", id);
        log.debug("Update payload: {}", request);
        CommonResponse<VendorRoutingResponseDTO> response = vendorRoutingService.update(id, request);
        log.debug("VendorRouting updated: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<VendorRoutingResponseDTO>> getById(@PathVariable Long id) {
        log.info("Fetching VendorRouting by ID: {}", id);
        CommonResponse<VendorRoutingResponseDTO> response = vendorRoutingService.getById(id);
        log.debug("Fetched VendorRouting: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<VendorRoutingResponseDTO>>> getAll(
            @RequestParam(required = false) String subProductId,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Fetching all VendorRouting records with filters - subProductId: {}, createdBy: {}, page: {}, size: {}",
                subProductId, createdBy, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        CommonResponse<Page<VendorRoutingResponseDTO>> response = vendorRoutingService.getAll(subProductId, createdBy, pageable);
        log.debug("Fetched VendorRouting page size: {}", response.getData().getSize());

        return ResponseEntity.ok(response);
    }

}

