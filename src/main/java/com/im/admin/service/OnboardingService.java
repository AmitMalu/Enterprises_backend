package com.im.admin.service;

import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.response.GenericPendingResponseDto;
import org.springframework.data.domain.Page;

public interface OnboardingService {

  

    String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto);

    Page<GenericPendingRecordDto> getAllRecordsByStatus(String status, int page, int size);

    GenericPendingRecordDto getRecordById(String id);
}
