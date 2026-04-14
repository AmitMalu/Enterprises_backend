package com.im.admin.controller;

import com.im.admin.response.CommonResponse;
import com.im.admin.response.DropdownResponseDto;
import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.response.NotificationResponseDto;
import com.im.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;


    @PostMapping("/create")
    public ResponseEntity<CommonResponse<NotificationResponseDto>> createNotification(
            @RequestBody NotificationRequestDto requestDto) {

        NotificationResponseDto response =
                notificationService.createNotification(requestDto);

        return ResponseEntity.ok(
                CommonResponse.created(response, "Notification created successfully")
        );
    }


    @GetMapping("/alert-types")
    public ResponseEntity<CommonResponse<?>> getAlertTypes() {

        return ResponseEntity.ok(
                CommonResponse.success(
                        notificationService.getAllAlertTypes(),
                        "Alert types fetched successfully"
                )
        );
    }


    @GetMapping("/list")
    public ResponseEntity<CommonResponse<Page<NotificationResponseDto>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<NotificationResponseDto> response =
                notificationService.getAllNotifications(page, size, sortBy, sortDir);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Notifications fetched successfully")
        );
    }


    @GetMapping("/get/{id}")
    public ResponseEntity<CommonResponse<NotificationResponseDto>> getById(
            @PathVariable Long id) {

        NotificationResponseDto response =
                notificationService.getNotificationById(id);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Notification fetched successfully")
        );
    }


    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<NotificationResponseDto>> update(
            @PathVariable Long id,
            @RequestBody NotificationRequestDto requestDto) {

        NotificationResponseDto response =
                notificationService.updateNotification(id, requestDto);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Notification updated successfully")
        );
    }


    @GetMapping("/dropdowns")
    public ResponseEntity<CommonResponse<DropdownResponseDto>> getDropdowns() {

        DropdownResponseDto response =
                notificationService.getDropdownData();

        return ResponseEntity.ok(
                CommonResponse.success(response, "Dropdown data fetched successfully")
        );
    }
}