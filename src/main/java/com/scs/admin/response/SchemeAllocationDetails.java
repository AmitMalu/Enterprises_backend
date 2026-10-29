package com.scs.admin.response;

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
public class SchemeAllocationDetails {
    private String productCode;
    private String productName;
    private List<SchemeDetail> schemes;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SchemeDetail {
        private String schemeCode;
        private String schemeName;
        private List<SubProductDetail> subProducts;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SubProductDetail {
        private String subProductCode;
        private String subProductName;
    }
}
