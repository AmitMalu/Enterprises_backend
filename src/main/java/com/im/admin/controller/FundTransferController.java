package com.im.admin.controller;


import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.FundTransferResponseDto;
import com.im.admin.service.FundTransferService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/fund-transfer")
public class FundTransferController {

    @Autowired
    private FundTransferService fundTransferService;

    @PostMapping
    public ResponseEntity<CommonResponse<FundTransferResponseDto>> transferFunds(@RequestBody FundTransferRequestDto requestDto) {
        log.info("API Call: transferFunds from hierarchyId: {} to hierarchyId: {}, amount: {}",
                requestDto.getFromHierarchyId(), requestDto.getToHierarchyId(), requestDto.getAmount());

        FundTransferResponseDto response = fundTransferService.transferFunds(requestDto);

        log.info("Fund transfer result from hierarchyId {} to hierarchyId {}: {}",
                requestDto.getFromHierarchyId(), requestDto.getToHierarchyId());

        CommonResponse<FundTransferResponseDto> commonResponse =
                CommonResponse.success(response,"Fund transfer completed successfully");
        return ResponseEntity.ok(commonResponse);
    }
}
