package com.im.admin.controller;


import com.im.admin.dto.FundCreditDebitDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.FundingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/funding")
public class FundingController {

    @Autowired
    private FundingService fundingService;

    @PostMapping("/credit-debit")
    public ResponseEntity<CommonResponse<String>> fundsCreditDebit(@RequestBody FundCreditDebitDto dto) {
        String result = fundingService.fundsCreditDebit(dto);
        return ResponseEntity.ok(CommonResponse.success(result, "Transaction processed successfully"));
    }

}
