package com.im.admin.service;

import com.im.admin.dto.CheckBalanceRequestDto;
import com.im.admin.dto.FundReversalRequestDto;

public interface FundReversalService {

    String takeActionOnFundReversal(FundReversalRequestDto actionDto);

    String checkBalance(CheckBalanceRequestDto checkBalanceRequestDto);

    String resendOtpById(Long id);

}

