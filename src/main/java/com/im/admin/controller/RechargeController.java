package com.im.admin.controller;

import com.im.admin.orchestrator.RechargeOrchestrator;
import com.im.admin.request.RechargeRequest;

import com.im.admin.response.CommonResponse;
import com.im.admin.response.RechargeResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    @Autowired
    private RechargeOrchestrator rechargeOrchestrator;

    @PostMapping
    public ResponseEntity<CommonResponse<RechargeResponse>> recharge(
            @RequestBody RechargeRequest request) {

        RechargeResponse response =
                rechargeOrchestrator.recharge(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Recharge request submitted")
        );
    }

    @PostMapping("/balance")
    public ResponseEntity<CommonResponse<String>> checkBalance(
            @RequestBody RechargeRequest request) {

        String response =
                rechargeOrchestrator.checkBalance(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Balance fetched successfully")
        );
    }

    @PostMapping("/status")
    public ResponseEntity<CommonResponse<String>> checkStatus(
            @RequestBody RechargeRequest request) {

        String response =
                rechargeOrchestrator.checkStatus(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Status fetched successfully")
        );
    }
}
