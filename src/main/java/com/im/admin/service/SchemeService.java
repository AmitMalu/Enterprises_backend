package com.im.admin.service;


import com.im.admin.dto.*;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.VendorRouting;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GetAllSchemeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface SchemeService {

    List<Map<String, Object>> getAllSchemeHierarchySelections();

    List<Map<String, Object>> searchSchemeHierarchy(String keyword);

    Map<String, Object> saveScheme(SchemeDTO schemeDTO);

    OperatorProductResponseDTO getOperatorsAndProduct(String productCode, String subProductCode);

    List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO);

    List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode);

    CommonResponse<List<SchemeAllocation>> allocateSchemes(SchemeAllocationRequestDto request);

    VendorRouting saveVendorRouting(VendorRoutingDto request);

    Page<GetAllSchemeResponse> getAllSchemes(Pageable pageable);

    GetAllSchemeResponse getSchemeDtoById(Long id);

}
