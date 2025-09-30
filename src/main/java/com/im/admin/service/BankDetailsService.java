package com.im.admin.service;

import com.im.admin.entities.BanksDetailss;
import com.im.admin.response.CommonResponse;
import org.springframework.data.domain.Page;

public interface BankDetailsService {

    CommonResponse<BanksDetailss> save(BanksDetailss request);

    CommonResponse<BanksDetailss> update(Long id, BanksDetailss request);

    CommonResponse<BanksDetailss> getById(Long id);

    CommonResponse<Page<BanksDetailss>> getAll(String hierarchyId, String bankName, Boolean isVerified, int page, int size);

    CommonResponse<String> delete(Long id);
}

