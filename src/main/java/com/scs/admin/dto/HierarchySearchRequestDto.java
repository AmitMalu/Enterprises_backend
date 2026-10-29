package com.scs.admin.dto;

import lombok.Data;

@Data
public class HierarchySearchRequestDto {
    private String hierarchyType;  // RT, DT, PT, etc.
    private String searchKeyword;  // RT or hierarchyId or mobileNumber
}
