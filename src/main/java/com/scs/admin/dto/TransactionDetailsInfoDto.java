package com.scs.admin.dto;

import lombok.*;

/**
 * @author SHUBHAM KHOPADE
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionDetailsInfoDto {

    private String subProductName;
    private String subProductCode;

    private String productName;
    private String productCode;

    private String schemeCode;
    private String schemeName;

    private String operatorName;
    private String operatorCode;

    private String vendorName;
    private String vendorCode;

}
