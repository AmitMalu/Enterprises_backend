package com.im.admin.controller;

import com.im.admin.dto.HierarchyUpdateDTO;
import com.im.admin.dto.RemapRequestDTO;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserMappingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/user-mapping")
public class UserMappingController {

    @Autowired
    private UserMappingService userMappingService;

    @PostMapping("/remap-bulk")
    public ResponseEntity<CommonResponse> remapUsers(@RequestParam String fromId,
                                                     @RequestParam String toId) {
        log.info("Bulk remap request received: fromId={}, toId={}", fromId, toId);

        try {
            List<?> updatedUsers = userMappingService.remapUsers(fromId, toId);
            log.info("Bulk remap completed. Total users updated: {}", updatedUsers.size());
            return ResponseEntity.ok(new CommonResponse<>(200, "Mapped successfully", updatedUsers));
        } catch (Exception e) {
            log.error("Error during bulk remap from {} to {}", fromId, toId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Bulk remap failed", null));
        }
    }

    @PostMapping("/remap")
    public ResponseEntity<CommonResponse> remapMultipleUsers(@RequestBody RemapRequestDTO request) {
        log.info("Remap request received for multiple users to newParentId={}", request.getNewParentId());

        try {
            List<HierarchyUpdateDTO> updatedUsers =
                    userMappingService.remapMultipleUsers(request.getUserIds(), request.getNewParentId());

            log.info("Remap completed. Total users updated: {}", updatedUsers.size());
            return ResponseEntity.ok(new CommonResponse<>(200, "Users mapped successfully", updatedUsers));
        } catch (Exception e) {
            log.error("Error during remap for users: {}", request.getUserIds(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Remap failed", null));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<CommonResponse> getUsersByType(@RequestParam String userType) {
        log.info("Fetching users by type: {}", userType);

        try {
            List<HierarchyUpdateDTO> users = userMappingService.getUsersByType(userType.toUpperCase());
            log.info("Fetched {} users of type {}", users.size(), userType);
            return ResponseEntity.ok(new CommonResponse<>(200, "Users fetched successfully", users));
        } catch (Exception e) {
            log.error("Error fetching users by type: {}", userType, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch users", null));
        }
    }

}
