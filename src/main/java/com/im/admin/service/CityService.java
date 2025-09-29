package com.im.admin.service;

import com.im.admin.dto.CityDTO;
import com.im.admin.dto.CityRequestDTO;
import com.im.admin.dto.CityResponseDTO;
import com.im.admin.response.CommonResponse;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

public interface CityService {
    CommonResponse<CityResponseDTO> save(CityRequestDTO dto);

    CommonResponse<CityResponseDTO> update(Long id, CityRequestDTO dto);

    CommonResponse<CityResponseDTO> getById(Long id);

    CommonResponse<Page<CityResponseDTO>> getAll(int page, int size,
                                                 String cityName, String cityCode, Boolean blocked, String createdBy, String editedBy);

    List<CityDTO> getCitiesByDistrict(Long districtId);

    ByteArrayResource generateCitySampleTemplate() throws IOException;

    ByteArrayResource uploadCitiesFromExcel(MultipartFile file, String uploadedBy) throws IOException;
}

