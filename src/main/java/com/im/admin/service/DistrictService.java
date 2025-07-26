package com.im.admin.service;

import com.im.admin.dto.DistrictDTO;
import com.im.admin.dto.DistrictRequestDTO;
import com.im.admin.dto.DistrictResponseDTO;
import com.im.admin.response.CommonResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface DistrictService {

    CommonResponse<DistrictResponseDTO> save(DistrictRequestDTO dto);

    CommonResponse<DistrictResponseDTO> update(Long id, DistrictRequestDTO dto);

    CommonResponse<DistrictResponseDTO> getById(Long id);

    CommonResponse<Page<DistrictResponseDTO>> getAll(int page, int size,
                                                     String districtName, String districtCode, Boolean blocked, String createdBy, String editedBy);

    List<DistrictDTO> getDistrictsByState(Long stateId);

    ByteArrayResource generateSampleTemplate() throws IOException;

    String uploadDistrictsFromExcel(MultipartFile file, String uploadedBy) throws IOException;

}

