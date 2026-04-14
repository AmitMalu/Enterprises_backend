package com.im.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationRequestDto {
    private String alertType;
    private String product;
    private String title;
    private String body;
    private String deliveryChannel; // APP / WEB / BOTH
    private String audienceType;    // ALL / ROLE / USER
    private String severity;        // HIGH / LOW / MEDIUM / CRITICAL
    private String sendMode;        // INSTANT / SCHEDULE
    private LocalDateTime scheduleTime;    // "11:00:00 AM"

    // optional for targeting
    private String role;
    private String hierarchyId;

}
