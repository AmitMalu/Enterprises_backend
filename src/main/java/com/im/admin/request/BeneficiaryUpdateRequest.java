package com.im.admin.request;

import lombok.Data;

@Data
public class BeneficiaryUpdateRequest {

    private Long beneId;           // existing beneficiary ID (local DB or vendor)
    private String name;       // updated name
    private String bankid;         // updated bank ID
    private String bankname;       // updated bank name
    private String accno;          // updated account number
    private String ifsccode;       // updated IFSC code
    private String verified;       // updated verification status
    private String subProductId;     // to fetch vendor credentials dynamically
    private String mobile;
}
