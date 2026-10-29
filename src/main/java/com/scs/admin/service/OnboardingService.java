package com.scs.admin.service;

import com.scs.admin.dto.GenericPendingRecordDto;
import com.scs.admin.dto.OnboardingHierarchyRequestDto;
import com.scs.admin.request.ChangeStatusToPendingRequest;
import com.scs.admin.response.GenericPendingResponseDto;
import org.springframework.data.domain.Page;

public interface OnboardingService {

    String changeRejectedUserToPending(ChangeStatusToPendingRequest changeStatusToPending);

    String approveOrRejectHierarchy(OnboardingHierarchyRequestDto hierarchyRequestDto);

    GenericPendingRecordDto getRecordById(String id);

    Page<GenericPendingRecordDto> getAllRecordsByStatus(String status, int page, int size, String type, String id, String name, String createdBy, String createdOn, String mobileNumber, String emailId, String parentAccount,String rejectionReason);

    String moveToPending(String hierarchyId);

}
