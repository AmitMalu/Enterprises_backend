package com.im.admin.service;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.PendingStatusResponseDto;

public interface OnboardingService {

    PendingStatusResponseDto getAllRecordsByStatus(String status);

    String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto);
}
