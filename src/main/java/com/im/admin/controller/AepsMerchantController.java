package com.im.admin.controller;

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


    @PostMapping("/onboarding")
    public ResponseEntity<AepsMerchantResponseDto> register(@RequestBody AepsMerchantRequestDto aepsMerchantRequestDto) {
        try {

            ResponseEntity<AepsMerchantResponseDto> vendorResp =
                    aepsMerchantService.registerMerchantAndCall(aepsMerchantRequestDto);

            return ResponseEntity
                    .status(vendorResp.getStatusCode())
                    .body(vendorResp.getBody());

        } catch (Exception ex) {
            ex.printStackTrace();

            // Build error response using your DTO
            AepsMerchantResponseDto errorDto = new AepsMerchantResponseDto();
            errorDto.setStatus(false);
            errorDto.setMessage("Error: " + ex.getMessage());
            errorDto.setData(null);
            errorDto.setStatusCode(500);

            return ResponseEntity.internalServerError().body(errorDto);
        }
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
