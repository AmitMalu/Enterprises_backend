package com.im.admin.controller;

import com.im.admin.orchestrator.DmtOrchestrator;
import com.im.admin.request.InstantpayMerchantSignupRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.InstantpayMerchantSignupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
public class DmtMerchantController {

    @Autowired
    private DmtOrchestrator dmtOrchestrator;

//    @PostMapping("/signup-min-kyc")
//    public ResponseEntity<CommonResponse<InstantpayMerchantSignupResponse>> signupMinKyc(
//            @RequestBody InstantpayMerchantSignupRequest request) {
//
//        CommonResponse<InstantpayMerchantSignupResponse> response =
//                dmtOrchestrator.signupMinKyc(request);
//
//        return ResponseEntity
//                .status(response.getStatusCode())
//                .body(response);
//    }

}
