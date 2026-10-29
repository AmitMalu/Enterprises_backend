package com.scs.admin.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductDropdownResponseDto {

    private Short id;
    private String productName;
    private String productCode;
    private Boolean isSettlement;

}
