package com.im.admin.controller;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DropdownManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/dropdown-manager")
public class DropdownManagerController {

    @Autowired
    private DropdownManagerService dropdownManagerService;

    @GetMapping("/child-accounts/{hierarchyId}")
    public ResponseEntity<CommonResponse<Page<ChildUserDto>>> getFilteredChildUsers(
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
            @RequestParam(required = false) Boolean isBlocked
    ) {
        Page<ChildUserDto> result = dropdownManagerService.getChildUsers(
                hierarchyId, page, size, userId, parentAccount, abbreviation, title,
                name, mobileNumber, emailId, dob, status, isBlocked
        );

        boolean filtersApplied = userId != null || parentAccount != null || abbreviation != null ||
                title != null || name != null || mobileNumber != null || emailId != null ||
                dob != null || status != null || isBlocked != null;

        String message;
        if (result.isEmpty()) {
            message = filtersApplied
                    ? "No child users found matching the given filters."
                    : "No APPROVED child users found. Some users may still be in PENDING status.";
        } else {
            message = "Fetched child users successfully";
        }

        return ResponseEntity.ok(new CommonResponse<>(200, message, result));
    }


}
