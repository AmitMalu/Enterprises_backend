package com.im.admin.controller;

import com.im.admin.request.Aeps2FaRequest;
import com.im.admin.response.Aeps2FaResponseDto;
import com.im.admin.service.AepsTwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aeps/2fa")
public class AepsTwoFactorController {

    @Autowired
    private AepsTwoFactorService aepsTwoFactorService;

    @PostMapping("/validate-aadhar")
    public ResponseEntity<Aeps2FaResponseDto> validateAadhar(@RequestBody Aeps2FaRequest req) {
        return aepsTwoFactorService.validateAadhar(req);
    }
}
