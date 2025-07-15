package com.im.admin.controller;

import com.im.admin.entities.ImBankMaster;
import com.im.admin.request.ImBankMasterRequestDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.ImBankDropdownResponse;
import com.im.admin.response.ImBankMasterResponseDTO;
import com.im.admin.service.ImBankMasterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/im-bank")
public class ImBankMasterController {

    @Autowired
    private ImBankMasterService service;

    @PostMapping
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> create(@RequestBody ImBankMasterRequestDTO dto) {
        return ResponseEntity.ok(service.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> update(
            @PathVariable Long id,
            @RequestBody ImBankMasterRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<ImBankMasterResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<ImBankMaster>>> getAll(
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) Boolean charges,
            @RequestParam(required = false) String chargesType,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String createdBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(service.getAll(bankName, accountNumber, ifscCode, charges,
                chargesType, isActive, isBlocked, createdBy, page, size));
    }

    @GetMapping("/dropdown")
    public ResponseEntity<CommonResponse<List<ImBankDropdownResponse>>> getBankDropdown() {
        return ResponseEntity.ok(service.getDropdown());
    }

}

