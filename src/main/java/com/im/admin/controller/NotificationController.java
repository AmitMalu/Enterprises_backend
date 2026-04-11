package com.im.admin.controller;

import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.entities.Notification;
import com.im.admin.entities.NotificationMaster;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {
    @Autowired
    private NotificationService notificationService;

//    @PostMapping("/create")
//    public ResponseEntity<CommonResponse> createNotification(@RequestBody NotificationRequestDto dto) {
//        log.info("Received request to create notification: {}", dto);
//        Notification created = notificationService.createNotification(dto);
//        log.info("Notification created successfully with ID: {}", created.getId());
//        return ResponseEntity.ok(new CommonResponse<>(200, "success", created));
//    }

    @PostMapping("/create")
    public ResponseEntity<?> createNotification(
            @RequestBody NotificationMaster request) {

        NotificationMaster response =
                notificationService.createNotification(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/alert-types")
    public ResponseEntity<?> getAlertTypes() {
        return ResponseEntity.ok(
                notificationService.getAllAlertTypes());
    }

    @GetMapping("/list")
    public ResponseEntity<?> getAll() {
        return ResponseEntity.ok(
                notificationService.getAllNotifications());
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                notificationService.getNotificationById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody NotificationMaster request) {

        return ResponseEntity.ok(
                notificationService.updateNotification(id, request));
    }



}
