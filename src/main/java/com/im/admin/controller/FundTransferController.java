package com.im.admin.controller;


import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.FundTransferResponseDto;
import com.im.admin.service.FundTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fund-transfer")
public class FundTransferController {

    @Autowired
    private FundTransferService fundTransferService;

    @PostMapping
    public ResponseEntity<FundTransferResponseDto> transferFunds(@RequestBody FundTransferRequestDto requestDto) {
        FundTransferResponseDto response = fundTransferService.transferFunds(requestDto);
        return ResponseEntity.ok(response);
    }

}
