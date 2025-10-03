package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MasterResponseDto {
    private Integer imMasterId;
    private String imMasterName;
    private String createdBy;
    private LocalDateTime createdOn;
}
