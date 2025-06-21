package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ImSubMasterResponseDto {
    private Integer imSubMasterId;
    private String imSubMasterName;
    private Integer imMasterId;
    private String imMasterName;

}
