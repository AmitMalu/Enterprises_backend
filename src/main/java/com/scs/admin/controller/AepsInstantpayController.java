package com.scs.admin.controller;


import com.scs.admin.dto.InstantPayMerchantRequestDto;
import com.scs.admin.request.*;
import com.scs.admin.response.*;
import com.scs.admin.service.AepsInstantpayService;
import com.scs.admin.util.InstantpayEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/instantpay/aeps")
public class AepsInstantpayController {

    @Autowired
    private AepsInstantpayService aepsInstantpayService;

    @PostMapping("/onboard-merchant")
    public CommonResponse<InstantpayMerchantResponseDto> onboardMerchant(
            @RequestBody InstantPayMerchantRequestDto req) {

        return aepsInstantpayService.onboardMerchant(req);
    }

    @PostMapping("/kyc-status")
    public CommonResponse<InstantpayKycStatusResponseDto> checkKycStatus(
            @RequestBody InstantpayKycStatusRequestDto req) {

        return aepsInstantpayService.checkKycStatus(req);
    }

    @PostMapping("/biometric-kyc")
    public CommonResponse<InstantpayBiometricKycResponseDto> biometricKyc(
            @RequestBody InstantpayBiometricKycRequestDto req) {

        return aepsInstantpayService.biometricKyc(req);
    }

    @PostMapping("/outlet-login-status")
    public CommonResponse<Instantpay2FaResponseDto> outletLoginStatus(
            @RequestBody Instantpay2FaRequestDto request) {

        return aepsInstantpayService.outletLoginStatus(request);
    }


    @PostMapping("/outlet-login")
    public CommonResponse<Instantpay2FaResponseDto> outletLogin(
            @RequestBody Instantpay2FaRequestDto request) {

        return aepsInstantpayService.outletLogin(request);
    }

    @PostMapping("/banks")
    public CommonResponse<InstantpayBankListResponseDto> getBanks(
            @RequestBody InstantPayMerchantRequestDto req) {

        return aepsInstantpayService.getBanks(req);
    }

    @PostMapping("/encrypt-aadhaar")
    public Map<String, Object> encryptAadhaar(
            @RequestParam String aadhaarNumber,
            @RequestParam String key
    ) throws Exception {

        String encrypted = InstantpayEncryptionUtil.encryptAadhaar(aadhaarNumber, key);

        Map<String, Object> response = new HashMap<>();
        response.put("aadhaar", aadhaarNumber);
        response.put("encryptedAadhaar", encrypted);

        return response;
    }

    @PostMapping("/check-status")
    public CommonResponse<AepsStatusDataDto> checkStatus(@RequestBody AepsCheckStatus req) {
        return aepsInstantpayService.checkStatus(req);
    }
}
