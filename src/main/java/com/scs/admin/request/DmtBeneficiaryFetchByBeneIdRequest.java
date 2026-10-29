package com.scs.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryFetchByBeneIdRequest {
    private String mobile;       // Remitter's registered mobile number
    private String beneid;       // Beneficiary ID to fetch
    private String subProductId;
}
