package com.im.admin.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import com.im.admin.service.PayoutService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/payout")
@Slf4j
@RestController
public class PayoutController {

    @Autowired
    private PayoutService payoutService;

    LocalDateTime currentDateTimeDate = LocalDateTime.now();

    @PostMapping("/generate-token")
    public ResponseEntity<String> generateToken() {
        log.info("******* generateToken at {} started *******", currentDateTimeDate);
        String token = payoutService.generateToken();
        log.info("******* generateToken at {} ends *******", currentDateTimeDate);
        return new ResponseEntity<>(token, HttpStatus.OK);
    }

    @PostMapping("/send-payout-request")
    public ResponseEntity<CommonResponse<PayoutResponseDto>> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");

        try {
            CommonResponse<PayoutResponseDto> response = payoutService.sendPayoutRequest(sendPayoutRequest);
            log.info("======= sendPayoutRequest completed at {} =======", LocalDateTime.now());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            log.error("Error during payout request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to process payout request", null));
        }
    }

    @PostMapping("/update-callback-url")
    public ResponseEntity<CommonResponse<PayoutMerchantCallbackResponse>> updateCallbackUrl(@RequestBody SendPayoutRequest request) {
        PayoutMerchantCallbackResponse responseDTO = payoutService.updateMerchantCallbackUrl(request);
        return ResponseEntity.ok(new CommonResponse<>(200, "Callback URL updated", responseDTO));
    }

    @PostMapping("/callback")
    public ResponseEntity<String> payoutCallback(@RequestBody Map<String, String> payload, HttpServletRequest request) throws JsonProcessingException {
        log.info("Received callback request with payload: {}", payload);
        String response = payoutService.handlePayoutCallback(payload, request);
        log.info("Callback response: {}", response);
        return ResponseEntity.ok(response);
    }

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
