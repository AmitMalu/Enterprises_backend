package com.im.admin.controller;

import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RequestMapping("/api")
@Slf4j
@RestController
public class LatestPayoutController {

    @Autowired
    private PayoutOrchestrator payoutOrchestrator;


    @PostMapping("/send-payout")
    public ResponseEntity<CommonResponse<PayoutResponseDto>> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");

        try {
            CommonResponse<PayoutResponseDto> response = payoutOrchestrator.processPayout(sendPayoutRequest);
            log.info("======= sendPayoutRequest completed at {} =======", LocalDateTime.now());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            log.error("Error during payout request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to process payout request", null));
        }
    }

}
