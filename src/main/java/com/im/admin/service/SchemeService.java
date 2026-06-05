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

    Map<String, Object> updateScheme(String schemeId, SchemeDTO schemeDTO);

    OperatorProductResponseDTO getOperatorsAndProduct(String productCode, String subProductCode);

    List<SchemeRate> getSchemeRatesByOperatorCode(String operatorCode);;

    Page<GetAllSchemeResponse> getAllSchemes(
            Long id,
            String schemeName,
            String schemeCode,
            String schemeCombinationHashCode,
            String productId,
            String productName,
            String subProductId,
            String subProductName,
            String vendorId,
            String vendorName,
            String schemeHierarchyName,
            String schemeHierarchySelectionId,
            String createdOn,
            String createdBy,
            String editedOn,
            String editedBy,
            Pageable pageable,
            String hierarchyId
    );


    GetAllSchemeResponse getSchemeDtoById(Long id);

    void updateSchemeStatus(String schemeCode, boolean isActive);


}
