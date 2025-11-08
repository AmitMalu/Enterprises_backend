package com.im.admin.service;

import com.im.admin.dto.BeneficiaryBankAccountDTO;
import com.im.admin.entities.BeneficiaryBankAccount;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface PayoutBeneficiaryService {

    CommonResponse<BeneficiaryBankAccount> saveBeneficiary(BeneficiaryBankAccountDTO dto);

    CommonResponse<String> deleteMultipleBeneficiaries(List<Long> ids);

    CommonResponse<Page<BeneficiaryBankAccount>> getAllFiltered(Long id, String retailerId, String bankName, String bankHolderName, String accountNumber, String ifscCode, String paymentMode, Boolean isVerified, LocalDate createdOn, String createdBy, Pageable pageable);

    CommonResponse<BeneficiaryBankAccountDTO> updateBeneficiary(Long id, BeneficiaryBankAccountDTO requestDto);

}
