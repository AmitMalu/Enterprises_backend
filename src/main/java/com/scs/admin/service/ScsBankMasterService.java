package com.scs.admin.service;

import com.scs.admin.entities.ScsBankMaster;
import com.scs.admin.request.ScsBankMasterRequestDTO;
import com.scs.admin.response.BankResponseDto;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.ScsBankDropdownResponse;
import com.scs.admin.response.ScsBankMasterResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ScsBankMasterService {

    CommonResponse<ScsBankMasterResponseDTO> create(ScsBankMasterRequestDTO dto);

    CommonResponse<ScsBankMasterResponseDTO> update(Long id, ScsBankMasterRequestDTO dto);

    CommonResponse<ScsBankMasterResponseDTO> getById(Long id);

    CommonResponse<Page<ScsBankMaster>> getAll(
            Long id,
            String bankName,
            String accountNumber,
            String ifscCode,
            Boolean charges,
            String chargesType,
            Boolean isActive,
            Boolean isBlocked,
            String createdBy,
            String editedOn,
            int page,
            int size
    );

    CommonResponse<List<ScsBankDropdownResponse>> getDropdown();

    List<BankResponseDto> getBankDetails();

    List<BankResponseDto> getWhiteLabelBankDetails();
}
