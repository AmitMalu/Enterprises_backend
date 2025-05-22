package com.im.admin.controller;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.PendingStatusResponseDto;
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
    public ResponseEntity<PendingStatusResponseDto> getAllRecordsByStatus(@RequestParam("status") String status) {

       PendingStatusResponseDto response = onboardingService.getAllRecordsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approve-or-reject")
    public ResponseEntity<String> onboardHierarchy(@RequestBody OnboardingHierarchyRequestDto hierarchyRequestDto) {

        String response = onboardingService.approveOrRejectHierarchy(hierarchyRequestDto);
        return ResponseEntity.ok(response);
    }


}
