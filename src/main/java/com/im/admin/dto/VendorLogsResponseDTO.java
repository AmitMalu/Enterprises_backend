package com.im.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VendorLogsResponseDTO {
    private Integer srNo;
    private String imVendorId;
    private String request;
    private String response;
    private String requestIp;
    private LocalDateTime requestDateTime;
    private LocalDateTime responseDateTime;
}
