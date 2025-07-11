package com.im.admin.controller;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GenericPendingResponseDto;
import com.im.admin.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@Slf4j
public class OnboardingHierarchyController {

    @Autowired
    private OnboardingService onboardingService;

    @PostMapping("/getAll-records")
    public ResponseEntity<CommonResponse<GenericPendingResponseDto>> getAllRecordsByStatus(@RequestParam("status") String status) {
        GenericPendingResponseDto response = onboardingService.getAllRecordsByStatus(status);
        return ResponseEntity.ok(CommonResponse.success(response, "Pending records fetched successfully"));
    }


    @PostMapping("/approve-or-reject")
    public ResponseEntity<String> onboardHierarchy(@RequestBody OnboardingHierarchyRequestDto hierarchyRequestDto) {

        String response = onboardingService.approveOrRejectHierarchy(hierarchyRequestDto);
        return ResponseEntity.ok(response);
    }


}
