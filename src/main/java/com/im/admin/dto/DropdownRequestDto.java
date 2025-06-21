package com.im.admin.dto;


import lombok.Data;

@Data
public class DropdownRequestDto {
    private String imMasterName;       // e.g., Currency
    private String imSubMasterName;     // e.g., Rs, AED, USD
}
