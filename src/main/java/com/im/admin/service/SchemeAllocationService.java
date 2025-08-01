package com.im.admin.service;

import com.im.admin.dto.SchemeAllocationRequestDto;
import com.im.admin.entities.SchemeAllocation;
import com.im.admin.request.SchemeAllocationRequest;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.RetailerSchemeAllocationResponse;
import com.im.admin.response.SchemeDropdownListBySubProductResponse;
import com.im.admin.response.UserSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchemeAllocationService {

    List<SchemeAllocation> assignOrUpdateSchemes(SchemeAllocationRequestDto request);

    CommonResponse<String> saveSchemeAllocation(SchemeAllocationRequest request);

    List<SchemeDropdownListBySubProductResponse> getSchemeDropdownListBySubProduct(String subProductName);

    List<RetailerSchemeAllocationResponse> getAllocatedSchemesForRetailer(String hierarchyId);

    Page<UserSearchResponseDto> searchActiveRetailers(String keyword, Pageable pageable);

}
