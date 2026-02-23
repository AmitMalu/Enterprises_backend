package com.im.admin.controller;

import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.orchestrator.PayoutOrchestratorV0;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
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
public class PayoutControllerV0 {

    @Autowired
    private PayoutOrchestratorV0 payoutOrchestratorV0;

    @PostMapping("/send-payout-vo")
    public ResponseEntity<CommonResponse<PayoutResponseDto>> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");

        try {
            CommonResponse<PayoutResponseDto> response = payoutOrchestratorV0.processPayout(sendPayoutRequest);
            log.info("======= sendPayoutRequest completed at {} =======", LocalDateTime.now());
            return ResponseEntity.status(response.getStatusCode()).body(response);
        } catch (Exception e) {
            log.error("Error during payout request: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to process payout request", null));
        }
    }

    @PostMapping("/update-callback-url-vo")
    public ResponseEntity<CommonResponse<PayoutMerchantCallbackResponse>> updateCallbackUrl(
            @RequestBody SendPayoutRequest request) {
        log.info("[CALLBACK-REGISTER] Received callback registration request: subProductId={}, retailerId={}",
                request.getSubProductId(), request.getRetailerId());

        CommonResponse<PayoutMerchantCallbackResponse> response = payoutOrchestratorV0.registerCallbackUrl(request);

        log.info("[CALLBACK-REGISTER] Response: {}", response.getMessage());
        return ResponseEntity.status(response.getStatusCode()).body(response);
    }



}
