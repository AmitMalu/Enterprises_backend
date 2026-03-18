package com.im.admin.service;

import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.FundReversalResponseDto;

public interface FundReversalService {

    CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(FundReversalRequestDto request, Double latitude, Double longitude);

    CommonResponse<FundReversalResponseDto> resendOtpById(Long id);

    String checkBalance(CheckBalanceRequestDto checkBalanceRequestDto);

}

