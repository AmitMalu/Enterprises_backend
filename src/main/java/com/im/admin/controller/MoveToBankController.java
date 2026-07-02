package com.im.admin.controller;

import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RequestMapping("/api/move-to-bank")
@RestController
@Slf4j
public class MoveToBankController {

    @Autowired
    private PayoutOrchestrator payoutOrchestrator;

    @PostMapping("/send-payout-request-VO")
    public ResponseEntity<CommonResponse<PayoutResponseDto>> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest, @RequestHeader Double latitude, @RequestHeader Double longitude, @RequestHeader("ipaddress") String ipAddress) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");

        try {
            CommonResponse<PayoutResponseDto> response = payoutOrchestrator.processPayout(sendPayoutRequest, latitude, longitude, ipAddress);
            log.info("======= sendPayoutRequest completed at {} =======", LocalDateTime.now());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            log.error("Error during payout request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to process payout request", null));
        }
    }
}
