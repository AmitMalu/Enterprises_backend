package com.im.admin.controller;

import com.im.admin.request.AepsCheckStatus;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.response.AepsMerchantResponseDto;
import com.im.admin.response.AepsStatusCheckResponse;
import com.im.admin.service.AepsMerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/merchant")
public class AepsMerchantController {

    @Autowired
    private AepsMerchantService aepsMerchantService;

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
    public ResponseEntity<AepsStatusCheckResponse> checkStatus(@RequestBody AepsCheckStatus aepsCheckStatus) {
        AepsStatusCheckResponse response = aepsMerchantService.checkStatus(aepsCheckStatus);
        return ResponseEntity.ok(response);
    }

}
