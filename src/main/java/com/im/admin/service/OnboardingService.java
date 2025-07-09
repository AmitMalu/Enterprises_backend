package com.im.admin.service;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.response.GenericPendingResponseDto;

public interface OnboardingService {

    GenericPendingResponseDto getAllRecordsByStatus(String status);

    String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto);
}
