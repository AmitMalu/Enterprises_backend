package com.im.admin.controller;

import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
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
        String message = fundReversalService.checkBalance(checkBalanceRequestDto);
        return ResponseEntity.ok(message);

    }

    @PostMapping
    public ResponseEntity<String> takeActionOnFundReversal(@RequestBody FundReversalRequestDto request) {
        String result = fundReversalService.takeActionOnFundReversal(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/fund-reversal/resend-otp/{id}")
    public ResponseEntity<String> resendOtp(@PathVariable Long id) {
        String result = fundReversalService.resendOtpById(id);
        return ResponseEntity.ok(result);
    }
}
