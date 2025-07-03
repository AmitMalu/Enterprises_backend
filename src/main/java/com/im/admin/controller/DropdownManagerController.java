package com.im.admin.controller;

import com.im.admin.dto.ChildUserDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.DropdownManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/dropdown-manager")
public class DropdownManagerController {

    @Autowired
    private DropdownManagerService dropdownManagerService;

    @GetMapping("/child-accounts/{hierarchyId}")
    public ResponseEntity<CommonResponse> getChildAccounts(@PathVariable String hierarchyId) {
        List<ChildUserDto> result = dropdownManagerService.getChildUsers(hierarchyId);

        if (result.isEmpty()) {
            return ResponseEntity.ok(new CommonResponse(200, "No APPROVED child users found. Some users may still be in PENDING status.", result));
        }

        return ResponseEntity.ok(new CommonResponse(200, "Fetched child users successfully", result));
    }



}
