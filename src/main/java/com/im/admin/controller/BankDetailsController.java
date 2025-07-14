package com.im.admin.controller;

import com.im.admin.entities.BanksDetailss;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-details")
public class BankDetailsController {

    @Autowired
    private BankDetailsService service;

    @PostMapping
    public ResponseEntity<CommonResponse<BanksDetailss>> save(@RequestBody BanksDetailss request) {
        return ResponseEntity.ok(service.save(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetailss>> update(@PathVariable Long id, @RequestBody BanksDetailss request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetailss>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<BanksDetailss>>> getAll(
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.getAll(hierarchyId, bankName, isVerified, page, size));
    }
}

