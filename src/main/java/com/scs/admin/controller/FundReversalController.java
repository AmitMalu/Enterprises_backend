package com.scs.admin.controller;

import com.scs.admin.dto.CheckBalanceRequestDto;
import com.scs.admin.dto.FundReversalRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.FundReversalResponseDto;
import com.scs.admin.service.FundReversalService;
import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<CommonResponse<FundReversalResponseDto>> takeActionOnFundReversal(@RequestBody FundReversalRequestDto request, HttpServletRequest httpServletRequest) {
        RequestHeaderMetaDto requestHeaderMetaDtoMeta = RequestHeaderMetaDto.from(httpServletRequest);
        CommonResponse<FundReversalResponseDto> response = fundReversalService.takeActionOnFundReversal(request, requestHeaderMetaDtoMeta);
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
