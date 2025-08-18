package com.im.admin.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HierarchyResponseDto {
    private String id; // hierarchyId
    private String displayName; // name#hierarchyId
    private String name;
    private String mobileno;
    private BigDecimal prepaidAmount;
    private BigDecimal postpaidAmount;
    private String email;

}
