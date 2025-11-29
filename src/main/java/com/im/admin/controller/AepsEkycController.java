package com.im.admin.controller;

import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsEkycService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aeps/ekyc")
public class AepsEkycController {


    @Autowired
    private AepsEkycService aepsEkycService;

    @PostMapping("/send-otp")
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(@RequestBody AepsSendOtpRequest req) {
        return aepsEkycService.sendOtp(req);
    }


    @PostMapping("/validate-otp")
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(@RequestBody AepsValidateOtpRequest req) {
        return aepsEkycService.validateOtp(req);
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(
            @RequestBody AepsResendOtpRequest req) {
        return aepsEkycService.resendOtp(req);
    }


    @PostMapping("/biometric")
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(
            @RequestBody AepsBiometricEkycRequest req) {
        return aepsEkycService.biometricEkyc(req);
    }

    @PostMapping("/validate-otp-biometric")
    public ResponseEntity<AepsBiometricEkycResponseDto> validateOtpThenBiometric(
            @RequestBody AepsCombinedRequest req) {

        return aepsEkycService.validateOtpThenBiometric(req);
    }


}
