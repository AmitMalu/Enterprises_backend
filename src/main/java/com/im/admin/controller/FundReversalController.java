package com.im.admin.controller;

import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.FundReversalResponseDto;
import com.im.admin.service.FundReversalService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fund-reversal")
@Slf4j
public class FundReversalController {

    @Autowired
    private FundReversalService fundReversalService;

    @PostMapping("/check-balance")
    public ResponseEntity<String> checkHierarchyWalletBalance(@RequestBody CheckBalanceRequestDto checkBalanceRequestDto) {
        log.info("API Call: checkHierarchyWalletBalance for hierarchyId: {}", checkBalanceRequestDto.getHierarchyId());
        String message = fundReversalService.checkBalance(checkBalanceRequestDto);
        log.info("Balance check result for hierarchyId {}: {}", checkBalanceRequestDto.getHierarchyId(), message);
        return ResponseEntity.ok(message);
    }

    @PostMapping
    public ResponseEntity<CommonResponse<FundReversalResponseDto>> takeActionOnFundReversal(@RequestBody FundReversalRequestDto request) {
        CommonResponse<FundReversalResponseDto> response = fundReversalService.takeActionOnFundReversal(request);
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }

    @PostMapping("/resend-otp/{id}")
    public ResponseEntity<CommonResponse<FundReversalResponseDto>> resendOtp(@PathVariable Long id) {
        log.info("API Call: resendOtp for fundReversalId: {}", id);
        CommonResponse<FundReversalResponseDto> response = fundReversalService.resendOtpById(id);
        log.info("OTP resent for fundReversalId {}: {}", id, response.getMessage());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }
}
