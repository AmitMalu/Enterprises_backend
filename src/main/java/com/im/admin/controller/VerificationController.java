package com.im.admin.controller;

import com.im.admin.dto.BankVerificationRequest;
import com.im.admin.dto.EmailValidationRequest;
import com.im.admin.service.VerificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/verification")
public class VerificationController {

    @Autowired
    private VerificationService verificationService;

    @PostMapping("/aadhaar-validate")
    public ResponseEntity<?> validateAadhaar(@RequestParam String aadhaarNumber) {
        log.info("Validating Aadhaar number: {}", aadhaarNumber);
        ResponseEntity<?> response = verificationService.validateAadhaar(aadhaarNumber);
        log.debug("Aadhaar validation response: {}", response);
        return response;
    }

    @PostMapping("/pan-validate")
    public ResponseEntity<?> validatePan(@RequestParam String panNumber) {
        log.info("Validating PAN number: {}", panNumber);
        ResponseEntity<?> response = verificationService.validatePan(panNumber);
        log.debug("PAN validation response: {}", response);
        return response;
    }

    @PostMapping("/email-validate")
    public ResponseEntity<?> validateEmail(@RequestBody EmailValidationRequest email) {
        log.info("Validating email: {}", email.getEmail());
        ResponseEntity<?> response = verificationService.validateEmail(email.getEmail());
        log.debug("Email validation response: {}", response);
        return response;
    }

    @PostMapping("/voterId-validate")
    public ResponseEntity<?> validateVoterid(@RequestParam String voterid) {
        log.info("Validating Voter ID: {}", voterid);
        ResponseEntity<?> response = verificationService.validateVoterId(voterid);
        log.debug("Voter ID validation response: {}", response);
        return response;
    }

    @PostMapping("/aadhaar-pan-link")
    public ResponseEntity<?> AdharPanLink(@RequestParam String aadhaarNumber) {
        log.info("Linking Aadhaar with PAN for Aadhaar number: {}", aadhaarNumber);
        ResponseEntity<?> response = verificationService.AdharPanLink(aadhaarNumber);
        log.debug("Aadhaar-PAN link response: {}", response);
        return response;
    }

    @PostMapping("/bank-validate")
    public ResponseEntity<?> validateBank(@RequestBody BankVerificationRequest request) {
        log.info("Validating Bank Account: {}", request.getId_number());
        ResponseEntity<?> response = verificationService.validateBank(request);
        log.debug("Bank validation response: {}", response);
        return response;
    }
}
