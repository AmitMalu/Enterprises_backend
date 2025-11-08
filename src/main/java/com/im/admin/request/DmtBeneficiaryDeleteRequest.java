package com.im.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryDeleteRequest {
    private String mobile;     // remitter mobile number
    private Long bene_id;    // beneficiary ID
    private String subProductId;
}
