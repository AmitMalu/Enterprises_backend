package com.im.admin.controller;

import com.im.admin.dto.ProductLimitResponseDto;
import com.im.admin.dto.ProductTransactionLimitDTO;
import com.im.admin.entities.ProductTransactionLimit;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.ProductTransactionLimitService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-transaction-limit")
public class ProductTransactionLimitController {

    @Autowired
    private ProductTransactionLimitService service;

    @PostMapping("/save")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> save(@RequestBody ProductTransactionLimitDTO dto) {
        return ResponseEntity.ok(service.save(dto));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> update(
            @PathVariable Long id,
            @RequestBody ProductTransactionLimitDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ProductLimitResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/get-all")
    public ResponseEntity<CommonResponse<Page<ProductLimitResponseDto>>> getAllPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.getAll(page, size));
    }
}

