package com.im.admin.controller;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.Notification;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createNotification(@RequestBody NotificationRequestDto dto) {
        Notification created = notificationService.createNotification(dto);
        return ResponseEntity.ok(new CommonResponse<>(200, "success", created));
    }
}
