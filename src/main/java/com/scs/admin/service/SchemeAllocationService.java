package com.scs.admin.service;

import com.scs.admin.dto.SchemeAllocationRequestDto;
import com.scs.admin.entities.SchemeAllocation;
import com.scs.admin.request.SchemeAllocationRequest;
import com.scs.admin.response.CommonResponse;
import com.scs.admin.response.RetailerSchemeAllocationResponse;
import com.scs.admin.response.SchemeDropdownListBySubProductResponse;
import com.scs.admin.response.UserSearchResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SchemeAllocationService {

    List<SchemeAllocation> assignOrUpdateSchemes(SchemeAllocationRequestDto request);

    CommonResponse<String> saveSchemeAllocation(SchemeAllocationRequest request);

    List<SchemeDropdownListBySubProductResponse> getSchemeDropdownListBySubProduct(String subProductName);

    List<RetailerSchemeAllocationResponse> getAllocatedSchemesForRetailer(String hierarchyId);

    Page<UserSearchResponseDto> searchActiveHierarchies(
            String mobileNumber,
            String hierarchyId,
            String hierarchyCode,
            String createdDate,
            String lastLoginDate,
            String emailId,
            String keyword,
            Pageable pageable
    );


}
