package com.im.admin.service;

import com.im.admin.dto.BankDetailsResponseDto;
import com.im.admin.entities.BanksDetails;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BankDetailsService {

    CommonResponse<BanksDetails> save(BanksDetails request);

    CommonResponse<BanksDetails> update(Long id, BanksDetails request);

    CommonResponse<BanksDetails> getById(Long id);

    CommonResponse<Page<BanksDetails>> getAll(String hierarchyId, String bankName, Boolean isVerified, int page, int size);

    CommonResponse<String> delete(Long id);

    CommonResponse<List<BankDetailsResponseDto>> getVerifiedBankDetails(String hierarchyId);

}

