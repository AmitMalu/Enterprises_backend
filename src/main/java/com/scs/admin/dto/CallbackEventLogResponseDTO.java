package com.scs.admin.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CallbackEventLogResponseDTO {
    private Integer id;
    private String request;
    private String response;
    private String ipAddress;
    private LocalDateTime requestDateTime;
    private LocalDateTime responseDateTime;
}
