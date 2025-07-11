package com.im.admin.controller;

import com.im.admin.dto.OnboardingHierarchyRequestDto;
import com.im.admin.dto.GenericPendingRecordDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.GenericPendingResponseDto;
import com.im.admin.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/onboarding")
@Slf4j
public class OnboardingHierarchyController {

    @Autowired
    private OnboardingService onboardingService;

    @PostMapping("/getAll-records")
    public ResponseEntity<CommonResponse<Page<GenericPendingRecordDto>>> getAllRecordsByStatus(
            @RequestParam("status") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<GenericPendingRecordDto> response = onboardingService.getAllRecordsByStatus(status, page, size);

        String formattedStatus = status.toUpperCase();
        String message;
        switch (formattedStatus) {
            case "APPROVED" -> message = "Approved records fetched successfully";
            case "REJECTED" -> message = "Rejected records fetched successfully";
            case "PENDING" -> message = "Pending records fetched successfully";
            default -> message = formattedStatus + " records fetched successfully";
        }

        return ResponseEntity.ok(CommonResponse.success(response, message));
    }



    @PostMapping("/approve-or-reject")
    public ResponseEntity<CommonResponse> onboardHierarchy(@RequestBody OnboardingHierarchyRequestDto hierarchyRequestDto) {

        String response = onboardingService.approveOrRejectHierarchy(hierarchyRequestDto);
        return ResponseEntity.ok(new CommonResponse<>(200, response,null));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<GenericPendingRecordDto>> getRecordById(@PathVariable String id) {
        GenericPendingRecordDto record = onboardingService.getRecordById(id);
        return ResponseEntity.ok(CommonResponse.success(record, "Record fetched successfully"));
    }




}
