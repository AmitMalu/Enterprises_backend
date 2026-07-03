package com.im.admin.controller;

import com.im.admin.commons.LoggableApi;
import com.im.admin.dto.BeneficiaryVerifyRequestDto;
import com.im.admin.dto.BeneficiaryVerifyResponseDto;
import com.im.admin.dto.RequestHeaderMetaDto;
import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@LoggableApi
@RequestMapping("/api")
@Slf4j
@RestController
public class PayoutController {

    @Autowired
    private PayoutOrchestrator payoutOrchestratorV0;

    @PostMapping("/send-payout")
    public ResponseEntity<CommonResponse<PayoutResponseDto>> sendPayoutRequest(@RequestBody SendPayoutRequest sendPayoutRequest, HttpServletRequest httpServletRequest) {
        log.info("======= Calling payoutService.sendPayoutRequest =======");

        try {
            RequestHeaderMetaDto requestHeaderMetaDtoMeta = RequestHeaderMetaDto.from(httpServletRequest);

            CommonResponse<PayoutResponseDto> response = payoutOrchestratorV0.processPayout(sendPayoutRequest, requestHeaderMetaDtoMeta);
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

    @PostMapping("/beneficiary-verify")
    public ResponseEntity<CommonResponse<BeneficiaryVerifyResponseDto>> beneficiaryVerify(
            @RequestBody BeneficiaryVerifyRequestDto request) {

        log.info("======= Calling payoutOrchestrator.beneficiaryVerify =======");

        try {
            CommonResponse<BeneficiaryVerifyResponseDto> response =
                    payoutOrchestratorV0.beneficiaryVerify(request);

            return ResponseEntity.status(response.getStatusCode()).body(response);

        } catch (Exception e) {
            log.error("Error verifying beneficiary: ", e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to verify beneficiary", null));
        }
    }


}
