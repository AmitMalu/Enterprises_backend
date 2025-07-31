package com.im.admin.controller;

import com.im.admin.dto.EmailValidationRequest;
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

    @PostMapping("/pan-validate")
    public ResponseEntity<?> validatePan(@RequestParam String panNumber) {
        return verificationService.validatepan(panNumber);
    }

    @PostMapping("/email-validate")
    public ResponseEntity<?> validateEmail(@RequestBody EmailValidationRequest email) {
        return verificationService.validateEmail(email.getEmail());
    }

    @PostMapping("/voterId-validate")
    public ResponseEntity<?> validateVoterid(@RequestParam String voterid) {
        return verificationService.validateVoterid(voterid);
    }

    @PostMapping("/aadhaar-pan-link")
    public ResponseEntity<?> AdharPanLink(@RequestParam String aadhaarNumber) {
        return verificationService.AdharPanLink(aadhaarNumber);
    }
}
