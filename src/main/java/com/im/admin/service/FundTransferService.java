package com.im.admin.service;

import com.im.admin.request.FundTransferRequestDto;
import com.im.admin.response.FundTransferResponseDto;

public interface FundTransferService {

    FundTransferResponseDto transferFunds(FundTransferRequestDto requestDto);

}
