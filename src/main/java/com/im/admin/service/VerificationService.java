package com.im.admin.service;

import org.springframework.http.ResponseEntity;

public interface VerificationService {
    ResponseEntity<?> validateAadhaar(String aadhaarNumber);
}
