package com.scs.admin.controller;


import com.scs.admin.dto.BeneficiaryBankAccountDTO;
import com.scs.admin.entities.BeneficiaryBankAccount;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.PayoutBeneficiaryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RequestMapping("/api/payout")
@Slf4j
@RestController
public class PayoutBeneficiaryController {

    @Autowired
    private PayoutBeneficiaryService payoutService;

    @PostMapping("/save-beneficiary")
    public ResponseEntity<CommonResponse<BeneficiaryBankAccount>> saveBeneficiary(@RequestBody BeneficiaryBankAccountDTO dto) {
        log.info("Received request to save beneficiary: {}", dto);
        CommonResponse<BeneficiaryBankAccount> response = payoutService.saveBeneficiary(dto);
        log.info("Beneficiary saved successfully: {}", response.getData());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    public ResponseEntity<CommonResponse<Page<BeneficiaryBankAccount>>> getAllBeneficiaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String retailerId,
            @RequestParam(required = false) String bankName,
            @RequestParam(required = false) String bankHolderName,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String ifscCode,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) Boolean isVerified,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate createdOn,
            @RequestParam(required = false) String createdBy
    ) {
        log.info("Fetching beneficiaries with filters, page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdOn"));
        CommonResponse<Page<BeneficiaryBankAccount>> response =
                payoutService.getAllFiltered(id, retailerId, bankName, bankHolderName, accountNumber,
                        ifscCode, paymentMode, isVerified, createdOn, createdBy, pageable);

        log.info("Fetched {} beneficiaries", response.getData().getTotalElements());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-beneficiaries")
    public ResponseEntity<CommonResponse<String>> deleteBeneficiaries(@RequestBody List<Long> ids) {
        log.info("Deleting beneficiaries with IDs: {}", ids);
        CommonResponse<String> response = payoutService.deleteMultipleBeneficiaries(ids);
        log.info("Deleted beneficiaries response: {}", response.getMessage());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-beneficiaries/{id}")
    public ResponseEntity<CommonResponse<BeneficiaryBankAccountDTO>> updateBeneficiary(
            @PathVariable Long id,
            @RequestBody BeneficiaryBankAccountDTO requestDto) {

        log.info("Updating BeneficiaryBankAccount with ID: {}", id);

        CommonResponse<BeneficiaryBankAccountDTO> response = payoutService.updateBeneficiary(id, requestDto);

        log.info("BeneficiaryBankAccount updated successfully for ID: {}", id);
        return ResponseEntity.ok(response);
    }

}
