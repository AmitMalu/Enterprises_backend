package com.im.admin.service;

import com.im.admin.dto.InstantPayMerchantRequestDto;
import com.im.admin.request.AepsMerchantRequestDto;
import com.im.admin.request.Instantpay2FaRequestDto;
import com.im.admin.request.InstantpayBiometricKycRequestDto;
import com.im.admin.request.InstantpayKycStatusRequestDto;
import com.im.admin.response.*;

public interface AepsInstantpayService {

    CommonResponse<InstantpayMerchantResponseDto> onboardMerchant(
            InstantPayMerchantRequestDto req
    );

    CommonResponse<InstantpayKycStatusResponseDto> checkKycStatus(
            InstantpayKycStatusRequestDto req);

    CommonResponse<InstantpayBiometricKycResponseDto> biometricKyc(
            InstantpayBiometricKycRequestDto req);

    CommonResponse<Instantpay2FaResponseDto> outletLoginStatus(
            Instantpay2FaRequestDto req);

    CommonResponse<Instantpay2FaResponseDto> outletLogin(
            Instantpay2FaRequestDto req);


    CommonResponse<InstantpayBankListResponseDto> getBanks(InstantPayMerchantRequestDto req);
}
