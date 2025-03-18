package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorDropdownResponseDto {

    private String id;
    private String vendorName;
    private String vendorCode;

}
