package com.scs.admin.service;

import com.scs.admin.request.*;
import com.scs.admin.response.*;
import org.springframework.http.ResponseEntity;

public interface AepsEkycService {
    ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req);


    ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req);

    ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req);


    ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req);

    ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(AepsCombinedRequest req);

}
