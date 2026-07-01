package com.im.admin.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.im.admin.orchestrator.PayoutOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/payout")
@RequiredArgsConstructor
@Slf4j
public class PayoutCallbackController {

    @Autowired
    private PayoutOrchestrator payoutOrchestratorV0;

    @PostMapping("/callback/runpaisa")
    public ResponseEntity<String> payoutCallback(@RequestBody Map<String, String> payload,
                                                 HttpServletRequest request) throws JsonProcessingException {
        log.info("<---------------- Received callback request with payload: {} --------------------->", payload);
        String response = payoutOrchestratorV0.processPayoutCallback("Runpaisa Payout old", payload, request);
        log.info("Callback response from runpaisa: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/unisus-callback")
    public ResponseEntity<String> unisusPayCallback(@RequestBody Map<String, String> payload,
                                                 HttpServletRequest request) throws JsonProcessingException {
        log.info("<---------------- UnisusPay callback request with payload: {} --------------------->", payload);
        String response = payoutOrchestratorV0.unisusPayoutCallback(payload, request);
        log.info("Callback response from unisus: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/nixasoft")
    public ResponseEntity<String> nixasoftPayCallback(@RequestBody Map<String, String> payload,
                                                    HttpServletRequest request) throws JsonProcessingException {
        log.info("<---------------- Nixasoft callback request with payload: {} --------------------->", payload);
        String response = payoutOrchestratorV0.nixasoftPayoutCallback(payload, request);
        log.info("Callback response from nixasoft: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/slpe")
    public ResponseEntity<String> slpePayCallback(@RequestBody Map<String, String> payload,
                                                    HttpServletRequest request) throws JsonProcessingException {
        log.info("<---------------- SLPE callback request with payload: {} --------------------->", payload);
        String response = payoutOrchestratorV0.slpePayoutCallback(payload, request);
        log.info("Callback response from slpe: {}", response);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/callback/vimopay")
    public ResponseEntity<Object> vimoPayCallback(
            @RequestBody(required = false) Map<String, Object> jsonBody,
            @RequestParam(required = false) Map<String, String> formParams) {

        log.info("<---------------- VIMOPAY callback request with payload: {} --------------------->", jsonBody);

        Map<String, Object> body = new HashMap<>();

        if (jsonBody != null && !jsonBody.isEmpty()) {
            body.putAll(jsonBody);
        } else if (formParams != null && !formParams.isEmpty()) {
            body.putAll(formParams);
        }

        log.info("Vendor callback received");
        log.debug("Callback body: {}", body);

        payoutOrchestratorV0.handleEncryptedCallbackAsync(body);

        return ResponseEntity.ok(Map.of(
                "successStatus", true,
                "message", "Success",
                "responseCode", "000"
        ));
    }

}
