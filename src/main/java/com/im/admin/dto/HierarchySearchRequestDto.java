package com.im.admin.dto;

import lombok.Data;

@Data
public class HierarchySearchRequestDto {
    private String hierarchyType;  // RT, DT, PT, etc.
}
