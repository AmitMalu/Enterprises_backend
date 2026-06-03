package com.im.admin.service;

import com.im.admin.dto.SchemeRateProductResponseDto;
import com.im.admin.dto.SchemeRateRequestDTO;
import com.im.admin.dto.SchemeSlabUpdateRequestDTO;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.product.Products;
import com.im.admin.request.SchemeRateRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.SchemeDetailResponseDto;
import com.im.admin.response.SchemeRateResponse;

import java.util.List;

public interface SchemeRateService {

    SchemeDetailResponseDto getSchemeDetails(String schemeCode);

    List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO);

    SchemeDetailResponseDto updateSchemeRates(SchemeSlabUpdateRequestDTO requestDTO);

    SchemeRateProductResponseDto getProductDetails(String schemeCode);

    CommonResponse<SchemeRateResponse> getSchemeRate(
            SchemeRateRequest request);

}
