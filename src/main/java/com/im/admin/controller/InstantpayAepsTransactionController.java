package com.im.admin.controller;

import com.im.admin.request.InstantPayCashWithdrawalRequestDto;
import com.im.admin.request.InstantpayBalanceEnquiryRequestDto;
import com.im.admin.request.InstantpayMiniStatementRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.InstantpayBalanceEnquiryResponseDto;
import com.im.admin.response.InstantpayCashWithdrawalResponseDto;
import com.im.admin.response.InstantpayMiniStatementResponseDto;
import com.im.admin.service.InstantpayAepsTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/instantpay/aeps")
@RequiredArgsConstructor
public class InstantpayAepsTransactionController {

    @Autowired
    private InstantpayAepsTransactionService instantpayAepsTransactionService;

    @PostMapping("/cash-withdrawal")
    public ResponseEntity<CommonResponse<InstantpayCashWithdrawalResponseDto>> cashWithdrawal(
            @RequestBody InstantPayCashWithdrawalRequestDto request,
            @RequestHeader Double latitude, @RequestHeader Double longitude) {

        CommonResponse<InstantpayCashWithdrawalResponseDto> response =
                instantpayAepsTransactionService.cashWithdrawal(request,latitude,longitude);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/balance-enquiry")
    public CommonResponse<InstantpayBalanceEnquiryResponseDto> balanceEnquiry(
            @RequestBody InstantpayBalanceEnquiryRequestDto req,@RequestHeader Double latitude, @RequestHeader Double longitude) {

        return instantpayAepsTransactionService.balanceEnquiry(req,latitude,longitude);

    }

    @PostMapping("/mini-statement")
    public ResponseEntity<CommonResponse<InstantpayMiniStatementResponseDto>> miniStatement(
            @RequestBody InstantpayMiniStatementRequestDto req,@RequestHeader Double latitude, @RequestHeader Double longitude) {

        CommonResponse<InstantpayMiniStatementResponseDto> response =
                instantpayAepsTransactionService.miniStatement(req,latitude,longitude);

        return ResponseEntity.ok(response);
    }

}
