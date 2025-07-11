package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemeDropdownListBySubProductResponse {

    private Long schemeId;
    private String schemeName;
    private String schemeCode;

}
