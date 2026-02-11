package com.im.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryFetchRequest {
    private String mobile;  // remitter’s registered mobile
    private String subProductId; // dynamic for vendor credentials


    private String beneId;
    private String bankid;
    private String bankname;
    private String name;
    private String accno;
    private String ifsc;
    private String verified;
    private Boolean paytm;
}
