package com.scs.admin.service;

import com.scs.admin.dto.BankDetailsResponseDto;
import com.scs.admin.entities.BanksDetails;
import com.scs.admin.response.CommonResponse;
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

