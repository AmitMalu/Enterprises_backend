package com.im.admin.controller;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DropdownManagerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/dropdown-manager")
public class DropdownManagerController {

    @Autowired
    private DropdownManagerService dropdownManagerService;

    @GetMapping("/child-accounts/{hierarchyId}")
    public ResponseEntity<CommonResponse<Page<ChildUserDto>>> getFilteredChildUsers(
            @PathVariable String hierarchyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10000") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String parentAccount,
            @RequestParam(required = false) String abbreviation,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) String dob,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) String createdOn
    ) {
        log.info("API Call: getFilteredChildUsers for hierarchyId: {} with filters - userId: {}, parentAccount: {}, abbreviation: {}, title: {}, name: {}, mobileNumber: {}, emailId: {}, dob: {}, status: {}, isBlocked: {}, network: {}, createdOn: {}",
                hierarchyId, userId, parentAccount, abbreviation, title, name, mobileNumber, emailId, dob, status, isBlocked, network, createdOn);

        Page<ChildUserDto> result = dropdownManagerService.getChildUsers(
                hierarchyId, page, size, userId, parentAccount, abbreviation, title,
                name, mobileNumber, emailId, dob, status, isBlocked, network, createdOn
        );

        boolean filtersApplied = userId != null || parentAccount != null || abbreviation != null ||
                title != null || name != null || mobileNumber != null || emailId != null ||
                dob != null || status != null || isBlocked != null;

        String message;
        if (result.isEmpty()) {
            message = filtersApplied
                    ? "No child users found matching the given filters."
                    : "No APPROVED child users found. Some users may still be in PENDING status.";
            log.info(message);
        } else {
            message = "Fetched child users successfully";
            log.info("Fetched {} child users for hierarchyId: {}", result.getTotalElements(), hierarchyId);
        }

        return ResponseEntity.ok(new CommonResponse<>(200, message, result));
    }

    @GetMapping("/child/{hierarchyId}")
    public ResponseEntity<CommonResponse<Page<ChildUserDto>>> getChildUsers(
            @PathVariable String hierarchyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String parentAccount,
            @RequestParam(required = false) String abbreviation,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String emailId,
            @RequestParam(required = false) String dob,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isBlocked,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) String createdOn
    ) {
        log.info("API Call: getFilteredChildUsers for hierarchyId: {} with filters - userId: {}, parentAccount: {}, abbreviation: {}, title: {}, name: {}, mobileNumber: {}, emailId: {}, dob: {}, status: {}, isBlocked: {}, network: {}, createdOn: {}",
                hierarchyId, userId, parentAccount, abbreviation, title, name, mobileNumber, emailId, dob, status, isBlocked, network, createdOn);

        Page<ChildUserDto> result = dropdownManagerService.getChildUsersList(
                hierarchyId, page, size, userId, parentAccount, abbreviation, title,
                name, mobileNumber, emailId, dob, status, isBlocked, network, createdOn
        );

        boolean filtersApplied = userId != null || parentAccount != null || abbreviation != null ||
                title != null || name != null || mobileNumber != null || emailId != null ||
                dob != null || status != null || isBlocked != null;

        String message;
        if (result.isEmpty()) {
            message = filtersApplied
                    ? "No child users found matching the given filters."
                    : "No APPROVED child users found. Some users may still be in PENDING status.";
            log.info(message);
        } else {
            message = "Fetched child users successfully";
            log.info("Fetched {} child users for hierarchyId: {}", result.getTotalElements(), hierarchyId);
        }

        return ResponseEntity.ok(new CommonResponse<>(200, message, result));
    }
}
