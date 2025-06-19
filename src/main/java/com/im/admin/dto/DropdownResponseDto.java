package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DropdownResponseDto {
    private Integer imMasterId;
    private String imMasterName;
    private Integer imSubMasterId;
    private String imSubMasterName;
}
