package com.scs.admin.service;

import com.scs.admin.dto.BankVerificationRequest;
import com.scs.admin.dto.RequestHeaderMetaDto;
import org.springframework.http.ResponseEntity;

public interface VerificationService {

    ResponseEntity<?> validateAadhaar(String aadhaarNumber);

    ResponseEntity<?> validatePan(String panNumber);

    ResponseEntity<?> validateEmail(String email);

    ResponseEntity<?> validateVoterId(String voterid);

    ResponseEntity<?> AdharPanLink(String aadhaarNumber);

    ResponseEntity<?> validateBank(BankVerificationRequest request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

}
