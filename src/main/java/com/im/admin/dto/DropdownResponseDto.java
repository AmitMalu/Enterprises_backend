package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DropdownResponseDto {
    private Integer imMasterId;
    private String imMasterName;
    private Integer imSubMasterId;
    private String imSubMasterName;
    private String createdBy;
    private LocalDateTime createdOn;
}
