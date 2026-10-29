package com.scs.admin.request;

import lombok.Data;

@Data
public class DmtBeneficiaryResponseDto {

    private Long id;
    private String beneId;
    private String name;
    private String bankId;
    private String bankName;
    private String accountNo;
    private String ifscCode;
    private String verified;
    private String status;
}
