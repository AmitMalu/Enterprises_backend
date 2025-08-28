package com.im.admin.controller;

import com.im.admin.dto.ProductLimitResponseDto;
import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductTransactionLimitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/product-transaction-limit")
public class ProductTransactionLimitController {

    @Autowired
    private ProductTransactionLimitService service;

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> save(@RequestBody ProductTransactionLimitDTO dto) {
        log.info("Received request to save Product Transaction Limit: {}", dto);
        CommonResponse<ProductLimitResponseDto> response = service.save(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> update(
            @PathVariable Long id,
            @RequestBody ProductTransactionLimitDTO dto) {
        log.info("Received request to update Product Transaction Limit with ID: {}", id);
        CommonResponse<ProductLimitResponseDto> response = service.update(id, dto);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> getById(@PathVariable Long id) {
        log.info("Fetching Product Transaction Limit with ID: {}", id);
        CommonResponse<ProductLimitResponseDto> response = service.getById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/get-all")
    public ResponseEntity<CommonResponse<Page<ProductLimitResponseDto>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching all Product Transaction Limits - Page: {}, Size: {}", page, size);
        CommonResponse<Page<ProductLimitResponseDto>> response = service.getAll(page, size);
        return ResponseEntity.ok(response);
    }
}

