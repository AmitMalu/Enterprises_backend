package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponseDto {
    private Long id;
    private String alertType;
    private String product;
    private String title;
    private String body;
    private String deliveryChannel;
    private String audienceType;
    private String severity;
    private String sendMode;
    private LocalDateTime scheduleTime;
    private LocalDateTime createdAt;
}
