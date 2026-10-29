package com.scs.admin.service;

import com.scs.admin.dto.PinCodeDTO;
import com.scs.admin.dto.PinCodeRequestDTO;
import com.scs.admin.dto.PinCodeResponseDTO;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.PinCodeLocationResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;

public interface PinCodeService {
    CommonResponse<PinCodeResponseDTO> save(PinCodeRequestDTO dto);

    CommonResponse<PinCodeResponseDTO> update(Long id, PinCodeRequestDTO dto);

    CommonResponse<PinCodeResponseDTO> getById(Long id);

    CommonResponse<Page<PinCodeResponseDTO>> getAll(int page, int size,
                                                    String pinCode, Boolean blocked, String createdBy, String editedBy);

    List<PinCodeDTO> getPinCodesByCity(Long cityId);

    PinCodeLocationResponse lookupByPinCode(String pinCode);

    ByteArrayResource generateSampleTemplate();

    ByteArrayInputStream uploadPinCodesFromExcel(MultipartFile file, String uploadedBy);
}

