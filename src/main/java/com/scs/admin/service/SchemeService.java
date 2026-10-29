package com.scs.admin.service;


import com.scs.admin.dto.*;
import com.scs.admin.entities.SchemeAllocation;
import com.scs.admin.entities.SchemeRate;
import com.scs.admin.entities.VendorRouting;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.GetAllSchemeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface SchemeService {

    List<Map<String, Object>> getAllSchemeHierarchySelections();

    List<Map<String, Object>> searchSchemeHierarchy(String keyword);

    Map<String, Object> saveScheme(SchemeDTO schemeDTO);

    Map<String, Object> updateScheme(String schemeId, SchemeDTO schemeDTO);

    OperatorProductResponseDTO getOperatorsAndProduct(String productCode, String subProductCode);

    List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode);;

    CommonResponse<Page<GetAllSchemeResponse>> getAllSchemes(

            SchemeFilter filter,

            Pageable pageable

    );

    GetAllSchemeResponse getSchemeDtoById(Long id);

    void updateSchemeStatus(String schemeCode, boolean isActive);


}
