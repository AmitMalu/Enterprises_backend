package com.im.admin.controller;

import com.im.admin.dto.BlockStatusRequest;
import com.im.admin.dto.StatusWiseUserFullDto;
import com.im.admin.dto.UserDetailsResponseDTO;
import com.im.admin.dto.ViewUserResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaginatedResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/block/{idOrMobile}")
    public ResponseEntity<CommonResponse> updateBlockStatus(
            @PathVariable String idOrMobile,
            @RequestBody BlockStatusRequest request
    ) {
        log.info("Received request to update block status for ID/Mobile: {} to {}", idOrMobile, request.isBlock());
        try {
            CommonResponse response = userService.updateUserBlockStatus(idOrMobile, request.isBlock());
            log.info("Block status updated successfully for ID/Mobile: {}", idOrMobile);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating block status for ID/Mobile: {}", idOrMobile, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse(500, "Failed to update block status", null));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<CommonResponse<List<StatusWiseUserFullDto>>> getUsersByStatus(@PathVariable String status) {
        log.info("Fetching users by status: {}", status);
        try {
            List<StatusWiseUserFullDto> users = userService.getUsersByStatus(status.toUpperCase());
            log.info("Fetched {} users with status: {}", users.size(), status);
            return ResponseEntity.ok(CommonResponse.success(users, "Fetched records for status: " + status));
        } catch (Exception e) {
            log.error("Error fetching users by status: {}", status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Failed to fetch users by status"));
        }
    }

    @GetMapping("/search-retailers")
    public ResponseEntity<CommonResponse<Page<UserSearchResponseDto>>> searchRetailers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("Searching active retailers with keyword: '{}', page: {}, size: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        try {
            Page<UserSearchResponseDto> result = userService.searchActiveRetailers(keyword, pageable);

            if (result.isEmpty()) {
                log.warn("No active retailers found for keyword: {}", keyword);
                return ResponseEntity.ok(CommonResponse.success(result, "No active retailers found for keyword: " + keyword));
            }

            log.info("Retailer search successful. Total results: {}", result.getTotalElements());
            return ResponseEntity.ok(CommonResponse.success(result, "Retailer search successful"));
        } catch (Exception e) {
            log.error("Error searching retailers with keyword: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResponse.error(500, "Retailer search failed"));
        }
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<ViewUserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        log.info("Fetching all users. Page: {}, Size: {}, Keyword: {}", page, size, keyword);

        try {
            Page<ViewUserResponseDto> resultPage = userService.getAllUsers(page, size, keyword);
            log.info("Fetched {} users", resultPage.getTotalElements());
            return ResponseEntity.ok(new CommonResponse<>(200, "All users fetched successfully", resultPage));
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResponse<>(500, "Failed to fetch users", null));
        }
    }

}
