package com.im.admin.service;

import com.im.admin.dto.PinCodeDTO;
import com.im.admin.dto.PinCodeRequestDTO;
import com.im.admin.dto.PinCodeResponseDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PinCodeLocationResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface PinCodeService {
    CommonResponse<PinCodeResponseDTO> save(PinCodeRequestDTO dto);

    CommonResponse<PinCodeResponseDTO> update(Long id, PinCodeRequestDTO dto);

    CommonResponse<PinCodeResponseDTO> getById(Long id);

    CommonResponse<Page<PinCodeResponseDTO>> getAll(int page, int size,
                                                    String pinCode, Boolean blocked, String createdBy, String editedBy);

    List<PinCodeDTO> getPinCodesByCity(Long cityId);

    PinCodeLocationResponse lookupByPinCode(String pinCode);

}

