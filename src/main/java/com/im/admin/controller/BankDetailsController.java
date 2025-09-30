package com.im.admin.controller;

import com.im.admin.entities.BanksDetailss;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/bank-details")
public class BankDetailsController {

    @Autowired
    private BankDetailsService service;

    @PostMapping
    public ResponseEntity<CommonResponse<BanksDetailss>> save(@RequestBody BanksDetailss request) {
        log.info("Saving bank details: {}", request);
        ResponseEntity<CommonResponse<BanksDetailss>> response = ResponseEntity.ok(service.save(request));
        log.info("Saved bank details response: {}", response.getBody());
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetailss>> update(@PathVariable Long id, @RequestBody BanksDetailss request) {
        log.info("Updating bank details for id: {}, request: {}", id, request);
        ResponseEntity<CommonResponse<BanksDetailss>> response = ResponseEntity.ok(service.update(id, request));
        log.info("Updated bank details response: {}", response.getBody());
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetailss>> getById(@PathVariable Long id) {
        log.info("Fetching bank details for id: {}", id);
        ResponseEntity<CommonResponse<BanksDetailss>> response = ResponseEntity.ok(service.getById(id));
        log.info("Fetched bank details response: {}", response.getBody());
        return response;
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<BanksDetailss>>> getAll(
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching all banks with hierarchyId: {}, bankName: {}, isVerified: {}, page: {}, size: {}",
                hierarchyId, bankName, isVerified, page, size);
        ResponseEntity<CommonResponse<Page<BanksDetailss>>> response =
                ResponseEntity.ok(service.getAll(hierarchyId, bankName, isVerified, page, size));
        log.info("Fetched all banks response: {}", response.getBody());
        return response;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<String>> deleteBank(@PathVariable Long id) {
        log.info("Request received to delete bank record | ID={}", id);
        CommonResponse<String> response = service.delete(id);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

}

