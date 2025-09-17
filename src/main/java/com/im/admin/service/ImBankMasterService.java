package com.im.admin.service;

import com.im.admin.entities.ImBankMaster;
import com.im.admin.request.ImBankMasterRequestDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.ImBankDropdownResponse;
import com.im.admin.response.ImBankMasterResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ImBankMasterService {

    CommonResponse<ImBankMasterResponseDTO> create(ImBankMasterRequestDTO dto);

    CommonResponse<ImBankMasterResponseDTO> update(Long id, ImBankMasterRequestDTO dto);

    CommonResponse<ImBankMasterResponseDTO> getById(Long id);

    CommonResponse<Page<ImBankMaster>> getAll(
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

    CommonResponse<List<ImBankDropdownResponse>> getDropdown();
}
