package com.im.admin.controller;

import com.im.admin.entities.ImBankMaster;
import com.im.admin.request.ImBankMasterRequestDTO;
import com.im.admin.response.BankResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.ImBankDropdownResponse;
import com.im.admin.response.ImBankMasterResponseDTO;
import com.im.admin.service.ImBankMasterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/im-bank")
public class ImBankMasterController {

    @Autowired
    private ImBankMasterService service;

    @PostMapping
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> create(@RequestBody ImBankMasterRequestDTO dto) {
        log.info("API Call: create ImBankMaster with request: {}", dto);
        CommonResponse<ImBankMasterResponseDTO> response = service.create(dto);
        log.info("ImBankMaster created successfully");
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody ImBankMasterRequestDTO dto) {
        log.info("API Call: update ImBankMaster with id: {} and request: {}", id, dto);
        CommonResponse<ImBankMasterResponseDTO> response = service.update(id, dto);
        log.info("ImBankMaster updated successfully for id: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> getById(@PathVariable Long id) {
        log.info("API Call: getById ImBankMaster for id: {}", id);
        CommonResponse<ImBankMasterResponseDTO> response = service.getById(id);
        log.info("Fetched ImBankMaster successfully for id: {}", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<ImBankMaster>>> getAll(
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
        log.info("API Call: getAll ImBankMaster with filters bankName: {}, accountNumber: {}, ifscCode: {}, charges: {}, chargesType: {}, isActive: {}, isBlocked: {}, createdBy: {}, page: {}, size: {}",
                bankName, accountNumber, ifscCode, charges, chargesType, isActive, isBlocked, createdBy, page, size);
        CommonResponse<Page<ImBankMaster>> response = service.getAll(id,bankName, accountNumber, ifscCode, charges,
                chargesType, isActive, isBlocked, createdBy, editedOn, page, size);
        log.info("Fetched ImBankMaster list successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<ImBankDropdownResponse>>> getBankDropdown() {
        log.info("API Call: getBankDropdown");
        CommonResponse<List<ImBankDropdownResponse>> response = service.getDropdown();
        log.info("Fetched ImBank dropdown successfully");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/list")
    public ResponseEntity<CommonResponse<List<BankResponseDto>>> getBankDetails() {
        List<BankResponseDto> responseDtoList = service.getBankDetails();
        return ResponseEntity.ok(CommonResponse.success(responseDtoList, "Record fetch successfully.."));
    }

}

