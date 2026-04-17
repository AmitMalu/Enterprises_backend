package com.im.admin.service;

import com.im.admin.request.InstantPayCashWithdrawalRequestDto;
import com.im.admin.request.InstantpayBalanceEnquiryRequestDto;
import com.im.admin.request.InstantpayMiniStatementRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.InstantpayBalanceEnquiryResponseDto;
import com.im.admin.response.InstantpayCashWithdrawalResponseDto;
import com.im.admin.response.InstantpayMiniStatementResponseDto;

public interface InstantpayAepsTransactionService {

    CommonResponse<InstantpayCashWithdrawalResponseDto> cashWithdrawal(
            InstantPayCashWithdrawalRequestDto request,Double latitude,Double longitude);

    CommonResponse<InstantpayBalanceEnquiryResponseDto> balanceEnquiry(
            InstantpayBalanceEnquiryRequestDto req,Double latitude,Double longitude);

    CommonResponse<InstantpayMiniStatementResponseDto> miniStatement(InstantpayMiniStatementRequestDto req,Double latitude,Double longitude);
}