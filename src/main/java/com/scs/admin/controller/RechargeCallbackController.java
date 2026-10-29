package com.scs.admin.controller;

import com.scs.admin.orchestrator.RechargeOrchestrator;
import com.scs.admin.service.impl.RechargeCallbackService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recharge")
@RequiredArgsConstructor
public class RechargeCallbackController {

    @Autowired
    private RechargeCallbackService rechargeCallbackService;

    @Autowired
    private RechargeOrchestrator rechargeOrchestrator;

    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(
            @RequestParam Map<String, String> payload,
            HttpServletRequest request) {

        log.info("Recharge callback received: {}", payload);

        rechargeOrchestrator.processRechargeCallback("Cellmoney Recharge",payload, request);

        return ResponseEntity.ok("Callback received");
    }
}
