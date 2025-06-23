package com.im.admin.service;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.BankMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


public interface BankService {
    BankMaster addBank(BankRequestDto bankRequestDto);

    Page<BankMaster> getAllBanks(Pageable pageable, String id, String bankCode, String bankName,
                                 String ifscCode, String imps, String neft,
                                 String rtgs, String activeStatus, String createdBy,
                                 String editedBy, Boolean isBlocked, LocalDate createdOn,
                                 LocalDate editedOn);

    BankMaster updateBank(Long id,BankRequestDto bankRequestDto);

    BankMaster getBankById(Long id);
}
