package com.im.admin.controller;


import com.im.admin.dto.ChildUserDto;
import com.im.admin.dto.FundRequestActionDto;
import com.im.admin.dto.FundRequestDto;
import com.im.admin.entities.FundRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.FundRequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/fund-request")
@Slf4j
@Validated
public class FundRequestController {

    @Autowired
    private FundRequestService fundRequestService;

    @PostMapping("/raise")
    public ResponseEntity<CommonResponse<String>> raiseFundRequest(@RequestBody FundRequestDto fundRequestDto) {
        log.info("API Call: raiseFundRequest with data: {}", fundRequestDto);
        fundRequestService.raiseFundRequest(fundRequestDto);
        log.info("Fund request raised successfully for: {}", fundRequestDto.getHierarchyId());
        return ResponseEntity.ok(new CommonResponse<>(200, "Fund request raised successfully", null));
    }

// @PostMapping("/action")
// public ResponseEntity<CommonResponse<String>> takeActionOnFundRequest(@RequestBody FundRequestActionDto actionDto) {
//     log.info("API Call: takeActionOnFundRequest with data: {}", actionDto);
//     fundRequestService.takeActionOnFundRequest(actionDto);
//     log.info("Fund request action processed successfully for requestId: {}", actionDto.getRequestId());
//     return ResponseEntity.ok(new CommonResponse<>(200, "Fund request action processed successfully", null));
// }

    @GetMapping("/get-all")
    public ResponseEntity<CommonResponse<List<FundRequest>>> getAllRecords() {
        log.info("API Call: getAllRecords");
        List<FundRequest> allRequests = fundRequestService.fetchAllRecords();

        if (allRequests.isEmpty()) {
            log.info("No fund requests found");
            return ResponseEntity.ok(new CommonResponse<>(200, "No fund requests found", Collections.emptyList()));
        }

        log.info("Fund requests fetched successfully. Total records: {}", allRequests.size());
        return ResponseEntity.ok(new CommonResponse<>(200, "Fund requests fetched successfully", allRequests));
    }
}
