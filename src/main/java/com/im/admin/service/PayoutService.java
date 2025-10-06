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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PayoutService {

    String generateToken();

    CommonResponse<PayoutResponseDto> sendPayoutRequest(SendPayoutRequest sendPayoutRequest);

    PayoutMerchantCallbackResponse updateMerchantCallbackUrl(SendPayoutRequest request);

    String handlePayoutCallback(Map<String, Object> payload);

    CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto);

    CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids);

    CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(Long id, String retailerId, String bankName, String bankHolderName, String accountNumber, String ifscCode, String paymentMode, Boolean isVerified, LocalDate createdOn, String createdBy, Pageable pageable);

    CommonResponse<BeneficiaryBankAccountDTO> updateBeneficiary(Long id, BeneficiaryBankAccountDTO requestDto);
}
