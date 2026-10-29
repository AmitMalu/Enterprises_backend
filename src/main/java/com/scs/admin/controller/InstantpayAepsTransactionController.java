package com.scs.admin.controller;

import com.scs.admin.request.InstantPayCashWithdrawalRequestDto;
import com.scs.admin.request.InstantpayBalanceEnquiryRequestDto;
import com.scs.admin.request.InstantpayMiniStatementRequestDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.InstantpayBalanceEnquiryResponseDto;
import com.scs.admin.response.InstantpayCashWithdrawalResponseDto;
import com.scs.admin.response.InstantpayMiniStatementResponseDto;
import com.scs.admin.service.InstantpayAepsTransactionService;
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
