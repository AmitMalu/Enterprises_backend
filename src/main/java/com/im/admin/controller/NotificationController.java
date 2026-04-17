package com.im.admin.controller;

import com.im.admin.entities.EmailCredentialsMaster;
import com.im.admin.request.EmailCredentialsRequestDto;
import com.im.admin.response.CommonResponse;
import com.im.admin.response.DropdownOptionDto;
import com.im.admin.response.DropdownResponseDto;
import com.im.admin.dto.NotificationRequestDto;
import com.im.admin.response.NotificationResponseDto;
import com.im.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @GetMapping("/user/list")
    public ResponseEntity<CommonResponse<Page<NotificationResponseDto>>> getUserNotifications(
            @RequestParam int page,
            @RequestParam int size) {

        Page<NotificationResponseDto> response =
                notificationService.getUserNotifications(page, size);

        return ResponseEntity.ok(
                CommonResponse.success(response, "User notifications fetched successfully")
        );
    }

    @GetMapping("/user/unread-count")
    public ResponseEntity<CommonResponse<Long>> getUnreadCount() {

        long count = notificationService.getUnreadCount();

        return ResponseEntity.ok(
                CommonResponse.success(count, "Unread count fetched successfully")
        );
    }


    @PutMapping("/{id}/read")
    public ResponseEntity<CommonResponse<String>> markAsRead(@PathVariable Long id) {

        notificationService.markAsRead(id);

        return ResponseEntity.ok(
                CommonResponse.success("Marked as read", "Notification marked as read")
        );
    }

    @PutMapping("/read-all")
    public ResponseEntity<CommonResponse<String>> markAll() {

        notificationService.markAllAsRead();

        return ResponseEntity.ok(
                CommonResponse.success("All marked as read", "All notifications marked as read")
        );
    }




    @GetMapping
    public ResponseEntity<CommonResponse<Page<NotificationResponseDto>>> getAllAudienceNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam String deliveryChannel // WEB / APP
    ) {

        Page<NotificationResponseDto> response =
                notificationService.getAllAudienceNotifications(page, size, deliveryChannel);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Notifications fetched successfully")
        );
    }

    @PostMapping("/email")
    public ResponseEntity<CommonResponse<String>> saveEmailCredentials(
            @RequestBody EmailCredentialsRequestDto dto) {

        String response = notificationService.saveEmailCredentials(dto);

        return ResponseEntity.ok(
                CommonResponse.created(response, "Email credentials saved successfully")
        );
    }

    @GetMapping("/email")
    public ResponseEntity<CommonResponse<Page<EmailCredentialsMaster>>> getAllEmailCredentials(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Page<EmailCredentialsMaster> response =
                notificationService.getAllEmailCredentials(page, size);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Email credentials fetched successfully")
        );
    }

    @PutMapping("/email/{id}")
    public ResponseEntity<CommonResponse<String>> updateEmailCredentials(
            @PathVariable Long id,
            @RequestBody EmailCredentialsRequestDto dto) {

        String response = notificationService.updateEmailCredentials(id, dto);

        return ResponseEntity.ok(
                CommonResponse.success(response, "Email credentials updated successfully")
        );
    }

    @GetMapping("/email/{id}")
    public ResponseEntity<CommonResponse<EmailCredentialsMaster>> getByIds(@PathVariable Long id) {

        EmailCredentialsMaster data = notificationService.getEmailCredentialById(id);

        return ResponseEntity.ok(
                CommonResponse.success(data, "Email credential fetched successfully")
        );
    }

    @GetMapping("/whitelabel/dropdown")
    public ResponseEntity<CommonResponse<List<DropdownOptionDto>>> getWhiteLabelDropdown(
            ) {


        List<DropdownOptionDto> data =
                notificationService.getWhiteLabelDropdownFromToken();

        return ResponseEntity.ok(
                CommonResponse.success(data, "WhiteLabel dropdown fetched successfully")
        );
    }


}