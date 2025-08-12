package com.im.admin.controller;

import com.im.admin.response.CommonResponse;
import com.im.admin.service.UserMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/user-mapping")
public class UserMapping {

    @Autowired
    private UserMappingService userMappingService;

    @PostMapping("/remap")
    public ResponseEntity<CommonResponse> remapUsers(
            @RequestParam String fromId,
            @RequestParam String toId) {

        List<?> updatedUsers = userMappingService.remapUsers(fromId, toId);
        return ResponseEntity.ok(new CommonResponse(200, "Mapped successfully", updatedUsers));
    }
}
