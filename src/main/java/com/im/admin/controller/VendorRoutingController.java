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

import java.util.Map;

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

    /**
     * Get all VendorRouting records with dynamic filters, pagination, sorting.
     * Example: /api/vendor-routing?subProductId=123&createdBy=admin&page=0&size=10&sort=id,desc
     */
    @GetMapping
    public ResponseEntity<CommonResponse<Page<VendorRoutingResponseDTO>>> getAll(
            @RequestParam Map<String, String> allParams,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 ? Sort.Direction.fromString(sort[1]) : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));

        log.info("Fetching VendorRouting with filters: {}, page: {}, size: {}", allParams, page, size);
        CommonResponse<Page<VendorRoutingResponseDTO>> response = vendorRoutingService.getAll(allParams, pageable);

        return ResponseEntity.ok(response);
    }

}

