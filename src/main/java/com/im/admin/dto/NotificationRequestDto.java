package com.im.admin.dto;

import lombok.Data;

@Data
public class NotificationRequestDto {
    private String message;
    private String type;
    private Boolean visible;
    private String createdBy;
    private String hierarchyCodes; // e.g. "RT,DT"
}
