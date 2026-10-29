package com.scs.admin.controller;

import com.scs.admin.orchestrator.AepsOrchestrator;
import com.scs.admin.request.AepsCheckStatus;
import com.scs.admin.request.AepsMerchantRequestDto;
import com.scs.admin.request.AepsSendOtpRequest;
import com.scs.admin.request.AepsStatusDataDto;
import com.scs.admin.response.AepsEkycResponseDto;
import com.scs.admin.response.AepsMerchantResponseDto;
import com.scs.admin.response.AepsStatusCheckResponse;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AepsEkycService;
import com.scs.admin.service.AepsMerchantService;
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
