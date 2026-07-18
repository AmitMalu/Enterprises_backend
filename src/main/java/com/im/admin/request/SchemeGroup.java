package com.im.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SchemeGroup {
    private String schemeCode;
    private String schemeName;
    private List<SubProductInfo> subProducts;
}
