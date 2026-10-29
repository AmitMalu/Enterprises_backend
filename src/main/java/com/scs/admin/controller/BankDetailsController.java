package com.scs.admin.controller;

import com.scs.admin.dto.BankDetailsResponseDto;
import com.scs.admin.entities.BanksDetails;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.BankDetailsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/bank-details")
public class BankDetailsController {

    @Autowired
    private BankDetailsService service;

    @PostMapping
    public ResponseEntity<CommonResponse<BanksDetails>> save(@RequestBody BanksDetails request) {
        log.info("Saving bank details: {}", request);
        ResponseEntity<CommonResponse<BanksDetails>> response = ResponseEntity.ok(service.save(request));
        log.info("Saved bank details response: {}", response.getBody());
        return response;
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetails>> update(@PathVariable Long id, @RequestBody BanksDetails request) {
        log.info("Updating bank details for id: {}, request: {}", id, request);
        ResponseEntity<CommonResponse<BanksDetails>> response = ResponseEntity.ok(service.update(id, request));
        log.info("Updated bank details response: {}", response.getBody());
        return response;
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<BanksDetails>> getById(@PathVariable Long id) {
        log.info("Fetching bank details for id: {}", id);
        ResponseEntity<CommonResponse<BanksDetails>> response = ResponseEntity.ok(service.getById(id));
        log.info("Fetched bank details response: {}", response.getBody());
        return response;
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<BanksDetails>>> getAll(
            @RequestParam(required = false) String hierarchyId,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("Fetching all banks with hierarchyId: {}, bankName: {}, isVerified: {}, page: {}, size: {}",
                hierarchyId, bankName, isVerified, page, size);
        ResponseEntity<CommonResponse<Page<BanksDetails>>> response =
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

    @GetMapping("/verified/{hierarchyId}")
    public ResponseEntity<CommonResponse<List<BankDetailsResponseDto>>> getVerifiedBanks(
            @PathVariable String hierarchyId) {
        CommonResponse<List<BankDetailsResponseDto>> response = service.getVerifiedBankDetails(hierarchyId);
        return ResponseEntity.ok(response);
    }

}

