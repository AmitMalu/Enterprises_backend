package com.scs.admin.service;

import com.scs.admin.dto.CheckBalanceRequestDto;
import com.scs.admin.dto.FundReversalRequestDto;
import com.scs.admin.dto.RequestHeaderMetaDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.FundReversalResponseDto;

public interface FundReversalService {

    CommonResponse<FundReversalResponseDto> takeActionOnFundReversal(FundReversalRequestDto request, RequestHeaderMetaDto requestHeaderMetaDtoMeta);

    CommonResponse<FundReversalResponseDto> resendOtpById(Long id);

    String checkBalance(CheckBalanceRequestDto checkBalanceRequestDto);

}

