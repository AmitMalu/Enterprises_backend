package com.im.admin.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.orchestrator.PayoutOrchestrator;
import com.im.admin.util.VendorFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payout")
@RequiredArgsConstructor
@Slf4j
public class CallbackController {

    @Autowired
    private PayoutOrchestrator orchestrator; // for fallback or shared logic

    @PostMapping("/callback")
    public ResponseEntity<String> payoutCallback(@RequestBody Map<String, String> payload,
                                                 HttpServletRequest request) throws JsonProcessingException {
        log.info("<---------------- Received callback request with payload: {} --------------------->", payload);
        String response = orchestrator.processPayoutCallback("Runpaisa Payout", payload, request);
        log.info("Callback response: {}", response);
        return ResponseEntity.ok(response);
    }

}
