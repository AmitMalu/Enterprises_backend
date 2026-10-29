package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
//@AllArgsConstructor
public class SubMasterRequestDto {
    private Integer scsMasterId;
    private String scsSubMasterName;
    private String createdBy;
}
