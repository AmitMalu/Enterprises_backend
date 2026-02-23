package com.im.admin.controller;

import com.im.admin.dto.Aeps2FACheckStatusRequest;
import com.im.admin.entities.AepsTwoFa;
import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.request.AepsTwoFaStatusDto;
import com.im.admin.response.Aeps2FaResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.AepsTwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/aeps/2fa")
public class AepsTwoFactorController {

    @Autowired
    private AepsTwoFactorService aepsTwoFactorService;

    @PostMapping("/validate-aadhar")
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(@RequestBody Aeps2FaRequest req) {
        return aepsTwoFactorService.validateAadhar(req);
    }



    @PostMapping("/check-status")
    public ResponseEntity<CommonResponse<AepsTwoFaStatusDto>> check2FA(
            @RequestBody Aeps2FACheckStatusRequest req) {
        return aepsTwoFactorService.check2FAStatus(req);
    }

}
