package com.scs.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchSubProductFromRetailerRequest {

    private String retailerId;
    private String productCode;
    
}
