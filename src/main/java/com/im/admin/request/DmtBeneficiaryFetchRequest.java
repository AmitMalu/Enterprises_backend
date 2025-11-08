package com.im.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryFetchRequest {
    private String mobile;  // remitter’s registered mobile
    private String subProductId; // dynamic for vendor credentials
}
