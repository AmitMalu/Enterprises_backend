package com.im.admin.controller;

import com.im.admin.dto.HierarchyUpdateDTO;
import com.im.admin.dto.RemapRequestDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user-mapping")
public class UserMappingController {

    @Autowired
    private UserMappingService userMappingService;

    @PostMapping("/remap-bulk")
    public ResponseEntity<CommonResponse> remapUsers(
            @RequestParam String fromId,
            @RequestParam String toId) {

        List<?> updatedUsers = userMappingService.remapUsers(fromId, toId);
        return ResponseEntity.ok(new CommonResponse(200, "Mapped successfully", updatedUsers));
    }

    @PostMapping("/remap")
    public ResponseEntity<CommonResponse> remapMultipleUsers(@RequestBody RemapRequestDTO request) {

        List<HierarchyUpdateDTO> updatedUsers =
                userMappingService.remapMultipleUsers(request.getUserIds(), request.getNewParentId());

        return ResponseEntity.ok(
                new CommonResponse(200, "Users mapped successfully", updatedUsers)
        );
    }

    @GetMapping("/users")
    public ResponseEntity<CommonResponse> getUsersByType(@RequestParam String userType) {
        List<HierarchyUpdateDTO> users = userMappingService.getUsersByType(userType.toUpperCase());

        return ResponseEntity.ok(
                new CommonResponse(200, "Users fetched successfully", users)
        );
    }

}
