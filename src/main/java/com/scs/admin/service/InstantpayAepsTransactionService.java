package com.scs.admin.service;

import com.scs.admin.request.InstantPayCashWithdrawalRequestDto;
import com.scs.admin.request.InstantpayBalanceEnquiryRequestDto;
import com.scs.admin.request.InstantpayMiniStatementRequestDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.InstantpayBalanceEnquiryResponseDto;
import com.scs.admin.response.InstantpayCashWithdrawalResponseDto;
import com.scs.admin.response.InstantpayMiniStatementResponseDto;

public interface InstantpayAepsTransactionService {

    CommonResponse<InstantpayCashWithdrawalResponseDto> cashWithdrawal(
            InstantPayCashWithdrawalRequestDto request,Double latitude,Double longitude);

    CommonResponse<InstantpayBalanceEnquiryResponseDto> balanceEnquiry(
            InstantpayBalanceEnquiryRequestDto req,Double latitude,Double longitude);

    CommonResponse<InstantpayMiniStatementResponseDto> miniStatement(InstantpayMiniStatementRequestDto req,Double latitude,Double longitude);
}