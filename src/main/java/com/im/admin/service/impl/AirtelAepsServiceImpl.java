package com.im.admin.service.impl;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.request.*;
import com.im.admin.response.*;
import com.im.admin.service.AepsVendorService;
import org.springframework.http.ResponseEntity;

public class AirtelAepsServiceImpl implements AepsVendorService {
    @Override
    public CommonResponse<AepsMerchantResponseDto> onboardMerchant(AepsMerchantRequestDto request) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<AepsEkycResponseDto>> sendOtp(AepsSendOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsValidateOtpResponseDto> validateOtp(AepsValidateOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<AepsResendOtpResponseDto>> resendOtp(AepsResendOtpRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsBiometricEkycResponseDto> biometricEkyc(AepsBiometricEkycRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<AepsBiometricEkycResponseDto>> validateOtpThenBiometric(AepsCombinedRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<CommonResponse<Aeps2FaResponseDto>> validateAadhar(Aeps2FaRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsMiniStatementResponse> processMiniStateMent(AepsMiniStatementRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public CommonResponse<AepsCashWithdrawalResponse> processCashWithdrawal(AepsCashWithdrawalRequest req, VendorCredentials credentials) {
        return null;
    }

    @Override
    public ResponseEntity<AepsBalanceInquiryResponse> processBalanceInquiry(AepsBalanceInquiryRequest req, VendorCredentials credentials) {
        return null;
    }



    @Override
    public String getVendorCode() {
        return "Airtel Aeps";
    }
}
