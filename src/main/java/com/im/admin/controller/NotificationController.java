package com.im.admin.controller;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.Notification;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponse> createNotification(@RequestBody NotificationRequestDto dto) {
        log.info("Received request to create notification: {}", dto);
        Notification created = notificationService.createNotification(dto);
        log.info("Notification created successfully with ID: {}", created.getId());
        return ResponseEntity.ok(new CommonResponse<>(200, "success", created));
    }

}
