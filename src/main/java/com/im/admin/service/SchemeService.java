package com.im.admin.service;


import com.im.admin.dto.*;
import com.im.admin.entities.SchemeRate;
import com.im.admin.entities.VendorRouting;

import java.util.List;
import java.util.Map;

public interface SchemeService {

    List<Map<String, Object>> getAllSchemeHierarchySelections();

    List<Map<String, Object>> searchSchemeHierarchy(String keyword);

    Map<String, Object> saveScheme(SchemeDTO schemeDTO);

    OperatorProductResponseDTO getOperatorsAndProduct(String productCode, String subProductCode);

    List<SchemeRate> createSchemeRates(SchemeRateRequestDTO requestDTO);

    List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode);

    void allocateSchemes(SchemeAllocationRequestDto request);

    VendorRouting saveVendorRouting(VendorRoutingDto request);

}
