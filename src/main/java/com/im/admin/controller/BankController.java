package com.im.admin.controller;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import com.im.admin.entities.SubProducts;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banks")
public class BankController {

    @Autowired
    private BankService bankService;

    @PostMapping("/add")
    public ResponseEntity<CommonResponse<BankMaster>> addBank(@RequestBody BankRequestDto bankRequestDto) {

        try {
            BankMaster saveBank = bankService.addBank(bankRequestDto);
            CommonResponse<BankMaster> response = new CommonResponse<>(201, "BankMaster created successfully", saveBank);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (IllegalArgumentException ex) {
            CommonResponse<BankMaster> response = new CommonResponse<>(400, ex.getMessage(), null);
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<Page<BankMaster>>> getAllBanks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,

            @RequestParam(required = false) String id,
            @RequestParam(required = false) String bankCode,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) String imps,
            @RequestParam(required = false) String neft,
            @RequestParam(required = false) String rtgs,
            @RequestParam(required = false) String activeStatus,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String editedBy,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate editedOn
    ) {

        Pageable pageable = PageRequest.of(page, size);
        Page<BankMaster> banks = bankService.getAllBanks(
                pageable, id, bankCode, bankName, ifscCode, imps, neft, rtgs,
                activeStatus, createdBy, editedBy, isBlocked, createdOn, editedOn
        );

        CommonResponse<Page<BankMaster>> response = new CommonResponse<>(
                200,
                "BankMaster fetched successfully",
                banks
        );
        return ResponseEntity.ok(response);
    }


    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse> updateBank(@PathVariable Long id,@RequestBody BankRequestDto bankRequestDto){
        log.info("Received request to update BankMaster with ID: {}", id);
        BankMaster UpdateBankMaster = bankService.updateBank(id, bankRequestDto);

        return ResponseEntity.ok(
                new CommonResponse<>(200, "BankMaster updated successfully", UpdateBankMaster));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<BankMaster>> getBankById(@PathVariable Long id) {
        log.info("Received request to fetch bank with ID: {}", id);

        BankMaster bank = bankService.getBankById(id);
        CommonResponse<BankMaster> response = new CommonResponse<>(
                200, "Bank fetched successfully", bank
        );
        return ResponseEntity.ok(response);
    }

}
