package com.im.admin.service;

import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.dto.SchemeSlabUpdateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.response.SchemeDetailResponseDto;

import java.util.List;

public interface SchemeRateService {

    SchemeDetailResponseDto getSchemeDetails(String schemeCode);

    List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO);

    SchemeDetailResponseDto updateSchemeRates(SchemeSlabUpdateRequestDTO requestDTO);



}
