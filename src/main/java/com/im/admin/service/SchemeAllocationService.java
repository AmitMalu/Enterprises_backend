package com.im.admin.service;

import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;

import java.util.List;

public interface SchemeAllocationService {

    CommonResponse<List<SchemeAllocation>> allocateSchemes(SchemeAllocationRequestDto request);

    CommonResponse<String> saveSchemeAllocation(SchemeAllocationRequest request);

    List<SchemeDropdownListBySubProductResponse> getSchemeDropdownListBySubProduct(String subProductName);

    List<RetailerSchemeAllocationResponse> getAllocatedSchemesForRetailer(String hierarchyId);

}
