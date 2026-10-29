package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author SHUBHAM KHOPADE
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SchemeAllocationDetailsDTO {

    private String productCode;
    private String productName;
    private String schemeCode;
    private String schemeName;
    private String subProductCode;
    private String subProductName;

}