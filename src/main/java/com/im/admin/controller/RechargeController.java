package com.im.admin.controller;

import com.im.admin.orchestrator.RechargeOrchestrator;
import com.im.admin.request.DthCustomerInfoRequest;
import com.im.admin.request.MobileLookupRequest;
import com.im.admin.request.RechargePlanRequest;
import com.im.admin.request.RechargeRequest;

import com.im.admin.response.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recharge")
public class RechargeController {

    @Autowired
    private RechargeOrchestrator rechargeOrchestrator;

    @PostMapping
    public ResponseEntity<CommonResponse<RechargeResponse>> recharge(
            @RequestBody RechargeRequest request, @RequestHeader Double latitude, @RequestHeader Double longitude, @RequestHeader("ipaddress") String ipAddress ) {

        RechargeResponse response =
                rechargeOrchestrator.recharge(request, latitude, longitude, ipAddress);

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

    @PostMapping("/db-status")
    public ResponseEntity<CommonResponse<String>> checkStatusFromDb(
            @RequestBody RechargeRequest request) {

        String response =
                rechargeOrchestrator.checkStatusDb(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Status fetched successfully")
        );
    }

    @PostMapping("/mobile-lookup")
    public ResponseEntity<CommonResponse<MobileLookupResponse>> mobileLookup(
            @RequestBody MobileLookupRequest request) {

        MobileLookupResponse response =
                rechargeOrchestrator.mobileLookup(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Lookup success")
        );
    }

    @PostMapping("/operators")
    public ResponseEntity<CommonResponse<OperatorResponse>> getOperators( @RequestBody MobileLookupRequest request) {

        OperatorResponse response = rechargeOrchestrator.getOperatorCodes(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Operators fetched successfully")
        );
    }

    @PostMapping("/plans")
    public ResponseEntity<CommonResponse<RechargePlanResponse>> getRechargePlans(
            @RequestBody RechargePlanRequest request) {

        RechargePlanResponse response =
                rechargeOrchestrator.getRechargePlans(request);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Recharge plans fetched successfully")
        );
    }

    @PostMapping("/dth-customer-info")
    public ResponseEntity<CommonResponse<DthCustomerInfoResponse>>
    getDthCustomerInfo(
            @RequestBody DthCustomerInfoRequest request) {

        DthCustomerInfoResponse response =
                rechargeOrchestrator.getDthCustomerInfo(request);

        return ResponseEntity.ok(
                CommonResponse.success(
                        response,
                        "Customer information fetched successfully"
                )
        );
    }

}
