package com.im.admin.service;

import com.im.admin.dto.BankVerificationRequest;
import org.springframework.http.ResponseEntity;

public interface VerificationService {
    ResponseEntity<?> validateAadhaar(String aadhaarNumber);

    ResponseEntity<?> validatepan(String panNumber);

    ResponseEntity<?> validateEmail(String email);

    ResponseEntity<?> validateVoterid(String voterid);

    ResponseEntity<?> AdharPanLink(String aadhaarNumber);

    ResponseEntity<?> validateBank(BankVerificationRequest request);

}
