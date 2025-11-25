package com.im.admin.service;

import com.im.admin.request.*;
import com.im.admin.response.*;
import org.springframework.http.ResponseEntity;

public interface AepsEkycService {
    ResponseEntity<AepsEkycResponseDto> sendOtp(AepsSendOtpRequest req);

    ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req);

    ResponseEntity<AepsResendOtpResponseDto> resendOtp(AepsResendOtpRequest req);

    ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req);

    ResponseEntity<AepsBiometricEkycResponseDto> validateOtpThenBiometric(AepsCombinedRequest req);
}
