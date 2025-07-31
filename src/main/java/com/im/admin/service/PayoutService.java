package com.im.admin.service;

import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.request.PayoutMerchantCallbackRequest;
import com.im.admin.request.PayoutResponseDto;
import com.im.admin.request.SendPayoutRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PayoutMerchantCallbackResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface PayoutService {

    String generateToken();

    CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest);

    PayoutMerchantCallbackResponse updateMerchantCallbackUrl(PayoutMerchantCallbackRequest request);

    String handlePayoutCallback(Map<String, Object> payload);

    CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto);

    CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(String search, Pageable pageable);

    CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids);

}
