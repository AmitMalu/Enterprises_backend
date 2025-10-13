package com.im.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SmsLogsResponseDTO {
    private Long id;
    private String request;
    private String response;
    private String ipAddress;
    private LocalDateTime requestDateTime;
    private LocalDateTime responseDateTime;
}
