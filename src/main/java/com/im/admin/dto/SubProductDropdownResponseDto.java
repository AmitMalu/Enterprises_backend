package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubProductDropdownResponseDto {

    private String id;
    private String subProduct_name;
    private String subProduct_code;

    public SubProductDropdownResponseDto(String id, String subProduct_name) {
        this.id = id;
        this.subProduct_name = subProduct_name;
    }
}
