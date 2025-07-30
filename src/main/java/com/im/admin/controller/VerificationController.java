package com.im.admin.controller;

import com.im.admin.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @PostMapping("/aadhaar-validate")
    public ResponseEntity<?> validateAadhaar(@RequestParam String aadhaarNumber) {
        return verificationService.validateAadhaar(aadhaarNumber);
    }
}
