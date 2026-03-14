package com.im.admin.service;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.*;
import com.im.admin.response.*;
import org.springframework.http.ResponseEntity;

public interface AepsVendorService {

    String getVendorCode();

    CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(
            AepsCashWithdrawalRequest req,
            VendorCredentials credentials
    );
    ;

    ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials);

    ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials);

    CommonResponse<AepsMerchantResponseDto> onboardMerchant(
            AepsMerchantRequestDto request
    );

    ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(
            AepsSendOtpRequest req,
            VendorCredentials credentials
    );

    ResponseEntity<AepsValidateOtpResponseDto> validateOtp(
            AepsValidateOtpRequest req,
            VendorCredentials credentials
    );

    ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(
            AepsResendOtpRequest req,
            VendorCredentials credentials
    );

    ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(
            AepsBiometricEkycRequest req,
            VendorCredentials credentials
    );

    ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(
            AepsCombinedRequest req,
            VendorCredentials credentials
    );


    ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(
            Aeps2FaRequest req,
            VendorCredentials credentials
    );}
