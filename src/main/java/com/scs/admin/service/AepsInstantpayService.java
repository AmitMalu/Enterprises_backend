package com.scs.admin.service;

import com.scs.admin.dto.InstantPayMerchantRequestDto;
import com.scs.admin.request.*;
import com.scs.admin.response.*;

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


    CommonResponse<AepsStatusDataDto> checkStatus(AepsCheckStatus aepsCheckStatus);

}
