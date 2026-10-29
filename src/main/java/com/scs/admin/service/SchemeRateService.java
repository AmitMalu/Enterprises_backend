package com.scs.admin.service;

import com.scs.admin.dto.SchemeRateProductResponseDto;
import com.scs.admin.dto.SchemeRateRequestDTO;
import com.scs.admin.dto.SchemeSlabUpdateRequestDTO;
import com.scs.admin.entities.SchemeRate;
import com.scs.admin.entities.product.Products;
import com.scs.admin.request.SchemeRateRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.SchemeDetailResponseDto;
import com.scs.admin.response.SchemeRateResponse;

import java.util.List;

public interface SchemeRateService {

    SchemeDetailResponseDto getSchemeDetails(String schemeCode);

    List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO);

    SchemeDetailResponseDto updateSchemeRates(SchemeSlabUpdateRequestDTO requestDTO);

    SchemeRateProductResponseDto getProductDetails(String schemeCode);

    CommonResponse<SchemeRateResponse> getSchemeRate(
            SchemeRateRequest request);

}
