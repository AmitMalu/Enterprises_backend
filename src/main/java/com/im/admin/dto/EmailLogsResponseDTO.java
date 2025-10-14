package com.im.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailLogsResponseDTO {

    private Long id;
    private String request;
    private String response;
    private String ipAddress;
    private LocalDateTime requestDateTime;
    private LocalDateTime responseDateTime;
}
