package com.im.admin.controller;

import com.im.admin.dto.BlockStatusRequest;
import com.im.admin.dto.StatusWiseUserFullDto;
import com.im.admin.dto.UserDetailsResponseDTO;
import com.im.admin.dto.ViewUserResponseDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.PaginatedResponse;
import com.im.admin.response.UserSearchResponseDto;
import com.im.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    //api/user/true/90988

    @PutMapping("/block/{idOrMobile}")
    public ResponseEntity<CommonResponse> updateBlockStatus(
            @PathVariable String idOrMobile,
            @RequestBody BlockStatusRequest request
    ) {
        return ResponseEntity.ok(userService.updateUserBlockStatus(idOrMobile, request.isBlock()));
    }



    @GetMapping("/status/{status}")
    public ResponseEntity<CommonResponse<List<StatusWiseUserFullDto>>> getUsersByStatus(@PathVariable String status) {
        List<StatusWiseUserFullDto> users = userService.getUsersByStatus(status.toUpperCase());
        return ResponseEntity.ok(CommonResponse.success(users, "Fetched records for status: " + status));
    }

    @GetMapping("/search-retailers")
    public ResponseEntity<CommonResponse<Page<UserSearchResponseDto>>> searchRetailers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdDate").descending());

        Page<UserSearchResponseDto> result = userService.searchActiveRetailers(keyword, pageable);

        if (result.isEmpty()) {
            return ResponseEntity.ok(CommonResponse.success(result, "No active retailers found for keyword: " + keyword));
        }

        return ResponseEntity.ok(CommonResponse.success(result, "Retailer search successful"));
    }

    @GetMapping
    public ResponseEntity<CommonResponse<Page<ViewUserResponseDto>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
    ) {
        Page<ViewUserResponseDto> resultPage = userService.getAllUsers(page, size, keyword);
        return ResponseEntity.ok(
                new CommonResponse<>(200, "All users fetched successfully", resultPage)
        );
    }




}
