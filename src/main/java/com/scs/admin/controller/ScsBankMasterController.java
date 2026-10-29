package com.scs.admin.controller;

import com.scs.admin.entities.ScsBankMaster;
import com.scs.admin.request.ScsBankMasterRequestDTO;
import com.scs.admin.response.BankResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.ScsBankDropdownResponse;
import com.scs.admin.response.ScsBankMasterResponseDTO;
import com.scs.admin.service.ScsBankMasterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/scs-bank")
public class ScsBankMasterController {

    @Autowired
    private ScsBankMasterService service;

    @PostMapping
    public ResponseEntity<CommonResponse<ScsBankMasterResponseDTO>> create(@RequestBody ScsBankMasterRequestDTO dto) {
        log.info("API Call: create ScsBankMaster with request: {}", dto);
        CommonResponse<ScsBankMasterResponseDTO> response = service.create(dto);
        log.info("ScsBankMaster created successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ScsBankMasterResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody ScsBankMasterRequestDTO dto) {
        log.info("API Call: update ScsBankMaster with id: {} and request: {}", id, dto);
        CommonResponse<ScsBankMasterResponseDTO> response = service.update(id, dto);
        log.info("ScsBankMaster updated successfully for id: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ScsBankMasterResponseDTO>> getById(@PathVariable Long id) {
        log.info("API Call: getById ScsBankMaster for id: {}", id);
        CommonResponse<ScsBankMasterResponseDTO> response = service.getById(id);
        log.info("Fetched ScsBankMaster successfully for id: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<ScsBankMaster>>> getAll(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) Boolean charges,
            @RequestParam(required = false) String chargesType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedOn,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("API Call: getAll ScsBankMaster with filters bankName: {}, accountNumber: {}, ifscCode: {}, charges: {}, chargesType: {}, isActive: {}, isBlocked: {}, createdBy: {}, page: {}, size: {}",
                bankName, accountNumber, ifscCode, charges, chargesType, isActive, isBlocked, createdBy, page, size);
        CommonResponse<Page<ScsBankMaster>> response = service.getAll(id,bankName, accountNumber, ifscCode, charges,
                chargesType, isActive, isBlocked, createdBy, editedOn, page, size);
        log.info("Fetched ScsBankMaster list successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<ScsBankDropdownResponse>>> getBankDropdown() {
        log.info("API Call: getBankDropdown");
        CommonResponse<List<ScsBankDropdownResponse>> response = service.getDropdown();
        log.info("Fetched ScsBank dropdown successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<CommonResponse<List<BankResponseDto>>> getBankDetails() {
        List<BankResponseDto> responseDtoList = service.getBankDetails();
        return ResponseEntity.ok(CommonResponse.success(responseDtoList, "Record fetch successfully.."));
    }

    @PostMapping("list")
    public ResponseEntity<CommonResponse<List<BankResponseDto>>> getWhiteLabelBankDetails() {
        List<BankResponseDto> responseDtoList = service.getWhiteLabelBankDetails();
        return ResponseEntity.ok(CommonResponse.success(responseDtoList, "Record fetch successfully.."));
    }

}

