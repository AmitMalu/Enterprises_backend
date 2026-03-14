package com.im.admin.controller;

import com.im.admin.orchestrator.AepsOrchestrator;
import com.im.admin.request.AepsCheckStatus;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.request.AepsSendOtpRequest;
import com.im.admin.request.AepsStatusDataDto;
import com.im.admin.response.AepsEkycResponseDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.AepsStatusCheckResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsEkycService;
import com.im.admin.service.AepsMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/merchant")
public class AepsMerchantController {

    @Autowired
    private AepsMerchantService aepsMerchantService;


    @Autowired
    private AepsEkycService aepsEkycService;

    @Autowired
    private AepsOrchestrator aepsOrchestrator;


    @PostMapping("/onboarding")
    public CommonResponse<AepsMerchantResponseDto> register(
            @RequestBody AepsMerchantRequestDto req) {

        return aepsMerchantService.registerMerchantAndCall(req);
    }

    @PostMapping("/check-status")
    public CommonResponse<AepsStatusDataDto> checkStatus(@RequestBody AepsCheckStatus req) {
        return aepsMerchantService.checkStatus(req);
    }

    @PostMapping("/send-otp")
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(@RequestBody AepsSendOtpRequest req) {
        return aepsEkycService.sendOtp(req);
    }


}
