package com.im.admin.controller;

import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PutMapping("/{hierarchyId}/block-status")
    public ResponseEntity<CommonResponse> updateBlockStatus(
            @PathVariable String hierarchyId,
            @RequestParam boolean block  // true = block(1), false = unblock(0)
    ) {
        String message = userService.updateUserBlockStatus(hierarchyId, block);

        return ResponseEntity.ok(CommonResponse.success(null, message));
    }
}
