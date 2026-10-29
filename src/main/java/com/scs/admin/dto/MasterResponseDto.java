package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MasterResponseDto {
    private Integer scsMasterId;
    private String scsMasterName;
    private String createdBy;
    private LocalDateTime createdOn;
}
