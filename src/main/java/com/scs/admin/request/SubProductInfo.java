package com.scs.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubProductInfo {
    private String subProductCode;
    private String subProductName;
}
