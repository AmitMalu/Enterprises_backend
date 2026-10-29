package com.scs.admin.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetailerSchemeAllocationResponse {

    private String subProductName;
    private String subProductCode;
    private String schemeName;
}
