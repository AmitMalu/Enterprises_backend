package com.scs.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScsLogsResponseDTO {
    private Integer srNo;
    private String request;
    private String response;
    private String ipAddress;
    private LocalDateTime requestDateTime;
    private LocalDateTime responseDateTime;
}
