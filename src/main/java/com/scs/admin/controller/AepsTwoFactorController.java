package com.scs.admin.controller;

import com.scs.admin.dto.Aeps2FACheckStatusRequest;
import com.scs.admin.entities.AepsTwoFa;
import com.scs.admin.request.Aeps2FaRequest;
import com.scs.admin.request.AepsTwoFaStatusDto;
import com.scs.admin.response.Aeps2FaResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.service.AepsTwoFactorService;
import com.scs.admin.service.impl.AepsTwoFactorOrchestrator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/aeps/2fa")
public class AepsTwoFactorController {

    @Autowired
    private AepsTwoFactorService aepsTwoFactorService;

    @Autowired
    private AepsTwoFactorOrchestrator aepsTwoFactorOrchestrator;

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
