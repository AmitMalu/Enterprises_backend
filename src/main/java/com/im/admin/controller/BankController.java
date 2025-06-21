package com.im.admin.controller;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.BankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/banks")
public class BankController {

    @Autowired
    private BankService bankService;

    @PostMapping("/add")
    public ResponseEntity<BankMaster> addBank(@RequestBody BankRequestDto bankRequestDto) {
        BankMaster savedBank = bankService.addBank(bankRequestDto);
        return ResponseEntity.ok(savedBank);
    }

    @GetMapping("/all")
    public ResponseEntity<List<BankMaster>> getAllBanks() {
        List<BankMaster> banks = bankService.getAllBanks();
        return ResponseEntity.ok(banks);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<CommonResponse> updateBank(@PathVariable Long id,@RequestBody BankRequestDto bankRequestDto){
        log.info("Received request to update BankMaster with ID: {}", id);
        BankMaster UpdateBankMaster = bankService.updateBank(id, bankRequestDto);

        return ResponseEntity.ok(
                new CommonResponse<>(200, "BankMaster updated successfully", UpdateBankMaster));
    }

}
