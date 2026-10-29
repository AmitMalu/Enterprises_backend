package com.scs.admin.service;

import com.scs.admin.dto.DistrictDTO;
import com.scs.admin.dto.DistrictRequestDTO;
import com.scs.admin.dto.DistrictResponseDTO;
import com.scs.admin.response.CommonResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
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

    ByteArrayInputStream uploadDistrictsFromExcel(MultipartFile file, String uploadedBy) throws IOException;

}

