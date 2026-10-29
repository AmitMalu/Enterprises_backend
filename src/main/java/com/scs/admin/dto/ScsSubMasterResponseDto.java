package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ScsSubMasterResponseDto {
    private Integer scsSubMasterId;
    private String scsSubMasterName;
    private Integer scsMasterId;
    private String scsMasterName;

}
