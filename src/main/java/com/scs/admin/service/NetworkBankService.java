package com.scs.admin.service;

import com.scs.admin.dto.BankListRequest;
import com.scs.admin.dto.BankRequestDto;
import com.scs.admin.entities.NetworkBankMaster;
import com.scs.admin.request.NetworkBankMasterResponseDto;
import com.scs.admin.response.BankInfoResponse;
import com.scs.admin.response.NetworkBankDropdownResponse;
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

    List<NetworkBankMasterResponseDto> getAllNetworkBanks();

    void saveBankList(BankListRequest request);

    /**
     * Bulk upload banks from an .xlsx / .xls / .csv file.
     * Inserts up to 5000 records in batches for maximum performance.
     *
     * @param file      the uploaded Excel file
     * @param createdBy who triggered the upload (passed as query param)
     * @return upload summary (total rows, inserted, skipped, errors)
     */
    BankBulkUploadResult bulkUpload(MultipartFile file, String createdBy);

}
