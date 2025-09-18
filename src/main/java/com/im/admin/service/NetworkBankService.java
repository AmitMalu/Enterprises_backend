package com.im.admin.service;

import com.im.admin.dto.BankRequestDto;
import com.im.admin.entities.NetworkBankMaster;
import com.im.admin.response.BankInfoResponse;
import com.im.admin.response.NetworkBankDropdownResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;


public interface NetworkBankService {
    NetworkBankMaster addBank(BankRequestDto bankRequestDto, MultipartFile logoFile) throws IOException;

    

    NetworkBankMaster updateBank(Long id, BankRequestDto bankRequestDto, MultipartFile logoFile);

    NetworkBankMaster getBankById(Long id);

    List<NetworkBankDropdownResponse> getBankDropdownList();

    BankInfoResponse getBankInfoByIfsc(String ifscCode);

    Page<NetworkBankMaster> getAllBanks(Pageable pageable, String id, String bankCode, String bankName, String bankBranchName, String ifscCode, Boolean imps, Boolean neft, Boolean rtgs, Boolean isActive, String createdBy, String editedBy, Boolean isBlocked, LocalDate createdOn, LocalDate editedOn);
}
