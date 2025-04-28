package com.im.admin.controller;


import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.FundRequestDto;
import com.im.admin.service.FundRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fund-request")
@Slf4j
@Validated
public class FundRequestController {

    @Autowired
    private FundRequestService fundRequestService;

    @PostMapping("/raise")
    public ResponseEntity<String> raiseFundRequest(@RequestBody FundRequestDto fundRequestDto) {
        fundRequestService.raiseFundRequest(fundRequestDto);
        return ResponseEntity.ok("Fund request raised successfully");
    }

    @PostMapping("/action")
    public ResponseEntity<String> takeActionOnFundRequest(@RequestBody FundRequestActionDto actionDto) {
        fundRequestService.takeActionOnFundRequest(actionDto);
        return ResponseEntity.ok("Fund request action processed successfully");
    }
}
