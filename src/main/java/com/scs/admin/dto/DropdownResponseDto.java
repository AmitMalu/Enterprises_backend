package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DropdownResponseDto {
    private Integer scsMasterId;
    private String scsMasterName;
    private Integer scsSubMasterId;
    private String scsSubMasterName;
    private String createdBy;
    private LocalDateTime createdOn;
}
